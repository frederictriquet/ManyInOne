package fr.triquet.manyinone.radio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import fr.triquet.manyinone.data.local.AppDatabase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RadioUiState(
    val stations: List<RadioStation> = emptyList(),
    val currentStationId: Long? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val icyTitle: String? = null,
    val sleepTimerOption: SleepTimerOption = SleepTimerOption.OFF,
    val sleepTimerRemainingSeconds: Long = 0,
)

class RadioViewModel(
    application: Application,
    testPlayer: Player? = null,
) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).radioStationDao()

    private val _playerState = MutableStateFlow(RadioUiState())
    private val _stations = MutableStateFlow<List<RadioStation>>(emptyList())

    val uiState: StateFlow<RadioUiState> = combine(
        _stations,
        _playerState,
    ) { stations, player ->
        player.copy(stations = stations)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RadioUiState())

    init {
        viewModelScope.launch {
            dao.getAll().collect { _stations.value = it }
        }
    }

    private var mediaController: Player? = null
    private var sleepTimerJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _playerState.update {
                it.copy(isBuffering = playbackState == Player.STATE_BUFFERING)
            }
        }

        override fun onMediaMetadataChanged(metadata: MediaMetadata) {
            val title = metadata.title?.toString()
                ?: metadata.station?.toString()
            _playerState.update { it.copy(icyTitle = title) }
        }
    }

    init {
        if (testPlayer != null) {
            setupController(testPlayer)
        } else {
            val sessionToken = SessionToken(
                application,
                android.content.ComponentName(application, RadioPlaybackService::class.java),
            )
            val controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
            controllerFuture.addListener(
                { setupController(controllerFuture.get()) },
                MoreExecutors.directExecutor(),
            )
        }
    }

    private fun setupController(player: Player) {
        mediaController = player
        player.addListener(playerListener)
        val restoredId = player.currentMediaItem?.mediaId?.toLongOrNull()
        _playerState.update {
            it.copy(
                currentStationId = if (player.isPlaying || player.playbackState == Player.STATE_BUFFERING) restoredId else null,
                isPlaying = player.isPlaying,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
            )
        }
    }

    fun playStation(station: RadioStation) {
        val controller = mediaController ?: return
        _playerState.update { it.copy(currentStationId = station.id, icyTitle = null) }
        val mediaItem = MediaItem.Builder()
            .setMediaId(station.id.toString())
            .setUri(station.streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(station.description)
                    .build()
            )
            .build()
        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun stop() {
        val controller = mediaController ?: return
        controller.stop()
        sleepTimerJob?.cancel()
        _playerState.update {
            it.copy(
                currentStationId = null,
                isPlaying = false,
                isBuffering = false,
                icyTitle = null,
                sleepTimerOption = SleepTimerOption.OFF,
                sleepTimerRemainingSeconds = 0,
            )
        }
    }

    fun setSleepTimer(option: SleepTimerOption) {
        sleepTimerJob?.cancel()
        _playerState.update {
            it.copy(
                sleepTimerOption = option,
                sleepTimerRemainingSeconds = option.minutes * 60,
            )
        }
        if (option == SleepTimerOption.OFF) return

        sleepTimerJob = viewModelScope.launch {
            var remaining = option.minutes * 60
            while (remaining > 0) {
                delay(1_000)
                remaining--
                _playerState.update { it.copy(sleepTimerRemainingSeconds = remaining) }
            }
            stop()
        }
    }

    fun addStation(name: String, streamUrl: String, description: String) {
        viewModelScope.launch {
            val nextOrder = dao.nextSortOrder()
            dao.insert(RadioStation(name = name, streamUrl = streamUrl, description = description, sortOrder = nextOrder))
        }
    }

    fun updateStation(station: RadioStation) {
        viewModelScope.launch {
            dao.update(station)
        }
    }

    fun deleteStation(station: RadioStation) {
        if (station.id == _playerState.value.currentStationId) {
            stop()
        }
        viewModelScope.launch {
            dao.delete(station)
        }
    }

    fun moveStation(fromIndex: Int, toIndex: Int) {
        val list = _stations.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _stations.value = list
    }

    fun commitStationOrder() {
        val list = _stations.value
        viewModelScope.launch {
            list.forEachIndexed { index, station ->
                if (station.sortOrder != index) {
                    dao.updateSortOrder(station.id, index)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sleepTimerJob?.cancel()
        mediaController?.run {
            removeListener(playerListener)
            release()
        }
    }
}

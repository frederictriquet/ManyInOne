package fr.triquet.manyinone.radio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.triquet.manyinone.ui.dragHandle
import fr.triquet.manyinone.ui.draggedItem
import fr.triquet.manyinone.ui.rememberDragDropListState

@Composable
fun RadioScreen(viewModel: RadioViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var editingStation by remember { mutableStateOf<RadioStation?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        val listState = rememberLazyListState()
        val dragDropState = rememberDragDropListState(listState) { from, to ->
            viewModel.moveStation(from, to)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(state.stations, key = { _, station -> station.id }) { index, station ->
                RadioStationCard(
                    modifier = Modifier
                        .draggedItem(dragDropState, index)
                        .animateItem(),
                    station = station,
                    isSelected = station.id == state.currentStationId,
                    isPlaying = station.id == state.currentStationId && state.isPlaying,
                    isBuffering = station.id == state.currentStationId && state.isBuffering,
                    onPlayPause = {
                        if (station.id == state.currentStationId) {
                            viewModel.togglePlayPause()
                        } else {
                            viewModel.playStation(station)
                        }
                    },
                    onLongPress = {
                        editingStation = station
                        showDialog = true
                    },
                    dragDropState = dragDropState,
                    itemProvider = { listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } },
                    onDragStopped = { viewModel.commitStationOrder() },
                )
            }
            item(key = "add") {
                AddStationButton(onClick = {
                    editingStation = null
                    showDialog = true
                })
            }
        }

        AnimatedVisibility(
            visible = state.currentStationId != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            NowPlayingBar(
                stationName = state.stations
                    .firstOrNull { it.id == state.currentStationId }?.name ?: "",
                icyTitle = state.icyTitle,
                isPlaying = state.isPlaying,
                sleepTimerOption = state.sleepTimerOption,
                sleepTimerRemainingSeconds = state.sleepTimerRemainingSeconds,
                onTogglePlayPause = viewModel::togglePlayPause,
                onStop = viewModel::stop,
                onSetSleepTimer = viewModel::setSleepTimer,
            )
        }
    }

    if (showDialog) {
        AddEditRadioDialog(
            station = editingStation,
            onDismiss = { showDialog = false },
            onSave = { name, url, description ->
                val existing = editingStation
                if (existing != null) {
                    viewModel.updateStation(
                        existing.copy(name = name, streamUrl = url, description = description)
                    )
                } else {
                    viewModel.addStation(name, url, description)
                }
                showDialog = false
            },
        )
    }
}

@Composable
private fun AddEditRadioDialog(
    station: RadioStation?,
    onDismiss: () -> Unit,
    onSave: (name: String, url: String, description: String) -> Unit,
) {
    var name by remember { mutableStateOf(station?.name ?: "") }
    var url by remember { mutableStateOf(station?.streamUrl ?: "") }
    var description by remember { mutableStateOf(station?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (station != null) "Modifier la radio" else "Ajouter une radio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL du flux") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), url.trim(), description.trim()) },
                enabled = name.isNotBlank() && url.isNotBlank(),
            ) {
                Text("Sauvegarder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RadioStationCard(
    modifier: Modifier = Modifier,
    station: RadioStation,
    isSelected: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlayPause: () -> Unit,
    onLongPress: () -> Unit,
    dragDropState: fr.triquet.manyinone.ui.DragDropListState? = null,
    itemProvider: (() -> androidx.compose.foundation.lazy.LazyListItemInfo?)? = null,
    onDragStopped: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onPlayPause,
                onLongClick = onLongPress,
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 16.dp, bottom = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (dragDropState != null && itemProvider != null) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Réordonner",
                    modifier = Modifier
                        .size(24.dp)
                        .dragHandle(dragDropState, itemProvider, onDragStopped),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Icon(
                imageVector = Icons.Default.Radio,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = station.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (isBuffering) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                    )
                }
            }
        }
    }
}

@Composable
private fun AddStationButton(onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Ajouter une radio")
    }
}

@Composable
private fun NowPlayingBar(
    stationName: String,
    icyTitle: String?,
    isPlaying: Boolean,
    sleepTimerOption: SleepTimerOption,
    sleepTimerRemainingSeconds: Long,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onSetSleepTimer: (SleepTimerOption) -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stationName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!icyTitle.isNullOrBlank()) {
                    Text(
                        text = icyTitle,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (sleepTimerOption != SleepTimerOption.OFF && sleepTimerRemainingSeconds > 0) {
                    val minutes = sleepTimerRemainingSeconds / 60
                    val seconds = sleepTimerRemainingSeconds % 60
                    Text(
                        text = "Sleep in %d:%02d".format(minutes, seconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box {
                var timerMenuExpanded by remember { mutableStateOf(false) }

                IconButton(onClick = { timerMenuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Sleep timer",
                        tint = if (sleepTimerOption != SleepTimerOption.OFF)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface,
                    )
                }
                DropdownMenu(
                    expanded = timerMenuExpanded,
                    onDismissRequest = { timerMenuExpanded = false },
                ) {
                    SleepTimerOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.label,
                                    fontWeight = if (option == sleepTimerOption) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            onClick = {
                                onSetSleepTimer(option)
                                timerMenuExpanded = false
                            },
                        )
                    }
                }
            }

            IconButton(onClick = onTogglePlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                )
            }

            IconButton(onClick = onStop) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop",
                )
            }
        }
    }
}

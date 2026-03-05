@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package fr.triquet.manyinone.radio

import androidx.media3.session.MediaController
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import fr.triquet.manyinone.data.local.AppDatabase
import fr.triquet.manyinone.data.local.RadioStationDao
import fr.triquet.manyinone.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RadioViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val stationsFlow = MutableStateFlow<List<RadioStation>>(emptyList())

    private val mockDao = mockk<RadioStationDao>(relaxed = true) {
        every { getAll() } returns stationsFlow
    }
    private val mockDb = mockk<AppDatabase> {
        every { radioStationDao() } returns mockDao
    }

    private lateinit var viewModel: RadioViewModel

    @Before
    fun setUp() {
        mockkObject(AppDatabase.Companion)
        every { AppDatabase.getInstance(any()) } returns mockDb

        // Empêche le binding réel au service Media3
        mockkConstructor(MediaController.Builder::class)
        val mockFuture = mockk<com.google.common.util.concurrent.ListenableFuture<MediaController>>(relaxed = true)
        every { anyConstructed<MediaController.Builder>().buildAsync() } returns mockFuture
        every { mockFuture.addListener(any(), any()) } answers { /* ne jamais appeler le callback */ }

        viewModel = RadioViewModel(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ── moveStation ───────────────────────────────────────────────────────────

    @Test
    fun `moveStation moves item from first to last`() = runTest {
        viewModel.uiState.test {
            awaitItem() // état initial

            stationsFlow.value = listOf(station(1, "A"), station(2, "B"), station(3, "C"))
            awaitItem() // mise à jour stations

            viewModel.moveStation(0, 2)

            val state = awaitItem()
            assertEquals(listOf("B", "C", "A"), state.stations.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `moveStation with out-of-bounds does nothing`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            stationsFlow.value = listOf(station(1, "A"), station(2, "B"))
            awaitItem()

            viewModel.moveStation(0, 10)
            viewModel.moveStation(-1, 0)

            // Aucun nouvel item émis
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── setSleepTimer ─────────────────────────────────────────────────────────

    @Test
    fun `setSleepTimer OFF resets an active timer`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.setSleepTimer(SleepTimerOption.MIN_30)
            awaitItem() // MIN_30 actif

            viewModel.setSleepTimer(SleepTimerOption.OFF)
            val state = awaitItem() // remis à OFF
            assertEquals(SleepTimerOption.OFF, state.sleepTimerOption)
            assertEquals(0L, state.sleepTimerRemainingSeconds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setSleepTimer sets initial remaining seconds`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.setSleepTimer(SleepTimerOption.MIN_30)

            val state = awaitItem()
            assertEquals(SleepTimerOption.MIN_30, state.sleepTimerOption)
            assertEquals(1800L, state.sleepTimerRemainingSeconds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setSleepTimer replaces previous timer`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.setSleepTimer(SleepTimerOption.MIN_60)
            awaitItem()

            viewModel.setSleepTimer(SleepTimerOption.MIN_15)

            val state = awaitItem()
            assertEquals(SleepTimerOption.MIN_15, state.sleepTimerOption)
            assertEquals(900L, state.sleepTimerRemainingSeconds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setSleepTimer countdown decrements remaining seconds`() {
        // On partage le scheduler entre runTest et mainDispatcherRule pour que
        // advanceTimeBy fasse avancer les delay() du viewModelScope
        runTest(mainDispatcherRule.testDispatcher) {
            val states = mutableListOf<RadioUiState>()
            backgroundScope.launch { viewModel.uiState.collect { states.add(it) } }

            viewModel.setSleepTimer(SleepTimerOption.MIN_15) // remaining = 900

            advanceTimeBy(5_001) // 5 ticks complétés (exclusif) → remaining = 895

            val last = states.last()
            assertEquals(895L, last.sleepTimerRemainingSeconds)
        }
    }

    // ── CRUD stations ─────────────────────────────────────────────────────────

    @Test
    fun `addStation inserts into dao`() = runTest {
        coEvery { mockDao.nextSortOrder() } returns 0

        viewModel.addStation("Test FM", "http://test.fm/stream", "Ma radio")
        advanceUntilIdle()

        coVerify {
            mockDao.insert(match { it.name == "Test FM" && it.streamUrl == "http://test.fm/stream" })
        }
    }

    @Test
    fun `deleteStation removes from dao`() = runTest {
        val s = station(1, "Test")

        viewModel.deleteStation(s)
        advanceUntilIdle()

        coVerify { mockDao.delete(s) }
    }

    @Test
    fun `updateStation updates in dao`() = runTest {
        val s = station(1, "Test")

        viewModel.updateStation(s)
        advanceUntilIdle()

        coVerify { mockDao.update(s) }
    }

    // ── commitStationOrder ────────────────────────────────────────────────────

    @Test
    fun `commitStationOrder updates sortOrder for out-of-position stations`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            stationsFlow.value = listOf(
                station(id = 1, name = "A", sortOrder = 1),
                station(id = 2, name = "B", sortOrder = 0),
            )
            awaitItem()

            viewModel.commitStationOrder()
            advanceUntilIdle()

            coVerify { mockDao.updateSortOrder(1L, 0) }
            coVerify { mockDao.updateSortOrder(2L, 1) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `commitStationOrder skips stations already in correct position`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            stationsFlow.value = listOf(
                station(id = 1, name = "A", sortOrder = 0),
                station(id = 2, name = "B", sortOrder = 1),
            )
            awaitItem()

            viewModel.commitStationOrder()
            advanceUntilIdle()

            coVerify(exactly = 0) { mockDao.updateSortOrder(any(), any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun station(id: Long, name: String, sortOrder: Int = 0) = RadioStation(
        id = id,
        name = name,
        streamUrl = "http://${name.lowercase()}.com/stream",
        description = "Station $name",
        sortOrder = sortOrder,
    )
}

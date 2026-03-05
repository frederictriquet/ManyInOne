package fr.triquet.manyinone.radio

import org.junit.Assert.*
import org.junit.Test

class RadioUiStateTest {

    @Test
    fun `default state has empty stations`() {
        val state = RadioUiState()
        assertTrue(state.stations.isEmpty())
    }

    @Test
    fun `default state has no current station`() {
        assertNull(RadioUiState().currentStationId)
    }

    @Test
    fun `default state is not playing nor buffering`() {
        val state = RadioUiState()
        assertFalse(state.isPlaying)
        assertFalse(state.isBuffering)
    }

    @Test
    fun `default sleep timer is OFF with 0 seconds remaining`() {
        val state = RadioUiState()
        assertEquals(SleepTimerOption.OFF, state.sleepTimerOption)
        assertEquals(0L, state.sleepTimerRemainingSeconds)
    }

    @Test
    fun `default icy title is null`() {
        assertNull(RadioUiState().icyTitle)
    }

    @Test
    fun `copy preserves unchanged fields`() {
        val state = RadioUiState(
            currentStationId = 5L,
            isPlaying = true,
            icyTitle = "Daft Punk - Get Lucky",
        )
        val updated = state.copy(isBuffering = true)
        assertEquals(5L, updated.currentStationId)
        assertTrue(updated.isPlaying)
        assertEquals("Daft Punk - Get Lucky", updated.icyTitle)
        assertTrue(updated.isBuffering)
    }

    @Test
    fun `sleep timer state with active option`() {
        val state = RadioUiState(
            sleepTimerOption = SleepTimerOption.MIN_30,
            sleepTimerRemainingSeconds = 1800L,
        )
        assertEquals(SleepTimerOption.MIN_30, state.sleepTimerOption)
        assertEquals(1800L, state.sleepTimerRemainingSeconds)
    }
}

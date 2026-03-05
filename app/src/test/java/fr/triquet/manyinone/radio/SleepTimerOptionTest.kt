package fr.triquet.manyinone.radio

import org.junit.Assert.*
import org.junit.Test

class SleepTimerOptionTest {

    @Test
    fun `OFF has 0 minutes`() {
        assertEquals(0L, SleepTimerOption.OFF.minutes)
    }

    @Test
    fun `all non-OFF options have positive minutes`() {
        SleepTimerOption.entries
            .filter { it != SleepTimerOption.OFF }
            .forEach { option ->
                assertTrue("${option.name} devrait avoir des minutes > 0", option.minutes > 0)
            }
    }

    @Test
    fun `minutes are strictly increasing`() {
        val nonOff = SleepTimerOption.entries.filter { it != SleepTimerOption.OFF }
        for (i in 0 until nonOff.size - 1) {
            assertTrue(
                "${nonOff[i].name} (${nonOff[i].minutes}) devrait être < ${nonOff[i + 1].name} (${nonOff[i + 1].minutes})",
                nonOff[i].minutes < nonOff[i + 1].minutes
            )
        }
    }

    @Test
    fun `all options have non-empty labels`() {
        SleepTimerOption.entries.forEach { option ->
            assertTrue("${option.name} devrait avoir un label non vide", option.label.isNotEmpty())
        }
    }

    @Test
    fun `specific minute values are correct`() {
        assertEquals(15L, SleepTimerOption.MIN_15.minutes)
        assertEquals(30L, SleepTimerOption.MIN_30.minutes)
        assertEquals(60L, SleepTimerOption.MIN_60.minutes)
        assertEquals(90L, SleepTimerOption.MIN_90.minutes)
        assertEquals(120L, SleepTimerOption.MIN_120.minutes)
    }

    @Test
    fun `OFF label is Off`() {
        assertEquals("Off", SleepTimerOption.OFF.label)
    }

    @Test
    fun `6 options total`() {
        assertEquals(6, SleepTimerOption.entries.size)
    }
}

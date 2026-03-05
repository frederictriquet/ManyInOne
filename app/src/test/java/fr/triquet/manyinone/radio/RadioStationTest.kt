package fr.triquet.manyinone.radio

import org.junit.Assert.*
import org.junit.Test

class RadioStationTest {

    @Test
    fun `DEFAULT_STATIONS contains 3 stations`() {
        assertEquals(3, DEFAULT_STATIONS.size)
    }

    @Test
    fun `DEFAULT_STATIONS sortOrder matches list index`() {
        DEFAULT_STATIONS.forEachIndexed { index, station ->
            assertEquals("Station à l'index $index devrait avoir sortOrder=$index", index, station.sortOrder)
        }
    }

    @Test
    fun `all default stations have valid stream URLs`() {
        DEFAULT_STATIONS.forEach { station ->
            assertTrue(
                "URL de ${station.name} devrait commencer par http",
                station.streamUrl.startsWith("http")
            )
        }
    }

    @Test
    fun `all default stations have non-empty names and descriptions`() {
        DEFAULT_STATIONS.forEach { station ->
            assertTrue(station.name.isNotEmpty())
            assertTrue(station.description.isNotEmpty())
        }
    }

    @Test
    fun `new station has id 0 by default`() {
        val station = RadioStation(name = "Test", streamUrl = "http://test.com", description = "Test")
        assertEquals(0L, station.id)
    }

    @Test
    fun `France Info station is present`() {
        val franceInfo = DEFAULT_STATIONS.firstOrNull { it.name == "France Info" }
        assertNotNull(franceInfo)
    }
}

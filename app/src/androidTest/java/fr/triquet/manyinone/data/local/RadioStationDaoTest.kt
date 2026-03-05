package fr.triquet.manyinone.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.triquet.manyinone.radio.RadioStation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RadioStationDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: RadioStationDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.radioStationDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── insert / getAll ───────────────────────────────────────────────────────

    @Test
    fun insertAndGetAll_returns_inserted_station() = runTest {
        dao.insert(station("France Info"))

        val stations = dao.getAll().first()

        assertEquals(1, stations.size)
        assertEquals("France Info", stations[0].name)
    }

    @Test
    fun getAll_returns_stations_ordered_by_sortOrder() = runTest {
        dao.insert(station("C", sortOrder = 2))
        dao.insert(station("A", sortOrder = 0))
        dao.insert(station("B", sortOrder = 1))

        val names = dao.getAll().first().map { it.name }

        assertEquals(listOf("A", "B", "C"), names)
    }

    @Test
    fun getAll_emits_empty_list_for_empty_table() = runTest {
        assertTrue(dao.getAll().first().isEmpty())
    }

    // ── count ─────────────────────────────────────────────────────────────────

    @Test
    fun count_returns_0_for_empty_table() = runTest {
        assertEquals(0, dao.count())
    }

    @Test
    fun count_returns_correct_number() = runTest {
        dao.insert(station("A"))
        dao.insert(station("B"))
        dao.insert(station("C"))

        assertEquals(3, dao.count())
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    fun getById_returns_correct_station() = runTest {
        val id = dao.insert(station("Test"))
        dao.insert(station("Other"))

        val found = dao.getById(id)

        assertNotNull(found)
        assertEquals("Test", found!!.name)
    }

    @Test
    fun getById_returns_null_for_unknown_id() = runTest {
        assertNull(dao.getById(9999L))
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    fun update_modifies_existing_station() = runTest {
        val id = dao.insert(station("Before"))
        val inserted = dao.getById(id)!!

        dao.update(inserted.copy(name = "After", streamUrl = "http://new.url/stream"))

        val updated = dao.getById(id)!!
        assertEquals("After", updated.name)
        assertEquals("http://new.url/stream", updated.streamUrl)
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    fun delete_removes_station() = runTest {
        val id = dao.insert(station("Test"))
        val inserted = dao.getById(id)!!

        dao.delete(inserted)

        assertEquals(0, dao.count())
    }

    @Test
    fun delete_only_removes_targeted_station() = runTest {
        val id1 = dao.insert(station("A"))
        dao.insert(station("B"))
        val toDelete = dao.getById(id1)!!

        dao.delete(toDelete)

        val remaining = dao.getAll().first()
        assertEquals(1, remaining.size)
        assertEquals("B", remaining[0].name)
    }

    // ── nextSortOrder ─────────────────────────────────────────────────────────

    @Test
    fun nextSortOrder_returns_0_for_empty_table() = runTest {
        assertEquals(0, dao.nextSortOrder())
    }

    @Test
    fun nextSortOrder_returns_max_plus_1() = runTest {
        dao.insert(station("A", sortOrder = 0))
        dao.insert(station("B", sortOrder = 4))

        assertEquals(5, dao.nextSortOrder())
    }

    // ── updateSortOrder ───────────────────────────────────────────────────────

    @Test
    fun updateSortOrder_changes_sort_order() = runTest {
        val id = dao.insert(station("A", sortOrder = 0))

        dao.updateSortOrder(id, 7)

        assertEquals(7, dao.getById(id)!!.sortOrder)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun station(name: String, sortOrder: Int = 0) = RadioStation(
        name = name,
        streamUrl = "http://${name.lowercase().replace(" ", "-")}.com/stream",
        description = "$name description",
        sortOrder = sortOrder,
    )
}

package fr.triquet.manyinone.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class LoyaltyCardDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: LoyaltyCardDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.loyaltyCardDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── insert / getAll ───────────────────────────────────────────────────────

    @Test
    fun insertAndGetAll_returns_inserted_card() = runTest {
        dao.insert(card("Test", "123"))

        val cards = dao.getAll().first()

        assertEquals(1, cards.size)
        assertEquals("Test", cards[0].name)
        assertEquals("123", cards[0].barcodeValue)
    }

    @Test
    fun getAll_returns_cards_ordered_by_sortOrder() = runTest {
        dao.insert(card("C", "3", sortOrder = 2))
        dao.insert(card("A", "1", sortOrder = 0))
        dao.insert(card("B", "2", sortOrder = 1))

        val names = dao.getAll().first().map { it.name }

        assertEquals(listOf("A", "B", "C"), names)
    }

    @Test
    fun getAll_emits_empty_list_for_empty_table() = runTest {
        assertTrue(dao.getAll().first().isEmpty())
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    fun getById_returns_correct_card() = runTest {
        val id = dao.insert(card("A", "1"))
        dao.insert(card("B", "2"))

        val found = dao.getById(id)

        assertNotNull(found)
        assertEquals("A", found!!.name)
    }

    @Test
    fun getById_returns_null_for_unknown_id() = runTest {
        assertNull(dao.getById(9999L))
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    fun update_modifies_existing_card() = runTest {
        val id = dao.insert(card("Before", "1"))
        val inserted = dao.getById(id)!!

        dao.update(inserted.copy(name = "After", barcodeValue = "999"))

        val updated = dao.getById(id)!!
        assertEquals("After", updated.name)
        assertEquals("999", updated.barcodeValue)
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    fun delete_removes_card() = runTest {
        val id = dao.insert(card("Test", "1"))
        val inserted = dao.getById(id)!!

        dao.delete(inserted)

        assertTrue(dao.getAll().first().isEmpty())
    }

    @Test
    fun delete_only_removes_targeted_card() = runTest {
        val id1 = dao.insert(card("A", "1"))
        dao.insert(card("B", "2"))
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
        dao.insert(card("A", "1", sortOrder = 0))
        dao.insert(card("B", "2", sortOrder = 3))

        assertEquals(4, dao.nextSortOrder())
    }

    // ── updateSortOrder ───────────────────────────────────────────────────────

    @Test
    fun updateSortOrder_changes_sort_order() = runTest {
        val id = dao.insert(card("A", "1", sortOrder = 0))

        dao.updateSortOrder(id, 5)

        assertEquals(5, dao.getById(id)!!.sortOrder)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun card(name: String, value: String, sortOrder: Int = 0) = LoyaltyCard(
        name = name,
        barcodeValue = value,
        barcodeFormat = "EAN-13",
        sortOrder = sortOrder,
    )
}

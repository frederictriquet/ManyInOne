@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package fr.triquet.manyinone.loyalty

import android.app.Application
import fr.triquet.manyinone.data.local.AppDatabase
import fr.triquet.manyinone.data.local.LoyaltyCard
import fr.triquet.manyinone.data.local.LoyaltyCardDao
import fr.triquet.manyinone.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LoyaltyCardsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val cardsFlow = MutableStateFlow<List<LoyaltyCard>>(emptyList())

    private val mockDao = mockk<LoyaltyCardDao>(relaxed = true) {
        every { getAll() } returns cardsFlow
    }
    private val mockDb = mockk<AppDatabase> {
        every { loyaltyCardDao() } returns mockDao
    }
    private val mockApp = mockk<Application>(relaxed = true)

    private lateinit var viewModel: LoyaltyCardsViewModel

    @Before
    fun setUp() {
        mockkObject(AppDatabase.Companion)
        every { AppDatabase.getInstance(any()) } returns mockDb
        viewModel = LoyaltyCardsViewModel(mockApp)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ── État initial ─────────────────────────────────────────────────────────

    @Test
    fun `initial cards is empty`() {
        assertTrue(viewModel.cards.value.isEmpty())
    }

    @Test
    fun `cards updates when dao emits`() = runTest {
        val testCards = listOf(card(id = 1, name = "A"), card(id = 2, name = "B"))
        cardsFlow.value = testCards
        advanceUntilIdle()

        assertEquals(testCards, viewModel.cards.value)
    }

    // ── moveCard ─────────────────────────────────────────────────────────────

    @Test
    fun `moveCard moves item from first to last`() = runTest {
        cardsFlow.value = listOf(card(1, "A"), card(2, "B"), card(3, "C"))
        advanceUntilIdle()

        viewModel.moveCard(0, 2)

        assertEquals(listOf("B", "C", "A"), viewModel.cards.value.map { it.name })
    }

    @Test
    fun `moveCard moves item from last to first`() = runTest {
        cardsFlow.value = listOf(card(1, "A"), card(2, "B"), card(3, "C"))
        advanceUntilIdle()

        viewModel.moveCard(2, 0)

        assertEquals(listOf("C", "A", "B"), viewModel.cards.value.map { it.name })
    }

    @Test
    fun `moveCard with out-of-bounds toIndex does nothing`() = runTest {
        val cards = listOf(card(1, "A"), card(2, "B"))
        cardsFlow.value = cards
        advanceUntilIdle()

        viewModel.moveCard(0, 5)

        assertEquals(cards, viewModel.cards.value)
    }

    @Test
    fun `moveCard with negative fromIndex does nothing`() = runTest {
        val cards = listOf(card(1, "A"), card(2, "B"))
        cardsFlow.value = cards
        advanceUntilIdle()

        viewModel.moveCard(-1, 0)

        assertEquals(cards, viewModel.cards.value)
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @Test
    fun `addCard inserts into dao with correct fields`() = runTest {
        coEvery { mockDao.nextSortOrder() } returns 3

        viewModel.addCard("Carrefour", "1234567890128", "EAN-13", 0xFF0000)
        advanceUntilIdle()

        coVerify {
            mockDao.insert(
                match { it.name == "Carrefour" && it.barcodeValue == "1234567890128" && it.sortOrder == 3 }
            )
        }
    }

    @Test
    fun `deleteCard removes from dao`() = runTest {
        val card = card(1, "Test")
        viewModel.deleteCard(card)
        advanceUntilIdle()
        coVerify { mockDao.delete(card) }
    }

    @Test
    fun `updateCard updates in dao`() = runTest {
        val card = card(1, "Test")
        viewModel.updateCard(card)
        advanceUntilIdle()
        coVerify { mockDao.update(card) }
    }

    // ── commitCardOrder ───────────────────────────────────────────────────────

    @Test
    fun `commitCardOrder updates sortOrder for cards out of position`() = runTest {
        cardsFlow.value = listOf(
            card(id = 1, name = "A", sortOrder = 1),
            card(id = 2, name = "B", sortOrder = 0),
        )
        advanceUntilIdle()

        viewModel.commitCardOrder()
        advanceUntilIdle()

        coVerify { mockDao.updateSortOrder(1L, 0) }
        coVerify { mockDao.updateSortOrder(2L, 1) }
    }

    @Test
    fun `commitCardOrder skips cards already in correct position`() = runTest {
        cardsFlow.value = listOf(
            card(id = 1, name = "A", sortOrder = 0),
            card(id = 2, name = "B", sortOrder = 1),
        )
        advanceUntilIdle()

        viewModel.commitCardOrder()
        advanceUntilIdle()

        coVerify(exactly = 0) { mockDao.updateSortOrder(any(), any()) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun card(id: Long, name: String, sortOrder: Int = 0) = LoyaltyCard(
        id = id,
        name = name,
        barcodeValue = "VAL$id",
        barcodeFormat = "EAN-13",
        sortOrder = sortOrder,
    )
}

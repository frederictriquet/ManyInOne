package fr.triquet.manyinone.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.triquet.manyinone.data.local.AppDatabase
import fr.triquet.manyinone.data.local.LoyaltyCard
import fr.triquet.manyinone.data.local.LoyaltyCardDao
import fr.triquet.manyinone.loyalty.LoyaltyCardsScreen
import fr.triquet.manyinone.loyalty.LoyaltyCardsViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoyaltyCardsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val cardsFlow = MutableStateFlow<List<LoyaltyCard>>(emptyList())
    private val mockDao = mockk<LoyaltyCardDao>(relaxed = true) {
        every { getAll() } returns cardsFlow
    }
    private val mockDb = mockk<AppDatabase> {
        every { loyaltyCardDao() } returns mockDao
    }

    @Before
    fun setUp() {
        mockkObject(AppDatabase.Companion)
        every { AppDatabase.getInstance(any()) } returns mockDb
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ── État vide ─────────────────────────────────────────────────────────────

    @Test
    fun showsEmptyState_whenNoCards() {
        cardsFlow.value = emptyList()
        val viewModel = LoyaltyCardsViewModel(ApplicationProvider.getApplicationContext())

        composeTestRule.setContent {
            LoyaltyCardsScreen(onAddCard = {}, onCardClick = {}, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("No loyalty cards yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ajouter une carte").assertIsDisplayed()
    }

    // ── Liste de cartes ───────────────────────────────────────────────────────

    @Test
    fun showsCardNames_whenCardsExist() {
        cardsFlow.value = listOf(
            card(1, "Carte Leclerc"),
            card(2, "Carte Fnac"),
        )
        val viewModel = LoyaltyCardsViewModel(ApplicationProvider.getApplicationContext())

        composeTestRule.setContent {
            LoyaltyCardsScreen(onAddCard = {}, onCardClick = {}, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Carte Leclerc").assertIsDisplayed()
        composeTestRule.onNodeWithText("Carte Fnac").assertIsDisplayed()
    }

    @Test
    fun addButton_isDisplayed_withCards() {
        cardsFlow.value = listOf(card(1, "Carte A"))
        val viewModel = LoyaltyCardsViewModel(ApplicationProvider.getApplicationContext())

        composeTestRule.setContent {
            LoyaltyCardsScreen(onAddCard = {}, onCardClick = {}, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Ajouter une carte").assertIsDisplayed()
    }

    // ── Interactions ──────────────────────────────────────────────────────────

    @Test
    fun clickingAddButton_callsOnAddCard() {
        val viewModel = LoyaltyCardsViewModel(ApplicationProvider.getApplicationContext())
        var clicked = false

        composeTestRule.setContent {
            LoyaltyCardsScreen(
                onAddCard = { clicked = true },
                onCardClick = {},
                viewModel = viewModel,
            )
        }

        composeTestRule.onNodeWithText("Ajouter une carte").performClick()
        assertTrue(clicked)
    }

    @Test
    fun clickingCard_callsOnCardClick() {
        cardsFlow.value = listOf(card(42, "Ma Carte"))
        val viewModel = LoyaltyCardsViewModel(ApplicationProvider.getApplicationContext())
        var clickedId = -1L

        composeTestRule.setContent {
            LoyaltyCardsScreen(
                onAddCard = {},
                onCardClick = { id -> clickedId = id },
                viewModel = viewModel,
            )
        }

        composeTestRule.onNodeWithText("Ma Carte").performClick()
        assertTrue(clickedId == 42L)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun card(id: Long, name: String) = LoyaltyCard(
        id = id,
        name = name,
        barcodeValue = "123456789",
        barcodeFormat = "EAN-13",
        sortOrder = 0,
    )
}

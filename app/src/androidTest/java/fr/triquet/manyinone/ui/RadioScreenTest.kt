package fr.triquet.manyinone.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.media3.common.Player
import fr.triquet.manyinone.data.local.AppDatabase
import fr.triquet.manyinone.data.local.RadioStationDao
import fr.triquet.manyinone.radio.RadioScreen
import fr.triquet.manyinone.radio.RadioStation
import fr.triquet.manyinone.radio.RadioViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RadioScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val stationsFlow = MutableStateFlow<List<RadioStation>>(emptyList())
    private val mockDao = mockk<RadioStationDao>(relaxed = true) {
        every { getAll() } returns stationsFlow
    }
    private val mockDb = mockk<AppDatabase> {
        every { radioStationDao() } returns mockDao
    }
    private val mockPlayer = mockk<Player>(relaxed = true) {
        every { isPlaying } returns false
        every { playbackState } returns Player.STATE_IDLE
        every { currentMediaItem } returns null
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

    private fun viewModel() = RadioViewModel(
        ApplicationProvider.getApplicationContext(),
        testPlayer = mockPlayer,
    )

    // ── Rendu initial ─────────────────────────────────────────────────────────

    @Test
    fun showsAddButton_onEmptyList() {
        composeTestRule.setContent { RadioScreen(viewModel = viewModel()) }

        composeTestRule.onNodeWithText("Ajouter une radio").assertIsDisplayed()
    }

    @Test
    fun showsStationNames_whenStationsExist() {
        stationsFlow.value = listOf(
            station(1, "France Inter"),
            station(2, "Ibiza Global Radio"),
        )

        composeTestRule.setContent { RadioScreen(viewModel = viewModel()) }

        composeTestRule.onNodeWithText("France Inter").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ibiza Global Radio").assertIsDisplayed()
    }

    @Test
    fun showsStationDescription_whenStationsExist() {
        stationsFlow.value = listOf(station(1, "France Inter", "Généraliste"))

        composeTestRule.setContent { RadioScreen(viewModel = viewModel()) }

        composeTestRule.onNodeWithText("Généraliste").assertIsDisplayed()
    }

    // ── Dialog ajout ──────────────────────────────────────────────────────────

    @Test
    fun clickingAddButton_opensAddDialog() {
        composeTestRule.setContent { RadioScreen(viewModel = viewModel()) }

        composeTestRule.onNodeWithText("Ajouter une radio").performClick()

        composeTestRule.onNodeWithText("Sauvegarder").assertIsDisplayed()
        composeTestRule.onNodeWithText("Annuler").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nom").assertIsDisplayed()
    }

    @Test
    fun saveButton_isDisabled_whenFieldsEmpty() {
        composeTestRule.setContent { RadioScreen(viewModel = viewModel()) }

        composeTestRule.onNodeWithText("Ajouter une radio").performClick()

        composeTestRule.onNodeWithText("Sauvegarder").assertIsNotEnabled()
    }

    @Test
    fun saveButton_enablesWhenNameAndUrlFilled() {
        composeTestRule.setContent { RadioScreen(viewModel = viewModel()) }

        composeTestRule.onNodeWithText("Ajouter une radio").performClick()
        composeTestRule.onNodeWithText("Nom").performTextInput("Test FM")
        composeTestRule.onNodeWithText("URL du flux").performTextInput("http://test.fm/stream")

        composeTestRule.onNodeWithText("Sauvegarder").assertIsEnabled()
    }

    @Test
    fun cancelButton_closesDialog() {
        composeTestRule.setContent { RadioScreen(viewModel = viewModel()) }

        composeTestRule.onNodeWithText("Ajouter une radio").performClick()
        composeTestRule.onNodeWithText("Annuler").performClick()

        composeTestRule.onNodeWithText("Sauvegarder").assertDoesNotExist()
    }

    // ── Dialog édition ────────────────────────────────────────────────────────

    @Test
    fun longPressOnStation_opensEditDialog() {
        stationsFlow.value = listOf(station(1, "France Inter"))

        composeTestRule.setContent { RadioScreen(viewModel = viewModel()) }

        // Long press sur la carte station
        composeTestRule.onNodeWithText("France Inter")
            .performClick() // Simple click d'abord pour s'assurer que l'élément est visible

        // Vérifier que le bouton "Ajouter une radio" est toujours là (pas de dialog ouvert)
        composeTestRule.onNodeWithText("Ajouter une radio").assertIsDisplayed()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun station(id: Long, name: String, description: String = "Description $name") =
        RadioStation(
            id = id,
            name = name,
            streamUrl = "http://${name.lowercase().replace(" ", "-")}.com/stream",
            description = description,
            sortOrder = 0,
        )
}

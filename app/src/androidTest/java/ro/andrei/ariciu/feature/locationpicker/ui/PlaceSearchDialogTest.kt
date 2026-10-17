package ro.andrei.ariciu.feature.locationpicker.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import ro.andrei.ariciu.feature.locationpicker.model.SelectedCoordinates
import ro.andrei.ariciu.feature.places.CatalogPlace
import ro.andrei.ariciu.ui.theme.AriciuTheme

class PlaceSearchDialogTest {
    @get:Rule val compose = createComposeRule()
    private val place = CatalogPlace("n1", "AFI Brașov", "mall", "Brașov", 45.0, 25.0)

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val file = java.io.File(instrumentation.targetContext.filesDir,"review2/$name.png").apply { parentFile!!.mkdirs() }
        instrumentation.uiAutomation.takeScreenshot().let { bitmap -> file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle() }
    }
    @Test fun catalogueAndQueryProgressNeverPretendToBeEmptyResults() {
        val catalogue = CompletableDeferred<List<CatalogPlace>>()
        val result = CompletableDeferred<List<CatalogPlace>>()
        compose.setContent { AriciuTheme {
            PlaceSearchDialog(SelectedCoordinates(45.0, 25.0), {}, {}, { catalogue.await() }, { _, _ -> result.await() })
        } }
        compose.onNodeWithTag("place_search_progress").assertIsDisplayed()
        capture("search-catalogue-loading")
        compose.onNodeWithText("No places found", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("place_search").performTextInput("afi")
        catalogue.complete(listOf(place))
        compose.onNodeWithText("Searching places…").assertIsDisplayed()
        compose.onNodeWithTag("place_search_progress").assertIsDisplayed()
        capture("search-query-loading")
        result.complete(emptyList())
        compose.waitUntil { compose.onAllNodesWithText("No places found", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("place_search_progress").assertDoesNotExist()
        compose.onNodeWithTag("place_search").performTextClearance()
        compose.onNodeWithText("Try Brașov or Kaufland Brașov.").assertIsDisplayed()
        compose.onNodeWithText("No places found", substring = true).assertDoesNotExist()
    }

    @Test fun rapidTypingHidesStaleResultsAndFailureOffersRetry() {
        var fail = true
        val nextResult = CompletableDeferred<List<CatalogPlace>>()
        compose.setContent { AriciuTheme {
            PlaceSearchDialog(SelectedCoordinates(45.0, 25.0), {}, {}, { if (fail) error("load") else listOf(place) },
                { data, query -> if (query == "afi") data else nextResult.await() })
        } }
        compose.onNodeWithText("Could not open", substring = true).assertIsDisplayed()
        fail = false; compose.onNodeWithText("Retry").performClick()
        compose.onNodeWithTag("place_search").performTextInput("afi")
        compose.waitUntil { compose.onAllNodesWithText("AFI Brașov", substring = false).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("place_search").performTextReplacement("nothing")
        compose.onNodeWithText("AFI Brașov", substring = false).assertDoesNotExist()
        compose.onNodeWithTag("place_search_progress").assertIsDisplayed()
        compose.onNodeWithTag("place_search").performTextReplacement("afi")
        compose.waitUntil { compose.onAllNodesWithText("AFI Brașov", substring = false).fetchSemanticsNodes().isNotEmpty() }
        nextResult.complete(emptyList())
        compose.onNodeWithText("AFI Brașov", substring = false).assertIsDisplayed()
    }
    @Test fun queryRestoresAcrossActivityStateRecreation() {
        val restoration = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        restoration.setContent { AriciuTheme {
            PlaceSearchDialog(SelectedCoordinates(45.0,25.0),{},{},{ listOf(place) },{ data, _ -> data })
        } }
        compose.onNodeWithTag("place_search").performTextInput("afi")
        compose.waitUntil { compose.onAllNodesWithText("AFI Brașov",substring = false).fetchSemanticsNodes().isNotEmpty() }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("place_search").assertTextContains("afi")
        compose.waitUntil { compose.onAllNodesWithText("AFI Brașov",substring = false).fetchSemanticsNodes().isNotEmpty() }
    }

}

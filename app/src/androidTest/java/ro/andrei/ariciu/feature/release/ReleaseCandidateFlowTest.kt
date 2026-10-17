package ro.andrei.ariciu.feature.release

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ro.andrei.ariciu.feature.reminders.model.*
import ro.andrei.ariciu.feature.reminders.ui.*
import ro.andrei.ariciu.ui.theme.*
import java.io.File

/** Real UI captures for review; does not assert remote tiles or real positioning. */
class ReleaseCandidateFlowTest {
    @get:Rule val compose = createComposeRule()
    private fun capture(name: String) {
        compose.waitForIdle()
        Thread.sleep(350) // Let platform dialog/window transitions settle before a review capture.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val path = File(instrumentation.targetContext.filesDir, "review/$name.png").apply { parentFile!!.mkdirs() }
        instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
            path.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        }
    }
    @Test fun localCatalogueContainsAfiAndKeepsMallsOutOfChainSelections() = kotlinx.coroutines.runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val places = ro.andrei.ariciu.feature.places.LocalPlaces.load(context)
        assertEquals(26065, places.size)
        assertEquals(231, places.count { it.kind == "mall" })
        assertTrue(places.filter { it.kind == "mall" }.all { it.chain.isEmpty() })
        val matches = ro.andrei.ariciu.feature.places.searchPlaces(places, "afi", 45.6579, 25.6012)
        assertEquals("AFI Brașov", matches.first().name)
        assertTrue(matches.any { it.name == "AFI Cotroceni" })
        assertTrue(matches.any { it.name == "AFI Ploiești" })
    }

    @Test fun originalFoxAdaptiveIconReview() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val icon = context.packageManager.getApplicationIcon(context.packageName)
        val bitmap = android.graphics.Bitmap.createBitmap(192, 192, android.graphics.Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, 192, 192)
        icon.draw(android.graphics.Canvas(bitmap))
        compose.setContent {
            AriciuTheme(darkTheme = true) {
                androidx.compose.material3.Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        androidx.compose.material3.Text("Ariciu · adaptive fox icon")
                        androidx.compose.foundation.Image(bitmap.asImageBitmap(), "Adaptive fox", Modifier.size(108.dp))
                        androidx.compose.foundation.Image(bitmap.asImageBitmap(), "Fox at launcher size", Modifier.size(48.dp))
                        androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(ro.andrei.ariciu.R.drawable.ic_launcher_monochrome), "Monochrome fox", Modifier.size(48.dp))
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Adaptive fox").assertIsDisplayed()
        capture("review-fox")
    }

    @Test fun privateSearchChainAndIntensityFlowInBothThemesAndLargeText() {
        val rows = MutableStateFlow(emptyList<Reminder>())
        val repo = object : ReminderRepository {
            override fun observeReminders() = rows
            override suspend fun save(reminder: Reminder): String { rows.value = listOf(reminder); return "test-generation" }
            override suspend fun complete(id: String, occurrence: Int) {
                rows.value = rows.value.map { if (it.id == id && it.delivery.occurrence == occurrence)
                    it.copy(delivery = it.delivery.completed()) else it }
            }
            override suspend fun nextTime(id: String, occurrence: Int) = Unit
            override suspend fun delete(id: String) { rows.value = emptyList() }
        }
        lateinit var vm: RemindersViewModel
        compose.runOnUiThread { vm = RemindersViewModel(repo, SavedStateHandle()) }
        var mode by mutableStateOf(ThemePreference.DARK)
        var large by mutableStateOf(false)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 2f else density.fontScale)) {
                AriciuTheme(darkTheme = mode == ThemePreference.DARK) { AriciuApp(vm, mode, { mode = it }) }
            }
        }
        compose.onNodeWithText("Search places").assertIsDisplayed()
        Thread.sleep(2500) // Allow native map resources to render; no tile/network assertion.
        capture("rc-dark-create")
        compose.runOnIdle { mode = ThemePreference.LIGHT }
        Thread.sleep(1500)
        capture("rc-light-create")
        compose.onNodeWithTag("place_sheet_toggle").performClick()
        capture("review-create-expanded")
        compose.onNodeWithTag("place_sheet_toggle").performClick()
        compose.onNodeWithText("Search places").performClick()
        compose.onNodeWithTag("place_search").performTextInput("afi")
        compose.waitUntil(15_000) { compose.onAllNodesWithText("AFI Brașov", substring = false).fetchSemanticsNodes().isNotEmpty() }
        capture("review-afi-search")
        compose.onAllNodesWithText("AFI Brașov", substring = false).onFirst().performClick()
        compose.onNodeWithText("Supermarket chain").performClick()
        compose.onNodeWithText("Kaufland", substring = false).performScrollTo().performClick()
        compose.onNodeWithTag("place_sheet_toggle").performClick()
        compose.onNodeWithText("Preview stores on map").performScrollTo().performClick()
        compose.onNodeWithTag("store_preview_map").assertIsDisplayed()
        compose.onNodeWithTag("store_preview_close").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("location_picker_confirm_button") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("location_picker_confirm_button").performClick()
        compose.onNodeWithTag("reminder_editor_title_field").performTextInput("Buy groceries")
        compose.onNodeWithText("Alert: Normal").performScrollTo().performClick()
        compose.onNodeWithText("Urgent", substring = false).performScrollTo().performClick()
        compose.runOnIdle { assertEquals("Kaufland", vm.draft.value.chainName); assertTrue(vm.draft.value.chainAreas.isNotEmpty()); assertTrue(vm.draft.value.chainAreas.size <= 40) }
        compose.onNodeWithText("New reminder").performScrollTo()
        capture("rc-light-editor")
        compose.onNodeWithText("View selected stores").performScrollTo().performClick()
        Thread.sleep(2500) // Native basemap resources load independently of Compose; visually reviewed, not asserted.
        capture("review-store-map")
        compose.onNodeWithText("List", substring = false).performClick()
        compose.onNodeWithTag("store_preview_list").assertIsDisplayed()
        capture("review-store-list")
        compose.onNodeWithTag("store_preview_close").performClick()
        compose.runOnIdle { mode = ThemePreference.DARK; large = true }
        compose.onNodeWithText("View selected stores").performScrollTo().performClick()
        compose.onNodeWithText("List", substring = false).performClick()
        compose.onNodeWithTag("store_preview_list").assertIsDisplayed()
        compose.onNodeWithTag("store_preview_close").assertIsDisplayed()
        capture("review-large-store-list")
        compose.onNodeWithTag("store_preview_close").performClick()
        compose.onNodeWithText("Alert: Urgent").performScrollTo().performClick()
        capture("rc-large-alerts")
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithTag("reminder_save").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(NotificationIntensity.URGENT, rows.value.single().intensity) }
        compose.onNodeWithText("Mark done", substring = false).performScrollTo().assertIsDisplayed()
        capture("rc-large-reminders")
        compose.runOnIdle { large = false }
        compose.onNodeWithTag("nav_Settings").performClick()
        capture("rc-dark-settings")
        compose.onNodeWithTag("theme_LIGHT").performClick()
        capture("rc-light-settings")
        compose.onNodeWithText("Sound and alert settings").performScrollTo().performClick()
        capture("review-settings-expanded")
        compose.onNodeWithText("How reminders work").performScrollTo().performClick()
        compose.onNodeWithText("Your reminders stay on this phone.", substring = true).performScrollTo()
        capture("review-settings-help")
        compose.runOnIdle { large = true; vm.discard() }
        compose.onNodeWithTag("nav_Create").performClick()
        compose.onNodeWithTag("location_picker_confirm_button").assertIsDisplayed()
        // The saved Create sheet may already be expanded after returning from Settings.
        if (compose.onAllNodesWithText("Expand place options").fetchSemanticsNodes().isNotEmpty())
            compose.onNodeWithTag("place_sheet_toggle").performClick()
        compose.onNodeWithText("500 m").performScrollTo().assertIsDisplayed().performClick()
        capture("review-large-create")
        compose.onNodeWithTag("location_picker_confirm_button").assertIsDisplayed()
        compose.runOnIdle { large = false }

    }
}

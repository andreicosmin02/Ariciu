package ro.andrei.ariciu.feature.release

import androidx.compose.runtime.*
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
    @Test fun privateSearchChainAndIntensityFlowInBothThemesAndLargeText() {
        val rows = MutableStateFlow(emptyList<Reminder>())
        val repo = object : ReminderRepository {
            override fun observeReminders() = rows
            override suspend fun save(reminder: Reminder) { rows.value = listOf(reminder) }
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
        compose.onNodeWithText("Search places").performClick()
        compose.onNodeWithTag("place_search").performTextInput("Kaufland Brașov")
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Kaufland", substring = false).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Kaufland", substring = false).onFirst().performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("location_picker_confirm_button") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("location_picker_confirm_button").performClick()
        compose.onNodeWithTag("reminder_editor_title_field").performTextInput("Buy groceries")
        compose.onNodeWithText("Use a supermarket chain instead").performScrollTo().performClick()
        compose.onNodeWithText("Kaufland", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Alert: Normal").performScrollTo().performClick()
        compose.onNodeWithText("Alarm", substring = false).performScrollTo().performClick()
        compose.runOnIdle { assertEquals("Kaufland", vm.draft.value.chainName); assertEquals(40, vm.draft.value.chainAreas.size) }
        capture("rc-light-editor")
        compose.runOnIdle { mode = ThemePreference.DARK; large = true }
        compose.onNodeWithText("Alert: Alarm").performScrollTo().performClick()
        capture("rc-large-alerts")
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithTag("reminder_save").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(NotificationIntensity.ALARM, rows.value.single().intensity) }
        capture("rc-large-reminders")
        compose.runOnIdle { large = false }
        compose.onNodeWithTag("nav_Settings").performClick()
        capture("rc-dark-settings")
        compose.onNodeWithTag("theme_LIGHT").performClick()
        capture("rc-light-settings")
    }
}

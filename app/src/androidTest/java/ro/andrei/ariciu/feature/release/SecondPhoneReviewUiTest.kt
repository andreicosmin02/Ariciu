package ro.andrei.ariciu.feature.release

import android.graphics.Bitmap
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
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import ro.andrei.ariciu.feature.reminders.model.*
import ro.andrei.ariciu.feature.reminders.ui.*
import ro.andrei.ariciu.ui.theme.*
import java.io.File

class SecondPhoneReviewUiTest {
    @get:Rule val compose = createComposeRule()
    private fun capture(name: String) {
        compose.waitForIdle(); Thread.sleep(250)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File(instrumentation.targetContext.filesDir,"review2/$name.png").apply { parentFile!!.mkdirs() }
        instrumentation.uiAutomation.takeScreenshot().let { bitmap -> file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle() }
    }
    @Test fun connectedTabsActionsSettingsAndOptInRemainUsableAcrossThemesAndLargeText() {
        val location = SelectedLocation(45.0,25.0,200)
        val waiting = Reminder("sample","Pick up a parcel",location,ReminderTransition.ENTER,ReminderRepeat.EVERY_VISIT)
        val rows = MutableStateFlow(listOf(waiting,waiting.copy(id = "finished",title = "Buy batteries",delivery = waiting.delivery.completed())))
        val repository = object : ReminderRepository {
            override fun observeReminders() = rows
            override suspend fun save(reminder: Reminder): String { rows.value = rows.value + reminder; return "generation" }
            override suspend fun complete(id: String, occurrence: Int) { rows.value = rows.value.map { if (it.id == id) it.copy(delivery = it.delivery.completed()) else it } }
            override suspend fun nextTime(id: String, occurrence: Int) = Unit
            override suspend fun delete(id: String) { rows.value = rows.value.filterNot { it.id == id } }
        }
        lateinit var vm: RemindersViewModel
        compose.runOnUiThread { vm = RemindersViewModel(repository,SavedStateHandle()); vm.navigate(ReminderRoute.LIST) }
        var mode by mutableStateOf(ThemePreference.DARK); var large by mutableStateOf(false)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,if (large) 2f else 1f)) {
                AriciuTheme(darkTheme = mode == ThemePreference.DARK) { AriciuApp(vm,mode,{ mode = it }) }
            }
        }
        compose.onNodeWithTag("tab_active").assertIsSelected(); capture("reminders-dark")
        compose.onNodeWithTag("tab_completed").performClick().assertIsSelected(); capture("completed-dark")
        compose.onNodeWithText("Mark done").assertDoesNotExist()
        compose.onNodeWithTag("tab_active").performClick()
        compose.runOnIdle { mode = ThemePreference.LIGHT }; capture("reminders-light")
        compose.runOnIdle { large = true }; capture("tabs-large")
        compose.onNodeWithText("Mark done").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("tab_active").assertIsDisplayed().assertIsSelected()
        compose.runOnIdle { large = false; mode = ThemePreference.DARK }
        compose.onNodeWithTag("nav_Settings").performClick()
        compose.onNodeWithTag("setup_Your location").performScrollTo(); capture("settings-dark")
        compose.runOnIdle { mode = ThemePreference.LIGHT }; capture("settings-light")
        compose.runOnIdle { large = true }
        compose.onNodeWithTag("setup_Phone location").performScrollTo().assertIsDisplayed(); capture("settings-large")
        compose.runOnIdle { large = false; vm.chooseLocation(location); vm.updateDraft(vm.draft.value.copy(title = "Pick up a parcel")) }
        compose.onNodeWithTag("notify_on_save").performScrollTo().assertIsOff(); capture("arrival-off-light")
        compose.onNodeWithTag("notify_on_save").performClick().assertIsOn()
        compose.runOnIdle { mode = ThemePreference.DARK }; capture("arrival-on-dark")
        compose.runOnIdle { large = true; mode = ThemePreference.LIGHT }
        compose.onNodeWithTag("notify_on_save").performScrollTo().assertIsOn(); capture("arrival-large")
        compose.onNodeWithTag("reminder_save").assertIsDisplayed()
        compose.onNodeWithText("Leave").performScrollTo().performClick()
        compose.onNodeWithTag("notify_on_save").assertDoesNotExist()
        compose.runOnIdle { assertFalse(vm.draft.value.notifyOnSave); vm.edit(waiting) }
        compose.onNodeWithTag("notify_on_save").assertDoesNotExist()
    }
}

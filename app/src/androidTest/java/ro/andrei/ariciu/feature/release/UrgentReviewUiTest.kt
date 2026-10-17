package ro.andrei.ariciu.feature.release

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ro.andrei.ariciu.feature.delivery.model.*
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import ro.andrei.ariciu.feature.remindereditor.ui.ReminderEditorScreen
import ro.andrei.ariciu.feature.reminders.model.*
import ro.andrei.ariciu.feature.reminders.ui.*
import ro.andrei.ariciu.ui.theme.AriciuTheme
import java.io.File

class UrgentReviewUiTest {
    @get:Rule val compose = createComposeRule()
    private val place = SelectedLocation(45.0,25.0,200)
    private fun capture(name: String) {
        compose.waitForIdle(); Thread.sleep(300)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File(instrumentation.targetContext.filesDir,"urgent-review/$name.png").apply { parentFile!!.mkdirs() }
        instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle()
        }
    }
    @Test fun pendingRetryAndExplicitCardActionsAreClearInBothThemesAndLargeText() {
        val pending = Reminder("urgent","Pick up a parcel",place,ReminderTransition.ENTER,ReminderRepeat.EVERY_VISIT,
            delivery = DeliveryState(phase = DeliveryPhase.TRIGGERED,position = AreaPosition.INSIDE,occurrence = 4,urgentRetryAtMs = System.currentTimeMillis()+300_000),
            intensity = NotificationIntensity.URGENT,registration = RegistrationState.REGISTERED)
        val normal = Reminder("normal","Buy batteries",place,ReminderTransition.ENTER,ReminderRepeat.ONCE,registration = RegistrationState.REGISTERED)
        var rows by mutableStateOf(listOf(pending,normal)); var dark by mutableStateOf(true); var large by mutableStateOf(false)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,if (large) 2f else 1f)) {
                AriciuTheme(darkTheme = dark) {
                    Scaffold(contentWindowInsets = WindowInsets.safeDrawing,containerColor = MaterialTheme.colorScheme.background) { insets ->
                    Box(Modifier.padding(insets)) {
                    RemindersScreen(RemindersUiState(rows,loading = false),DeliveryCapabilities(true,true,true,true,true),null,{},{},{},{},
                        onDone = { chosen -> rows = rows.map { if (it.id == chosen.id) it.copy(delivery = it.delivery.completed()) else it } },
                        onNextTime = { chosen -> rows = rows.map { if (it.id == chosen.id) it.copy(delivery = it.delivery.onAction(OccurrenceAction.NEXT_TIME,chosen.delivery.occurrence,it.transition,allowUnresolvedUrgent = true)) else it } })
                    }
                    }
                }
            }
        }
        compose.onNodeWithText("Re-alert scheduled").assertIsDisplayed()
        compose.onNodeWithTag("next_time_urgent").performScrollTo().assertIsDisplayed(); capture("urgent-pending-dark")
        compose.runOnIdle { dark = false }; capture("urgent-pending-light")
        compose.runOnIdle { large = true }
        compose.onNodeWithTag("next_time_urgent").performScrollTo().assertIsDisplayed(); capture("urgent-pending-large")
        compose.onNodeWithTag("next_time_urgent").performClick()
        compose.onNodeWithTag("next_time_urgent").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(DeliveryPhase.WAITING_NEXT_VISIT,rows.first().delivery.phase)
            assertEquals(0L,rows.first().delivery.urgentRetryAtMs); assertEquals(normal,rows.last())
            rows = listOf(pending,normal)
        }
        compose.onNodeWithTag("done_urgent").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(normal,rows.single { it.delivery.active }) }
    }
    @Test fun legacyAlarmRemainsEditableButOnlyThreeChoicesAndExplicitUrgentConversionAreOffered() {
        var draft by mutableStateOf(ReminderDraft("Existing reminder",place,intensity = NotificationIntensity.ALARM))
        var dark by mutableStateOf(false); var large by mutableStateOf(false); var saved: ReminderDraft? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,if (large) 2f else 1f)) {
                AriciuTheme(darkTheme = dark) { ReminderEditorScreen(draft,{ draft = it },{},{ saved = draft },{},editing = true) }
            }
        }
        compose.onNodeWithText("Alarm is temporarily unavailable",substring = true).performScrollTo().assertIsDisplayed()
        capture("legacy-alarm-light")
        compose.onNodeWithText("Alert: Alarm").performScrollTo().performClick()
        compose.onNodeWithText("Alarm",substring = false).assertDoesNotExist()
        compose.onNodeWithText("Silent",substring = false).assertExists()
        compose.onNodeWithText("Normal",substring = false).assertExists()
        compose.onNodeWithText("Urgent",substring = false).performScrollTo().assertIsDisplayed(); capture("three-levels-light")
        compose.runOnIdle { dark = true; large = true }
        compose.onNodeWithText("Urgent",substring = false).performScrollTo().assertIsDisplayed(); capture("three-levels-dark")
        compose.onNodeWithText("Urgent",substring = false).performClick()
        compose.onNodeWithText("An ongoing alert.",substring = true).performScrollTo().assertIsDisplayed()
        capture("urgent-editor-large")
        compose.onNodeWithTag("reminder_save").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(NotificationIntensity.URGENT,saved?.intensity); assertFalse(saved!!.notifyOnSave) }
    }
}

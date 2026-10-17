package ro.andrei.ariciu.feature.reminders.ui

import androidx.compose.ui.test.*
import androidx.compose.runtime.*
import ro.andrei.ariciu.ui.theme.ThemePreference
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ro.andrei.ariciu.feature.reminders.model.*
import ro.andrei.ariciu.ui.theme.AriciuTheme

class AriciuAppTest {
    @get:Rule val compose = createComposeRule()

    @Test fun mapSelectionCreatesReminderThenCanBeEdited() {
        val repo = object : ReminderRepository {
            val rows = MutableStateFlow(emptyList<Reminder>())
            override fun observeReminders() = rows
            override suspend fun save(reminder: Reminder) {
                rows.value = rows.value.filterNot { it.id == reminder.id } + reminder
            }
            override suspend fun complete(id: String, occurrence: Int) {
                rows.value = rows.value.map { if (it.id == id && it.delivery.occurrence == occurrence)
                    it.copy(delivery = it.delivery.completed()) else it }
            }
            override suspend fun delete(id: String) { rows.value = rows.value.filterNot { it.id == id } }
        }
        lateinit var vm: RemindersViewModel
        compose.runOnUiThread { vm = RemindersViewModel(repo, SavedStateHandle()) }
        compose.setContent { AriciuTheme { AriciuApp(vm) } }
        compose.onNodeWithText("Ariciu").assertIsDisplayed()
        compose.onNodeWithTag("location_picker_confirm_button").performClick()
        compose.onNodeWithTag("reminder_editor_title_field").performTextInput("Buy bread")
        // Exercise the accessible choice action without racing Android's native keyboard resize.
        compose.onNodeWithText("Leave").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
        compose.onNodeWithText("Leave").assertIsSelected()
        compose.runOnIdle { assertEquals(ReminderTransition.EXIT, vm.draft.value.transition) }
        compose.onNodeWithTag("reminder_save").performClick()
        compose.onNodeWithText("Your reminders").assertIsDisplayed()
        compose.onNodeWithText("Buy bread").assertIsDisplayed()
        compose.onNodeWithText("Edit").performScrollTo().performClick()
        compose.onNodeWithText("Leave").assertIsSelected()
        compose.onNodeWithTag("reminder_editor_title_field").performTextReplacement("Buy milk")
        compose.runOnIdle { assertEquals(ReminderTransition.EXIT, vm.draft.value.transition) }
        compose.onNodeWithTag("reminder_save").performClick()
        compose.onNodeWithText("Buy milk").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(1, repo.rows.value.size)
            assertEquals(ReminderTransition.EXIT, repo.rows.value.single().transition)
        }
        compose.onNodeWithText("Mark done", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Completed · 1").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Buy milk").assertIsDisplayed()
        compose.onNodeWithTag("tab_completed").assertIsSelected()
    }

    @Test fun tabsPreserveDraftAndAppearanceSelectionIsExplicit() {
        val repository = object : ReminderRepository {
            override fun observeReminders() = MutableStateFlow(emptyList<Reminder>())
            override suspend fun save(reminder: Reminder) = Unit
            override suspend fun complete(id: String, occurrence: Int) = Unit
            override suspend fun delete(id: String) = Unit
        }
        lateinit var vm: RemindersViewModel
        compose.runOnUiThread { vm = RemindersViewModel(repository, SavedStateHandle()) }
        var appearance by mutableStateOf(ThemePreference.SYSTEM)
        compose.setContent {
            AriciuTheme(darkTheme = appearance.isDark(false)) { AriciuApp(vm, appearance, { appearance = it }) }
        }
        compose.onNodeWithTag("nav_Reminders").performClick()
        compose.onNodeWithText("Your reminders").assertIsDisplayed()
        compose.onNodeWithTag("nav_Settings").performClick()
        compose.onNodeWithTag("theme_DARK").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(ThemePreference.DARK, appearance) }
        compose.onNodeWithTag("theme_LIGHT").performClick().assertIsSelected()
        compose.onNodeWithTag("nav_Create").performClick()
        compose.onNodeWithTag("place_sheet_toggle").performClick()
        compose.onNodeWithText("500 m").performClick()
        compose.onNodeWithTag("location_picker_confirm_button").performClick()
        compose.onNodeWithTag("reminder_editor_title_field").performTextInput("Prescription")
        compose.onNodeWithText("Change place").performScrollTo().performClick()
        compose.onNodeWithTag("nav_Settings").performClick()
        compose.onNodeWithTag("nav_Create").performClick()
        compose.onNodeWithTag("reminder_editor_title_field").assertTextContains("Prescription")
        compose.runOnIdle { assertEquals(500, vm.draft.value.location?.radiusMeters) }
    }
}

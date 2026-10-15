package ro.andrei.ariciu.feature.reminders.ui

import androidx.compose.ui.test.*
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
            override suspend fun delete(id: String) { rows.value = rows.value.filterNot { it.id == id } }
        }
        lateinit var vm: RemindersViewModel
        compose.runOnUiThread { vm = RemindersViewModel(repo, SavedStateHandle()) }
        compose.setContent { AriciuTheme { AriciuApp(vm) } }
        compose.onNodeWithText("Ariciu").assertIsDisplayed()
        compose.onNodeWithTag("location_picker_confirm_button").performScrollTo().performClick()
        compose.onNodeWithTag("reminder_editor_title_field").performTextInput("Buy bread")
        compose.onNodeWithText("Leave").performScrollTo().performClick()
        compose.onNodeWithTag("reminder_save").performScrollTo().performClick()
        compose.onNodeWithText("Your reminders").assertIsDisplayed()
        compose.onNodeWithText("Buy bread").assertIsDisplayed()
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithTag("reminder_editor_title_field").performTextReplacement("Buy milk")
        compose.onNodeWithTag("reminder_save").performScrollTo().performClick()
        compose.onNodeWithText("Buy milk").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(1, repo.rows.value.size)
            assertEquals(ReminderTransition.EXIT, repo.rows.value.single().transition)
        }
    }
}

package ro.andrei.ariciu.feature.remindereditor.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import ro.andrei.ariciu.feature.reminders.model.*
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderEditorScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun reminderEditorScreen_allowsEnteringATitle() {
        composeTestRule.setContent {
            ReminderEditorScreen()
        }

        composeTestRule.onNodeWithText("Create reminder").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reminder_editor_title_field")
            .assertIsDisplayed()
            .assertTextEquals("Reminder title", "", includeEditableText = true)
            .performTextInput("Buy batteries")
        composeTestRule.onNodeWithTag("reminder_editor_title_field")
            .assertTextEquals("Reminder title", "Buy batteries", includeEditableText = true)
    }

    @Test
    fun saveRequiresTitleAndLocationAndKeepsTriggerChoice() {
        var saved: ReminderDraft? = null
        composeTestRule.setContent {
            var draft by remember { mutableStateOf(ReminderDraft(
                location = SelectedLocation(45.0, 25.0, 200))) }
            ReminderEditorScreen(draft, { draft = it }, {}, { saved = draft }, {})
        }
        composeTestRule.onNodeWithTag("reminder_save").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("reminder_editor_title_field").performTextInput("Buy bread")
        composeTestRule.onNodeWithText("Leave").performClick()
        composeTestRule.onNodeWithTag("reminder_repeat").performClick()
        composeTestRule.onNodeWithTag("reminder_save").performScrollTo().assertIsEnabled().performClick()
        composeTestRule.runOnIdle {
            assertEquals("Buy bread", saved?.title)
            assertEquals(ReminderTransition.EXIT, saved?.transition)
            assertEquals(ReminderRepeat.EVERY_VISIT, saved?.repeat)
        }
    }
}

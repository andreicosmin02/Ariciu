package ro.andrei.ariciu.feature.remindereditor.ui

import androidx.compose.ui.test.assertIsDisplayed
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
}

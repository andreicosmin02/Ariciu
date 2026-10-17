package ro.andrei.ariciu.feature.reminders.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ro.andrei.ariciu.feature.delivery.model.*
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import ro.andrei.ariciu.feature.reminders.model.*
import ro.andrei.ariciu.ui.theme.AriciuTheme

class ReminderTabsTest {
    @get:Rule val compose = createComposeRule()
    private fun reminder(id: String) = Reminder(id, "Task $id", SelectedLocation(45.0,25.0,200), ReminderTransition.ENTER, ReminderRepeat.ONCE)
    @Test fun traditionalTabsKeepSelectionCountsAndCompletionActionDistinctAtLargeText() {
        var rows by mutableStateOf((1..8).map { reminder("$it") } + reminder("finished").let { it.copy(delivery = it.delivery.completed()) })
        var opened by mutableStateOf<String?>(null)
        var doneId: String? = null
        compose.setContent { AriciuTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                RemindersScreen(RemindersUiState(rows, loading = false), DeliveryCapabilities(true,true,true,true,true), opened, {}, {}, {}, {}, {
                    doneId = it.id; rows = rows.map { row -> if (row.id == it.id) row.copy(delivery = row.delivery.completed()) else row }
                })
            }
        } }
        compose.onNodeWithTag("tab_active").assertIsSelected()
        compose.onNodeWithText("Active reminders").assertDoesNotExist()
        compose.onNodeWithText("Completed reminders").assertDoesNotExist()
        compose.onNodeWithTag("done_1").performScrollTo().assertTextEquals("Mark done").performClick()
        compose.runOnIdle { assertEquals("1", doneId) }
        compose.onNodeWithText("Completed · 2").performClick().assertIsSelected()
        compose.onNodeWithText("Task 1").assertIsDisplayed()
        compose.onNodeWithTag("done_1").assertDoesNotExist()
        compose.onNodeWithTag("tab_active").performClick()
        compose.onNodeWithTag("reminders_list").performScrollToNode(hasText("Task 8"))
        compose.onNodeWithTag("tab_active").assertIsDisplayed().assertIsSelected()
        compose.runOnIdle { opened = "finished" }
        compose.waitForIdle()
        compose.onNodeWithTag("tab_completed").assertIsSelected()
        compose.onNodeWithText("Task finished").assertIsDisplayed()
    }
}

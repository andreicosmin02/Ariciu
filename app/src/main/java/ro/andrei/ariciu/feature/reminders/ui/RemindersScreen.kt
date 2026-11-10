package ro.andrei.ariciu.feature.reminders.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ro.andrei.ariciu.feature.delivery.model.DeliveryCapabilities
import ro.andrei.ariciu.feature.reminders.model.*
import ro.andrei.ariciu.ui.components.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun RemindersScreen(state: RemindersUiState, capabilities: DeliveryCapabilities, openedId: String?,
    onCreate: () -> Unit, onSettings: () -> Unit, onEdit: (Reminder) -> Unit, onDelete: (Reminder) -> Unit, onDone: (Reminder) -> Unit = {}, onNextTime: (Reminder) -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    var completed by rememberSaveable { mutableStateOf(false) }
    val active = state.reminders.filter { it.delivery.active }
    val finished = state.reminders.filterNot { it.delivery.active }
    val reminders = if (completed) finished else active
    val listState = rememberLazyListState()
    LaunchedEffect(openedId, state.reminders) {
        val opened = state.reminders.firstOrNull { it.id == openedId }
        if (opened != null) {
            completed = !opened.delivery.active
            val matching = if (completed) finished else active
            val index = matching.indexOfFirst { it.id == openedId }
            if (index >= 0) listState.animateScrollToItem(index + (if (!capabilities.ready && active.isNotEmpty()) 1 else 0) + (if (state.loading) 1 else 0))
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 20.dp)) {
        ScreenHeading("Your reminders", "The things you want to remember.")
        Spacer(Modifier.height(8.dp))
        ReminderTabs(completed, active.size, finished.size, { completed = it })
        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("reminders_list"), state = listState,
            contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!capabilities.ready && active.isNotEmpty()) item {
            Surface(shape = MaterialTheme.shapes.medium, color = colors.tertiaryContainer) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Let’s get reminders working", style = MaterialTheme.typography.titleMedium, color = colors.onTertiaryContainer)
                    Text("A few phone settings need your attention.", style = MaterialTheme.typography.bodyMedium, color = colors.onTertiaryContainer)
                    TextButton(onClick = onSettings, contentPadding = PaddingValues(0.dp)) { Text("Review settings") }
                }
            }
        }
        if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        else if (reminders.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(AriciuIcons.Reminders, null, tint = colors.primary, modifier = Modifier.size(40.dp))
                Text(if (completed) "A clear slate" else "Remember it in the right place.", style = MaterialTheme.typography.headlineMedium)
                Text(if (completed) "Reminders you finish will be kept here." else "Pick a place, add a reminder, and let your phone do the remembering.",
                    color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                if (!completed) PrimaryAction("Create a reminder", onCreate)
            }
        }
        items(reminders, key = { it.id }) { reminder ->
            val status = reminder.presentStatus(capabilities)
            Surface(shape = MaterialTheme.shapes.medium, color = colors.surface,
                border = BorderStroke(1.dp, if (reminder.id == openedId) colors.primary else colors.outlineVariant)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (reminder.transition == ReminderTransition.ENTER) "ON ARRIVAL" else "ON DEPARTURE",
                            modifier = Modifier.padding(vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                        Surface(shape = MaterialTheme.shapes.small, color = if (status.needsAttention) colors.tertiaryContainer else colors.primaryContainer) {
                            Text(status.label, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium,
                                color = if (status.needsAttention) colors.onTertiaryContainer else colors.onPrimaryContainer)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(reminder.title, style = MaterialTheme.typography.titleLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp),
                            itemVerticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(AriciuIcons.Place, null, Modifier.size(16.dp), tint = colors.onSurfaceVariant)
                                Text("${reminder.location.radiusMeters} m · ${if (reminder.repeat == ReminderRepeat.ONCE) "Just once" else "Every visit"}",
                                    style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            }
                            Text(reminder.intensity.label, style = MaterialTheme.typography.labelMedium, color = colors.primary,
                                modifier = Modifier.semantics { contentDescription = "Alert: ${reminder.intensity.label}" })
                        }
                        reminder.chainName?.let { Text("$it · ${reminder.chainAreas.size} selected stores around your pin", style = MaterialTheme.typography.bodyMedium) }
                        Text(status.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (reminder.delivery.active) OutlinedButton(onClick = { onDone(reminder) }, enabled = !state.busy,
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.heightIn(min = 48.dp).testTag("done_${reminder.id}")) { Text("Mark done") }
                            if (reminder.unresolvedUrgent) OutlinedButton(onClick = { onNextTime(reminder) }, enabled = !state.busy,
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.heightIn(min = 48.dp).testTag("next_time_${reminder.id}")) { Text("Next time") }
                        }
                        Row {
                            TextButton(onClick = { onEdit(reminder) }, enabled = !state.busy, contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.heightIn(min = 48.dp)) { Text("Edit") }
                            TextButton(onClick = { onDelete(reminder) }, enabled = !state.busy, contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.heightIn(min = 48.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = colors.onSurfaceVariant)) { Text("Delete") }
                        }
                    }
                }
            }
        }
        }
    }
}

/** Plain text tabs: selection is bold + accent colour + a 2dp underline, never colour alone. */
@Composable
private fun ReminderTabs(completed: Boolean, active: Int, finished: Int, onSelect: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup().testTag("reminder_tabs")) {
        listOf(false to "Active · $active", true to "Completed · $finished").forEach { (value, label) ->
            val selected = completed == value
            Column(Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp)
                .selectable(selected, role = Role.Tab, onClick = { onSelect(value) })
                .semantics { stateDescription = if (selected) "Selected" else "Not selected" }
                .testTag(if (value) "tab_completed" else "tab_active")) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 2.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) colors.primary else colors.onSurfaceVariant)
                }
                Box(Modifier.fillMaxWidth().height(2.dp).background(if (selected) colors.primary else Color.Transparent))
            }
        }
    }
    HorizontalDivider(color = colors.outlineVariant)
}

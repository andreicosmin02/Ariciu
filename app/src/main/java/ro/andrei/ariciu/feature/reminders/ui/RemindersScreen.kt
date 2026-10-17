package ro.andrei.ariciu.feature.reminders.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ro.andrei.ariciu.feature.delivery.model.DeliveryCapabilities
import ro.andrei.ariciu.feature.reminders.model.*
import ro.andrei.ariciu.ui.components.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun RemindersScreen(state: RemindersUiState, capabilities: DeliveryCapabilities, openedId: String?,
    onCreate: () -> Unit, onSettings: () -> Unit, onEdit: (Reminder) -> Unit, onDelete: (Reminder) -> Unit, onDone: (Reminder) -> Unit = {}) {
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
            if (index >= 0) listState.animateScrollToItem(index + 3)
        }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("reminders_list"), state = listState,
        contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ScreenHeading("Your reminders", "The things you want to remember.") }
        stickyHeader {
            Surface(color = colors.background) {
            Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(false to "Active · ${active.size}", true to "Completed · ${finished.size}").forEach { (value, label) ->
                        FilterChip(completed == value, { completed = value }, label = { Text(label) },
                            leadingIcon = if (completed == value) ({ Icon(AriciuIcons.Check, null, Modifier.size(18.dp)) }) else null,
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.primaryContainer, selectedLabelColor = colors.onPrimaryContainer),
                            modifier = (if (LocalDensity.current.fontScale > 1.3f) Modifier.fillMaxWidth() else Modifier.weight(1f)).heightIn(min = 48.dp))
                    }
                }
                Text(if (completed) "Completed reminders" else "Active reminders", style = MaterialTheme.typography.titleSmall)
            }
            }
        }
        item {
            if (!capabilities.ready && active.isNotEmpty()) Surface(shape = MaterialTheme.shapes.medium, color = colors.tertiaryContainer) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Surface(shape = MaterialTheme.shapes.large, color = colors.surface,
                border = if (reminder.id == openedId) BorderStroke(1.dp, colors.primary) else null) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (reminder.transition == ReminderTransition.ENTER) "ON ARRIVAL" else "ON DEPARTURE",
                            modifier = Modifier.padding(vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                        Surface(shape = MaterialTheme.shapes.small, color = if (status.needsAttention) colors.tertiaryContainer else colors.primaryContainer) {
                            Text(status.label, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium,
                                color = if (status.needsAttention) colors.onTertiaryContainer else colors.onPrimaryContainer)
                        }
                    }
                    Text(reminder.title, style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(AriciuIcons.Place, null, Modifier.size(18.dp), tint = colors.onSurfaceVariant)
                        Text("${reminder.location.radiusMeters} m area · ${if (reminder.repeat == ReminderRepeat.ONCE) "Just once" else "Every visit"}",
                            style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                    Text("Alert: ${reminder.intensity.label}", style = MaterialTheme.typography.labelLarge, color = colors.primary)
                    reminder.chainName?.let { Text("$it · ${reminder.chainAreas.size} selected stores around your pin", style = MaterialTheme.typography.bodyMedium) }
                    Text(status.description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    HorizontalDivider(color = colors.outlineVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (reminder.delivery.active) FilledTonalButton(onClick = { onDone(reminder) }, enabled = !state.busy,
                            modifier = Modifier.testTag("done_${reminder.id}")) { Icon(AriciuIcons.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Done") }
                        TextButton(onClick = { onEdit(reminder) }, enabled = !state.busy, contentPadding = PaddingValues(0.dp)) { Text("Edit") }
                        TextButton(onClick = { onDelete(reminder) }, enabled = !state.busy, contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = colors.onSurfaceVariant)) { Text("Delete") }
                    }
                }
            }
        }
    }
}

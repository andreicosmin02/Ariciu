package ro.andrei.ariciu.feature.reminders.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
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
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 24.dp)) {
        ScreenHeading("Your reminders", "The things you want to remember.")
        Spacer(Modifier.height(16.dp))
        ReminderTabs(completed, active.size, finished.size, { completed = it })
        val panelBorder = colors.primary.copy(alpha = 0.55f)
        LazyColumn(Modifier.weight(1f).fillMaxWidth().background(colors.surface).drawBehind {
            val stroke = 1.dp.toPx()
            drawLine(panelBorder, Offset(stroke / 2, 0f), Offset(stroke / 2, size.height), stroke)
            drawLine(panelBorder, Offset(size.width - stroke / 2, 0f), Offset(size.width - stroke / 2, size.height), stroke)
            drawLine(panelBorder, Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
        }.testTag("reminders_list"), state = listState,
            contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        if (reminder.delivery.active) OutlinedButton(onClick = { onDone(reminder) }, enabled = !state.busy,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("done_${reminder.id}")) { Text("Mark done") }
                        if (reminder.unresolvedUrgent) OutlinedButton(onClick = { onNextTime(reminder) }, enabled = !state.busy,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("next_time_${reminder.id}")) { Text("Next time") }
                        TextButton(onClick = { onEdit(reminder) }, enabled = !state.busy, contentPadding = PaddingValues(0.dp)) { Text("Edit") }
                        TextButton(onClick = { onDelete(reminder) }, enabled = !state.busy, contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = colors.onSurfaceVariant)) { Text("Delete") }
                    }
                }
            }
        }
        }
    }
}

/** Fixed above the scrolling panel: tabs and content share a border without a chip gap. */
@Composable
private fun ReminderTabs(completed: Boolean, active: Int, finished: Int, onSelect: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val panelBorder = colors.primary.copy(alpha = 0.55f)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup().testTag("reminder_tabs")) {
        listOf(false to "Active · $active", true to "Completed · $finished").forEach { (value, label) ->
            val selected = completed == value
            Box(Modifier.weight(1f).fillMaxHeight().heightIn(min = 56.dp)
                .background(if (selected) colors.surface else colors.background, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .selectable(selected, role = Role.Tab, onClick = { onSelect(value) })
                .semantics { stateDescription = if (selected) "Selected" else "Not selected" }
                .testTag(if (value) "tab_completed" else "tab_active")
                .drawBehind {
                    val width = 1.dp.toPx(); val inset = width / 2; val radius = 12.dp.toPx()
                    val path = Path().apply {
                        moveTo(inset, size.height); lineTo(inset, radius)
                        quadraticTo(inset, inset, radius, inset); lineTo(size.width - radius, inset)
                        quadraticTo(size.width - inset, inset, size.width - inset, radius)
                        lineTo(size.width - inset, size.height)
                    }
                    drawPath(path, if (selected) panelBorder else colors.outlineVariant, style = Stroke(width))
                    if (!selected) drawLine(panelBorder, Offset(0f, size.height - inset), Offset(size.width, size.height - inset), width)
                    else drawLine(colors.primary, Offset(radius, 3.dp.toPx()), Offset(size.width - radius, 3.dp.toPx()), 2.dp.toPx())
                }.padding(horizontal = 12.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) colors.primary else colors.onSurfaceVariant)
            }
        }
    }
}

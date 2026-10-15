package ro.andrei.ariciu.feature.remindereditor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ro.andrei.ariciu.feature.reminders.model.*
import ro.andrei.ariciu.ui.components.*

@Composable
fun ReminderEditorScreen() {
    var title by rememberSaveable { mutableStateOf("") }
    var transition by rememberSaveable { mutableStateOf(ReminderTransition.ENTER) }
    var repeat by rememberSaveable { mutableStateOf(ReminderRepeat.ONCE) }
    ReminderEditorScreen(ReminderDraft(title = title, transition = transition, repeat = repeat),
        { title = it.title; transition = it.transition; repeat = it.repeat }, {}, {}, {})
}

@Composable
fun ReminderEditorScreen(
    draft: ReminderDraft,
    onDraftChange: (ReminderDraft) -> Unit,
    onChooseLocation: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    editing: Boolean = false,
    busy: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Scaffold(modifier = Modifier.imePadding(), contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(color = colors.surface) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryAction(if (busy) "Saving…" else "Save reminder", onSave, enabled = draft.canSave && !busy,
                        modifier = Modifier.testTag("reminder_save"))
                    Text("Only on this phone. No account needed.", style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            TextButton(onClick = onCancel, enabled = !busy, contentPadding = PaddingValues(0.dp)) {
                Icon(AriciuIcons.Back, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp)); Text("Cancel")
            }
            ScreenHeading(if (editing) "Edit reminder" else "What should you remember?",
                if (editing) "Make it right for your next visit." else "Add a note and choose when to hear it.")
            OutlinedTextField(value = draft.title, onValueChange = { onDraftChange(draft.copy(title = it)) },
                label = { Text("Reminder") }, placeholder = { Text("For example, pick up a prescription") },
                enabled = !busy, minLines = 1, maxLines = 3,
                textStyle = MaterialTheme.typography.titleLarge, shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().testTag("reminder_editor_title_field"))
            Surface(shape = MaterialTheme.shapes.medium, color = colors.surfaceContainer) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(AriciuIcons.Place, null, tint = colors.primary, modifier = Modifier.size(24.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (draft.location == null) "Choose a place" else "Your chosen place", style = MaterialTheme.typography.titleMedium)
                            Text(draft.location?.let { "Within ${it.radiusMeters} metres of your pin" } ?: "Set a spot on the map",
                                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                    }
                    TextButton(onClick = onChooseLocation, enabled = !busy, contentPadding = PaddingValues(horizontal = 0.dp)) {
                        Text(if (draft.location == null) "Choose place" else "Change place")
                        Spacer(Modifier.width(8.dp)); Icon(AriciuIcons.Arrow, null, Modifier.size(18.dp))
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel("When should we remind you?")
                val arrival: @Composable (Modifier) -> Unit = { modifier ->
                    ChoiceCard("Arrive", "When I get there", draft.transition == ReminderTransition.ENTER,
                        { onDraftChange(draft.copy(transition = ReminderTransition.ENTER)) }, modifier, !busy)
                }
                val departure: @Composable (Modifier) -> Unit = { modifier ->
                    ChoiceCard("Leave", "When I head out", draft.transition == ReminderTransition.EXIT,
                        { onDraftChange(draft.copy(transition = ReminderTransition.EXIT)) }, modifier, !busy)
                }
                if (LocalDensity.current.fontScale > 1.3f) Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    arrival(Modifier.fillMaxWidth()); departure(Modifier.fillMaxWidth())
                } else Row(Modifier.selectableGroup().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    arrival(Modifier.weight(1f).fillMaxHeight()); departure(Modifier.weight(1f).fillMaxHeight())
                }
            }
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel("How often?")
                ChoiceCard("Just once", "One reminder for this visit", draft.repeat == ReminderRepeat.ONCE,
                    { onDraftChange(draft.copy(repeat = ReminderRepeat.ONCE)) }, Modifier.fillMaxWidth(), !busy)
                ChoiceCard("Every visit", "Remind me again on future visits", draft.repeat == ReminderRepeat.EVERY_VISIT,
                    { onDraftChange(draft.copy(repeat = ReminderRepeat.EVERY_VISIT)) }, Modifier.fillMaxWidth().testTag("reminder_repeat"), !busy)
            }
            Text(if (editing) "Completed reminders stay completed when you edit them." else "Android may take a few minutes to notice an arrival or departure.",
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}

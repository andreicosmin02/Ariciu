package ro.andrei.ariciu.feature.remindereditor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ro.andrei.ariciu.feature.reminders.model.ReminderDraft
import ro.andrei.ariciu.feature.reminders.model.ReminderRepeat
import ro.andrei.ariciu.feature.reminders.model.ReminderTransition
import java.util.Locale

// Keep the original standalone editor available for previews and existing callers.
@Composable
fun ReminderEditorScreen() {
    var title by rememberSaveable { mutableStateOf("") }
    ReminderEditorScreen(ReminderDraft(title = title), { title = it.title }, {}, {}, {})
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
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(if (editing) "Edit reminder" else "Create reminder",
                style = MaterialTheme.typography.headlineMedium)
            Text("A little help, in the right place.", style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = draft.title,
                onValueChange = { onDraftChange(draft.copy(title = it)) },
                label = { Text("Reminder title") },
                singleLine = true,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().testTag("reminder_editor_title_field"),
            )
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Location", style = MaterialTheme.typography.titleMedium)
                    Text(draft.location?.let {
                        String.format(Locale.US, "%.5f, %.5f · %d m radius", it.latitude, it.longitude, it.radiusMeters)
                    } ?: "Choose a place on the map")
                    TextButton(onClick = onChooseLocation, enabled = !busy) {
                        Text(if (draft.location == null) "Choose location" else "Change location")
                    }
                }
            }
            Text("Remind me when I", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ReminderTransition.entries.forEach { transition ->
                    FilterChip(selected = draft.transition == transition,
                        onClick = { onDraftChange(draft.copy(transition = transition)) }, enabled = !busy,
                        label = { Text(if (transition == ReminderTransition.ENTER) "Arrive" else "Leave") })
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Repeat every visit", style = MaterialTheme.typography.titleMedium)
                    Text("Off means remind me once.", style = MaterialTheme.typography.bodyMedium)
                }
                Switch(checked = draft.repeat == ReminderRepeat.EVERY_VISIT,
                    onCheckedChange = { onDraftChange(draft.copy(repeat =
                        if (it) ReminderRepeat.EVERY_VISIT else ReminderRepeat.ONCE)) },
                    enabled = !busy, modifier = Modifier.testTag("reminder_repeat"))
            }
            Text("Saved on this device. Reminder delivery is not enabled yet.",
                style = MaterialTheme.typography.bodySmall)
            Button(onClick = onSave, enabled = draft.canSave && !busy,
                modifier = Modifier.fillMaxWidth().testTag("reminder_save")) {
                Text(if (busy) "Saving…" else "Save reminder")
            }
            TextButton(onClick = onCancel, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

package ro.andrei.ariciu.feature.reminders.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.andrei.ariciu.feature.locationpicker.ui.LocationPickerScreen
import ro.andrei.ariciu.feature.remindereditor.ui.ReminderEditorScreen
import ro.andrei.ariciu.feature.reminders.model.Reminder
import ro.andrei.ariciu.feature.reminders.model.ReminderRepeat
import ro.andrei.ariciu.feature.reminders.model.ReminderTransition

@Composable
fun AriciuApp(viewModel: RemindersViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val routeName by viewModel.route.collectAsStateWithLifecycle()
    val route = ReminderRoute.valueOf(routeName)
    var confirmDiscard by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<Reminder?>(null) }
    var pendingEdit by remember { mutableStateOf<Reminder?>(null) }
    val cancelEditor = {
        if (draft.title.isNotBlank() || viewModel.editing) confirmDiscard = true
        else viewModel.discard()
    }
    BackHandler(enabled = route != ReminderRoute.MAP || state.busy || draft.location != null) {
        if (!state.busy) {
            when (route) {
                ReminderRoute.EDITOR -> cancelEditor()
                ReminderRoute.MAP -> viewModel.navigate(ReminderRoute.EDITOR)
                ReminderRoute.LIST -> viewModel.navigate(ReminderRoute.MAP)
            }
        }
    }
    when (route) {
        ReminderRoute.MAP -> LocationPickerScreen(
            onLocationSelected = viewModel::chooseLocation,
            initialLocation = draft.location,
            onShowReminders = { viewModel.navigate(ReminderRoute.LIST) },
            onCancel = if (draft.location != null)
                ({ viewModel.navigate(ReminderRoute.EDITOR) }) else null,
        )
        ReminderRoute.EDITOR -> ReminderEditorScreen(
            draft, viewModel::updateDraft, { viewModel.navigate(ReminderRoute.MAP) },
            viewModel::save, cancelEditor, viewModel.editing, state.busy,
        )
        ReminderRoute.LIST -> Scaffold { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Your reminders", style = MaterialTheme.typography.headlineMedium)
                Text("Saved locally · Delivery not enabled yet", style = MaterialTheme.typography.bodyMedium)
                FilledTonalButton(onClick = { viewModel.navigate(ReminderRoute.MAP) }, enabled = !state.busy) {
                    Text("Back to map")
                }
                if (state.loading) CircularProgressIndicator()
                else if (state.reminders.isEmpty()) Text("Choose a place on the map to create your first reminder.")
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.reminders, key = { it.id }) { reminder ->
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(reminder.title, style = MaterialTheme.typography.titleLarge)
                                Text("${if (reminder.transition == ReminderTransition.ENTER) "On arrival" else "On departure"} · " +
                                    "${reminder.location.radiusMeters} m · " +
                                    if (reminder.repeat == ReminderRepeat.ONCE) "Once" else "Every visit")
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    TextButton(onClick = {
                                        if (draft.title.isNotBlank() || viewModel.editing) pendingEdit = reminder
                                        else viewModel.edit(reminder)
                                    }, enabled = !state.busy) { Text("Edit") }
                                    TextButton(onClick = { deleteCandidate = reminder }, enabled = !state.busy) { Text("Delete") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false },
        title = { Text("Discard changes?") }, text = { Text("Your unsaved changes will be lost.") },
        confirmButton = { TextButton(onClick = { confirmDiscard = false; viewModel.discard() }) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } })
    pendingEdit?.let { reminder ->
        AlertDialog(onDismissRequest = { pendingEdit = null }, title = { Text("Replace unsaved draft?") },
            text = { Text("Keep your current draft, or discard it to edit “${reminder.title}”.") },
            confirmButton = { TextButton(onClick = {
                pendingEdit = null
                viewModel.discard()
                viewModel.edit(reminder)
            }) { Text("Discard draft") } },
            dismissButton = { TextButton(onClick = { pendingEdit = null }) { Text("Keep draft") } })
    }
    deleteCandidate?.let { reminder ->
        AlertDialog(onDismissRequest = { deleteCandidate = null }, title = { Text("Delete reminder?") },
            text = { Text("Delete “${reminder.title}” from this device?") },
            confirmButton = { TextButton(onClick = { deleteCandidate = null; viewModel.delete(reminder) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Cancel") } })
    }
    state.error?.let { error ->
        AlertDialog(onDismissRequest = viewModel::clearError, title = { Text("Something went wrong") },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = viewModel::clearError) { Text("OK") } },
            dismissButton = { TextButton(onClick = { viewModel.clearError(); viewModel.reload() }) { Text("Reload reminders") } })
    }
}

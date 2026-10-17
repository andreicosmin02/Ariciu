package ro.andrei.ariciu.feature.reminders.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.andrei.ariciu.feature.locationpicker.ui.LocationPickerScreen
import ro.andrei.ariciu.feature.remindereditor.ui.ReminderEditorScreen
import ro.andrei.ariciu.feature.delivery.ui.DeliverySetupScreen
import ro.andrei.ariciu.feature.delivery.ui.rememberDeliveryCapabilities
import ro.andrei.ariciu.feature.reminders.model.Reminder
import ro.andrei.ariciu.ui.components.AriciuIcons
import ro.andrei.ariciu.ui.theme.ThemePreference
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity

@Composable
fun AriciuApp(viewModel: RemindersViewModel,
    themePreference: ThemePreference = ThemePreference.SYSTEM, onThemeChange: (ThemePreference) -> Unit = {}) {
    val capabilities = rememberDeliveryCapabilities()
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
                ReminderRoute.DELIVERY -> viewModel.navigate(ReminderRoute.MAP)
                ReminderRoute.LIST -> viewModel.navigate(ReminderRoute.MAP)
            }
        }
    }
    val stateHolder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val tabs = listOf(ReminderRoute.MAP to "Create", ReminderRoute.LIST to "Reminders", ReminderRoute.DELIVERY to "Settings")
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing, containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (route != ReminderRoute.EDITOR) {
                NavigationBar(modifier = Modifier.heightIn(min = if (LocalDensity.current.fontScale > 1.3f) 104.dp else 80.dp), containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
                    tabs.forEach { (destination, label) ->
                        NavigationBarItem(selected = route == destination, onClick = {
                            val target = if (destination == ReminderRoute.MAP && draft.title.isNotBlank()) ReminderRoute.EDITOR else destination
                            viewModel.navigate(target)
                        }, enabled = !state.busy,
                            icon = { Icon(when (destination) {
                                ReminderRoute.MAP -> AriciuIcons.Place
                                ReminderRoute.LIST -> AriciuIcons.Reminders
                                else -> AriciuIcons.Settings
                            }, null, Modifier.size(24.dp)) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) }, alwaysShowLabel = true,
                            modifier = Modifier.testTag("nav_$label"),
                            colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer))
                    }
                }
            }
        }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            when (route) {
                ReminderRoute.DELIVERY -> DeliverySetupScreen(capabilities, { viewModel.navigate(ReminderRoute.MAP) }, themePreference, onThemeChange)
                ReminderRoute.MAP -> stateHolder.SaveableStateProvider("map_${draft.location}") {
                    LocationPickerScreen(onLocationSelected = viewModel::chooseLocation, initialLocation = draft.location,
                        onCancel = if (draft.location != null) ({ viewModel.navigate(ReminderRoute.EDITOR) }) else null)
                }
                ReminderRoute.EDITOR -> ReminderEditorScreen(draft, viewModel::updateDraft,
                    { viewModel.navigate(ReminderRoute.MAP) }, viewModel::save, cancelEditor, viewModel.editing, state.busy)
                ReminderRoute.LIST -> RemindersScreen(state, capabilities, viewModel.openReminderId,
                    onCreate = { viewModel.navigate(if (draft.title.isNotBlank()) ReminderRoute.EDITOR else ReminderRoute.MAP) },
                    onSettings = { viewModel.navigate(ReminderRoute.DELIVERY) },
                    onEdit = { reminder ->
                        if (draft.title.isNotBlank() || viewModel.editing) pendingEdit = reminder else viewModel.edit(reminder)
                    }, onDelete = { deleteCandidate = it }, onDone = viewModel::complete)
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

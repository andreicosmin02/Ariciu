package ro.andrei.arichu.feature.remindereditor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import ro.andrei.arichu.feature.places.*
import ro.andrei.arichu.feature.delivery.model.ChainArea
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ro.andrei.arichu.feature.reminders.model.*
import ro.andrei.arichu.ui.components.*

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
    checkingArrival: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var refreshMessage by remember { mutableStateOf<String?>(null) }
    /** Re-run the stored search label around the pin. A refresh that finds nothing never replaces the current places. */
    fun refreshPlaces() {
        val location = draft.location ?: return
        val label = draft.chainName ?: return
        scope.launch {
            try {
                val index = LocalPlaces.index(context)
                val found = searchCatalog(index.near(location.latitude, location.longitude, SEARCH_RADIUS_METERS), label, emptySet(),
                    location.latitude, location.longitude, towns = emptyList())
                if (found.isEmpty()) refreshMessage = "No listed places found around this pin. Your current places are kept."
                else { refreshMessage = null; onDraftChange(draft.copy(chainAreas = found.map { ChainArea(it.id, it.latitude, it.longitude) })) }
            } catch (e: CancellationException) { throw e } catch (_: Exception) { refreshMessage = "Could not read the place catalogue. Your current places are kept." }
        }
    }
    val editorScroll = rememberScrollState()
    Scaffold(modifier = Modifier.imePadding(), contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(color = colors.surface) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (checkingArrival) Text("Your reminder is saved. Checking whether you’re already here…", style = MaterialTheme.typography.bodySmall)
                    PrimaryAction(if (checkingArrival) "Checking location…" else if (busy) "Saving…" else "Save reminder", onSave, enabled = draft.canSave && !busy,
                        modifier = Modifier.testTag("reminder_save"))
                    Text("Only on this phone. No account needed.", style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(editorScroll).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCancel, enabled = !busy, contentPadding = PaddingValues(0.dp)) {
                Icon(ArichuIcons.Back, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp)); Text("Cancel")
            }
            ScreenHeading(if (editing) "Edit reminder" else "New reminder",
                if (editing) "Make it right for your next visit." else "Add a note and choose when to hear it.")
            OutlinedTextField(value = draft.title, onValueChange = { onDraftChange(draft.copy(title = it)) },
                label = { Text("Reminder") }, placeholder = { Text("For example, pick up a prescription") },
                enabled = !busy, minLines = 1, maxLines = 3,
                textStyle = MaterialTheme.typography.bodyLarge, shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().testTag("reminder_editor_title_field"))
            Surface(shape = MaterialTheme.shapes.medium, color = colors.surfaceContainer) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(ArichuIcons.Place, null, tint = colors.primary, modifier = Modifier.size(24.dp))
                        Column(Modifier.weight(1f)) {
                            Text(draft.chainName?.let { "${draft.chainAreas.size} places · $it" } ?: if (draft.location == null) "Choose a place" else "Your chosen place", style = MaterialTheme.typography.titleMedium)
                            Text(draft.location?.let { if (draft.chainName == null) "Within ${it.radiusMeters} metres of your pin" else "Within ${it.radiusMeters} metres of each of these places" } ?: "Set a spot on the map",
                                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                    }
                    TextButton(onClick = onChooseLocation, enabled = !busy, contentPadding = PaddingValues(horizontal = 0.dp)) {
                        Text(if (draft.location == null) "Choose place" else "Change place")
                        Spacer(Modifier.width(8.dp)); Icon(ArichuIcons.Arrow, null, Modifier.size(18.dp))
                    }
                }
            }
            if (draft.chainName != null) {
                if (draft.chainAreas.isEmpty()) Text("No places are selected. Choose a place again.", color = colors.error)
                var chainHelp by rememberSaveable { mutableStateOf(false) }
                Text("${draft.chainAreas.size} places. Refresh within 25 km of your pin when travelling.", style = MaterialTheme.typography.bodyMedium)
                ExpandableSection("How multi-place reminders work", chainHelp, { chainHelp = !chainHelp }) { Text("This is an incomplete Romania catalogue. Done completes the whole reminder. Every visit waits until you leave all selected places. Each place uses one of Android’s 100 monitored areas.", style = MaterialTheme.typography.bodyMedium) }
                refreshMessage?.let { Text(it, color = colors.error, style = MaterialTheme.typography.bodySmall) }
                TextButton(onClick = { refreshPlaces() }, enabled = !busy && draft.location != null) { Text("Refresh places around this pin") }
                TextButton(onClick = { onDraftChange(draft.copy(chainName = null, chainAreas = emptyList())) }, enabled = !busy) { Text("Use only my map pin") }
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
            var choosingIntensity by remember { mutableStateOf(false) }
            OutlinedButton(onClick = { focus.clearFocus(); choosingIntensity = true }, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text("Alert: ${draft.intensity.label}")
            }
            if (!editing && draft.canCheckArrival) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                        value = draft.notifyOnSave, enabled = !busy, role = androidx.compose.ui.semantics.Role.Checkbox,
                        onValueChange = { onDraftChange(draft.copy(notifyOnSave = it)) }).testTag("notify_on_save"),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(draft.notifyOnSave,onCheckedChange = null,enabled = !busy)
                        Text("Notify me now if I’m already here",style = MaterialTheme.typography.bodyMedium)
                    }
                    if (draft.notifyOnSave) Text("On save, check your precise location once while Arichu is open. If you’re clearly inside, request your chosen alert. An uncertain or unavailable location won’t stop the reminder being saved.",
                        style = MaterialTheme.typography.bodySmall,color = colors.onSurfaceVariant)
                }
            }
            if (choosingIntensity) AlertDialog(onDismissRequest = { choosingIntensity = false },
                title = { Text("How should we alert you?") },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NotificationIntensity.available.forEach { intensity ->
                            ChoiceCard(intensity.label, intensity.explanation, draft.intensity == intensity,
                                { onDraftChange(draft.copy(intensity = intensity)); choosingIntensity = false }, Modifier.fillMaxWidth())
                        }
                    }
                }, confirmButton = { TextButton(onClick = { choosingIntensity = false }) { Text("Close") } })
            if (draft.intensity == NotificationIntensity.ALARM) Text(
                "Alarm is temporarily unavailable for new reminders. This existing reminder still works. Choose Urgent in Alert to switch; any current snooze is kept.",
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            if (draft.intensity == NotificationIntensity.URGENT)
                Text(draft.intensity.explanation + " Android notification settings can limit alerts; newer Android versions may allow dismissal.",
                    style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Text(if (editing) "Completed reminders stay completed when you edit them." else "Android may take a few minutes to notice an arrival or departure.",
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}

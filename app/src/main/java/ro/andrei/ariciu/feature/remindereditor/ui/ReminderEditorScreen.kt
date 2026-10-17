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
import androidx.compose.ui.platform.LocalContext
import ro.andrei.ariciu.feature.places.*
import ro.andrei.ariciu.feature.delivery.model.ChainArea
import kotlinx.coroutines.CancellationException
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
    val context = LocalContext.current
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    var catalog by remember { mutableStateOf<List<CatalogPlace>>(emptyList()) }
    var catalogError by remember { mutableStateOf(false) }
    var viewingStores by remember { mutableStateOf(false) }
    var choosingChain by remember { mutableStateOf(false) }
    LaunchedEffect(context) { try { catalog = LocalPlaces.load(context) } catch (e: CancellationException) { throw e } catch (_: Exception) { catalogError = true } }
    fun selectChain(chain: String?) {
        val location = draft.location ?: return
        val areas = if (chain == null) emptyList() else chainPlaces(catalog, chain, location.latitude, location.longitude).map { ChainArea(it.id, it.latitude, it.longitude) }
        onDraftChange(draft.copy(chainName = chain, chainAreas = areas))
    }
    LaunchedEffect(draft.chainName, draft.location, catalog) {
        if (draft.chainName != null && draft.chainAreas.isEmpty() && catalog.isNotEmpty()) selectChain(draft.chainName)
    }
    val editorScroll = rememberScrollState()
    if (viewingStores && draft.location != null) {
        StorePreviewScreen(draft.chainAreas, catalog, draft.location, { viewingStores = false })
        return
    }
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
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(editorScroll).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onCancel, enabled = !busy, contentPadding = PaddingValues(0.dp)) {
                Icon(AriciuIcons.Back, null, Modifier.size(20.dp))
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
                        Icon(AriciuIcons.Place, null, tint = colors.primary, modifier = Modifier.size(24.dp))
                        Column(Modifier.weight(1f)) {
                            Text(draft.chainName?.let { "Any selected $it store" } ?: if (draft.location == null) "Choose a place" else "Your chosen place", style = MaterialTheme.typography.titleMedium)
                            Text(draft.location?.let { if (draft.chainName == null) "Within ${it.radiusMeters} metres of your pin" else "Within ${it.radiusMeters} metres of each selected store" } ?: "Set a spot on the map",
                                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                    }
                    TextButton(onClick = onChooseLocation, enabled = !busy, contentPadding = PaddingValues(horizontal = 0.dp)) {
                        Text(if (draft.location == null) "Choose place" else "Change place")
                        Spacer(Modifier.width(8.dp)); Icon(AriciuIcons.Arrow, null, Modifier.size(18.dp))
                    }
                }
            }
            OutlinedButton(onClick = { focus.clearFocus(); choosingChain = true }, enabled = !busy && draft.location != null && catalog.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(draft.chainName?.let { "$it · ${draft.chainAreas.size} selected stores" } ?: "Use a supermarket chain instead") }
            if (catalogError) Text("Store catalogue unavailable. A single map pin still works.", color = colors.error)
            if (draft.chainName != null) {
                if (draft.chainAreas.isEmpty()) Text("No listed stores within 25 km. Change the pin, choose another chain, or use a single place.", color = colors.error)
                var chainHelp by rememberSaveable { mutableStateOf(false) }
                Text("${draft.chainAreas.size} selected stores. Refresh within 25 km of your pin when travelling.", style = MaterialTheme.typography.bodyMedium)
                ExpandableSection("How chain reminders work", chainHelp, { chainHelp = !chainHelp }) { Text("This is an incomplete Romania catalogue. Done completes the whole reminder. Every visit waits until you leave all selected store areas. Missing crossings can delay rearming. Store data updates with the app; refresh picks listed stores within 25 km from its current catalogue.", style = MaterialTheme.typography.bodySmall) }
                TextButton(onClick = { viewingStores = true; focus.clearFocus() }) { Text("View selected stores") }
                TextButton(onClick = { selectChain(draft.chainName) }, enabled = !busy && catalog.isNotEmpty()) { Text("Refresh stores around this pin") }
            }
            if (choosingChain) AlertDialog(onDismissRequest = { choosingChain = false }, title = { Text("Choose places") }, text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Up to 40 listed stores within 25 km of your pin. Romania · OpenStreetMap source: 15 Oct 2026. Coverage may be incomplete. Refresh when travelling.")
                    TextButton(onClick = { selectChain(null); choosingChain = false }) { Text("Only my map pin") }
                    catalog.map { it.chain }.filter { it.isNotBlank() }.distinct().sorted().forEach { chain ->
                        TextButton(onClick = { selectChain(chain); choosingChain = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(chain) }
                    }
                }
            }, confirmButton = { TextButton(onClick = { choosingChain = false }) { Text("Close") } })
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
            if (choosingIntensity) AlertDialog(onDismissRequest = { choosingIntensity = false },
                title = { Text("How should we alert you?") },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NotificationIntensity.entries.forEach { intensity ->
                            ChoiceCard(intensity.label, intensity.explanation, draft.intensity == intensity,
                                { onDraftChange(draft.copy(intensity = intensity)); choosingIntensity = false }, Modifier.fillMaxWidth())
                        }
                    }
                }, confirmButton = { TextButton(onClick = { choosingIntensity = false }) { Text("Close") } })
            if (draft.intensity == NotificationIntensity.ALARM || draft.intensity == NotificationIntensity.URGENT)
                Text(draft.intensity.explanation + " Android notification settings can limit alerts; newer Android versions may allow dismissal.",
                    style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Text(if (editing) "Completed reminders stay completed when you edit them." else "Android may take a few minutes to notice an arrival or departure.",
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}

package ro.andrei.ariciu.feature.delivery.ui

import android.Manifest
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.location.LocationManager
import androidx.core.content.ContextCompat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.common.GoogleApiAvailability
import ro.andrei.ariciu.feature.delivery.model.DeliveryCapabilities
import ro.andrei.ariciu.feature.delivery.platform.*
import ro.andrei.ariciu.ui.components.*
import ro.andrei.ariciu.ui.theme.ThemePreference
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag

@Composable
fun rememberDeliveryCapabilities(): DeliveryCapabilities {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var capabilities by remember { mutableStateOf(context.deliveryCapabilities()) }
    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                capabilities = context.deliveryCapabilities()
                DeliveryWork.reconcile(context, force = true)
            }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                capabilities = context.deliveryCapabilities()
                DeliveryWork.reconcile(context, force = true)
            }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(LocationManager.MODE_CHANGED_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED)
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); context.unregisterReceiver(receiver) }
    }
    return capabilities
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeliverySetupScreen(capabilities: DeliveryCapabilities, onBack: () -> Unit,
    themePreference: ThemePreference = ThemePreference.SYSTEM, onThemeChange: (ThemePreference) -> Unit = {}) {
    val context = LocalContext.current
    val offline = remember(context) { ro.andrei.ariciu.feature.offlinemap.OfflineMaps.get(context) }
    val offlineAreas by offline.areas.collectAsState()
    val offlineError by offline.error.collectAsState()
    ro.andrei.ariciu.feature.offlinemap.PauseOfflineMapsWhenBackgrounded(offline)
    var current by remember(capabilities) { mutableStateOf(capabilities) }
    var explanation by remember { mutableStateOf<String?>(null) }
    var requestedForeground by rememberSaveable { mutableStateOf(false) }
    var requestedNotifications by rememberSaveable { mutableStateOf(false) }
    val foreground = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        current = context.deliveryCapabilities()
        DeliveryWork.reconcile(context, force = true)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        current = context.deliveryCapabilities()
        DeliveryWork.reconcile(context, force = true)
    }
    fun appSettings() {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
    }
    val colors = MaterialTheme.colorScheme
    var showExplanation by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenHeading("Settings", "Appearance and reminder delivery.")
        SettingsPanel {
            SectionLabel("Appearance")
            if (LocalDensity.current.fontScale > 1.3f) Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemePreference.entries.forEach { preference ->
                    ChoiceCard(preference.label, selected = themePreference == preference,
                        onClick = { onThemeChange(preference) }, modifier = Modifier.fillMaxWidth().testTag("theme_${preference.name}"))
                }
            } else Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemePreference.entries.forEach { preference ->
                    FilterChip(selected = themePreference == preference, onClick = { onThemeChange(preference) },
                        leadingIcon = if (themePreference == preference) ({ Icon(AriciuIcons.Check, null, Modifier.size(16.dp)) }) else null,
                        label = { Text(preference.label) }, modifier = Modifier.weight(1f).heightIn(min = 52.dp).testTag("theme_${preference.name}"),
                        shape = MaterialTheme.shapes.small)
                }
            }
            Text("System follows your phone’s appearance.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        SettingsPanel {
            SectionLabel("Reminder delivery")
            Surface(color = colors.surfaceContainer, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (!current.ready) "A few things to set up" else if (current.blockedIntensities.isNotEmpty()) "Some alerts are turned off" else "Ready when you are", style = MaterialTheme.typography.titleLarge)
                    Text(if (current.ready) "Your phone can look out for your chosen places." else "Give Ariciu permission to remind you, even when the app is closed.",
                        style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            }
            Surface(color = colors.surface, shape = MaterialTheme.shapes.large) {
                Column {
                    SetupStep("Your location", current.preciseLocation, "Recognize the places you choose.") { explanation = "foreground" }
                    HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = colors.outlineVariant)
                    SetupStep("In the background", current.backgroundLocation,
                        if (current.preciseLocation) "Remind you while the app is closed." else "Allow your location first.", current.preciseLocation) { explanation = "background" }
                    HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = colors.outlineVariant)
                    SetupStep("Phone location", current.locationEnabled, "Keep location switched on.") { explanation = "location" }
                    HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = colors.outlineVariant)
                    SetupStep("Notifications", current.notifications, "Show your reminders and their actions.") { explanation = "notifications" }
                    HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = colors.outlineVariant)
                    SetupStep("Location service", current.playServices, "Uses Google Play services on your phone.") { explanation = "play" }
                }
            }
            if (current.blockedIntensities.isNotEmpty()) Text("Alerts turned off: ${current.blockedIntensities.joinToString { it.label }}. Review notification channels below.", color = colors.error)
            var showChannels by rememberSaveable { mutableStateOf(false) }
            ExpandableSection("Sound and alert settings", showChannels, { showChannels = !showChannels }) {
                ro.andrei.ariciu.feature.reminders.model.NotificationIntensity.available.forEach { intensity ->
                    TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .putExtra(Settings.EXTRA_CHANNEL_ID, AndroidReminderNotifications.channelId(intensity))) }, modifier = Modifier.fillMaxWidth()) {
                        Text("${intensity.label} alert settings", modifier = Modifier.weight(1f))
                    }
                }
            }
            Text("Retry monitoring after changing permissions or phone settings. This re-registers eligible reminder areas; it does not trigger an alert or replay a missed visit.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            OutlinedButton(onClick = { DeliveryWork.reconcile(context, force = true) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = MaterialTheme.shapes.medium) { Text("Check reminders again") }
        }
        SettingsPanel {
            SectionLabel("Offline maps")
            Text("Keeps roads, labels and map detail available without internet for about 4 × 4 km in the downloaded theme. It does not download extra searchable stores or change reminder coverage; search already works offline. Start in Create → Location details. Resume partial downloads here. Leaving Settings or Create pauses downloads.", style = MaterialTheme.typography.bodyMedium)
            if (offlineAreas.isEmpty()) Text("No downloaded areas yet.", color = colors.onSurfaceVariant)
            offlineError?.let { Text(it, color = colors.error) }
            offlineAreas.forEach { area ->
                Surface(color = colors.surfaceContainer, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(area.name, style = MaterialTheme.typography.titleMedium)
                        Text("${"%.1f".format(java.util.Locale.ROOT, area.bytes / 1_000_000.0)} MB · ${if (area.complete) "Ready offline" else if (area.active) "Downloading ${(area.progress * 100).toInt()}%" else "Incomplete · paused"}")
                        area.error?.let { Text(it, color = colors.error) }
                        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!area.complete) TextButton(onClick = { if (area.active) offline.pause(area.id) else offline.resume(area.id) }) { Text(if (area.active) "Pause" else "Resume") }
                            TextButton(onClick = { offline.delete(area.id) }) { Text("Delete area") }
                        }
                    }
                }
            }
            Text("Sizes show map resources; shared cache and database overhead are additional. Android app storage shows the total.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { appSettings() }) { Text("Phone storage settings") }
            Text("Place search: Romania catalogue © OpenStreetMap contributors, ODbL 1.0. Geofabrik source: 15 Oct 2026; catalogue rebuilt 17 Oct 2026. Shops and addresses may be missing or outdated. Searches stay on this phone.", style = MaterialTheme.typography.bodySmall)
        }
        SettingsPanel {
            ExpandableSection("How reminders work", showExplanation, { showExplanation = !showExplanation }) {
                Text("Your reminders stay on this phone. Android uses Google Play services to notice when you enter or leave an area. Alerts can take a few minutes, and battery restrictions or weak location signals can delay or prevent them. Location may need a network connection. Sound and pop-ups follow your phone’s notification settings.",
                    style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            Text("Private by design. No account. No subscription.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Text("Maps © OpenFreeMap · OpenMapTiles · OpenStreetMap contributors. Basemap design © CARTO, Stamen & Paul Norman.",
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
    explanation?.let { step ->
        val text = when (step) {
            "foreground" -> "Allow location while using Ariciu and enable Precise location. You can keep creating reminders if you decline. If Android no longer shows a prompt, open app settings and choose Permissions → Location."
            "background" -> "To detect arrivals and departures while Ariciu is closed, open Permissions → Location and choose “${if (Build.VERSION.SDK_INT >= 30) context.packageManager.backgroundPermissionOptionLabel else "Allow all the time"}”. Keep Precise location enabled. You can decline and still manage saved reminders."
            "notifications" -> "Allow notifications for reminder alerts. In Android Settings, enable Ariciu notifications and the channel for your chosen alert intensity. Android decides whether alerts pop up or make sound."
            "location" -> "Turn on Location in device settings. Wi-Fi scanning and network-assisted positioning can improve area detection."
            else -> "This feature requires Google Play services. Install, enable or update it on a supported Android device. Saved reminders remain available without it."
        }
        AlertDialog(onDismissRequest = { explanation = null }, title = { Text("Let Ariciu remind you") }, text = { Text(text) },
            confirmButton = { TextButton(onClick = {
                explanation = null
                when (step) {
                    "foreground" -> if (!current.preciseLocation && !requestedForeground) {
                        requestedForeground = true
                        foreground.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                    } else appSettings()
                    "background" -> if (!current.backgroundLocation && Build.VERSION.SDK_INT == 29) permission.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) else appSettings()
                    "notifications" -> if (Build.VERSION.SDK_INT >= 33 && !requestedNotifications &&
                        androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        requestedNotifications = true; permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    "location" -> context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    "play" -> {
                        val api = GoogleApiAvailability.getInstance()
                        val intent = api.getErrorResolutionIntent(context, api.isGooglePlayServicesAvailable(context), "ariciu")
                        if (intent != null) context.startActivity(intent) else appSettings()
                    }
                }
            }) { Text("Continue") } },
            dismissButton = { TextButton(onClick = { explanation = null }) { Text("Not now") } })
    }
}

@Composable
private fun SetupStep(title: String, ready: Boolean, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val action: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (ready) Icon(AriciuIcons.Check, null, tint = colors.primary,
                modifier = Modifier.size(20.dp).testTag("setup_status_$title"))
            TextButton(onClick = onClick, enabled = enabled,
                modifier = Modifier.testTag("setup_$title").heightIn(min = 48.dp).semantics {
                    contentDescription = "$title. ${if (ready) "Review" else "Set up"}"
                    stateDescription = if (ready) "Allowed" else "Needs setup"
                }) { Text(if (ready) "Review" else "Set up") }
        }
    }
    val descriptionContent: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
    if (LocalDensity.current.fontScale > 1.3f) Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        descriptionContent(Modifier.fillMaxWidth())
        Box(Modifier.align(Alignment.End)) { action() }
    } else Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        descriptionContent(Modifier.weight(1f)); action()
    }
}

@Composable
private fun SettingsPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

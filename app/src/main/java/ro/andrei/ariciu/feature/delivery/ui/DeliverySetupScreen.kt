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

@Composable
fun DeliverySetupScreen(capabilities: DeliveryCapabilities, onBack: () -> Unit,
    themePreference: ThemePreference = ThemePreference.SYSTEM, onThemeChange: (ThemePreference) -> Unit = {}) {
    val context = LocalContext.current
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
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        ScreenHeading("Settings", "Appearance and reminder delivery.")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("Reminder delivery")
            Surface(color = colors.surfaceContainer, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (current.ready) "Ready when you are" else "A few things to set up", style = MaterialTheme.typography.titleLarge)
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
            OutlinedButton(onClick = { DeliveryWork.reconcile(context, force = true) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = MaterialTheme.shapes.medium) { Text("Check reminders again") }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { showExplanation = !showExplanation }, contentPadding = PaddingValues(0.dp)) { Text("How reminders work") }
            if (showExplanation) Text("Your reminders stay on this phone. Android uses Google Play services to notice when you enter or leave an area. Alerts can take a few minutes, and battery restrictions or weak location signals can delay or prevent them. Location may need a network connection. Sound and pop-ups follow your phone’s notification settings.",
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Text("Private by design. No account. No subscription.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Text("Maps © OpenFreeMap · OpenMapTiles · OpenStreetMap contributors. Basemap design © CARTO, Stamen & Paul Norman.",
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
    explanation?.let { step ->
        val text = when (step) {
            "foreground" -> "Allow location while using Ariciu and enable Precise location. You can keep creating reminders if you decline. If Android no longer shows a prompt, open app settings and choose Permissions → Location."
            "background" -> "To detect arrivals and departures while Ariciu is closed, open Permissions → Location and choose “${if (Build.VERSION.SDK_INT >= 30) context.packageManager.backgroundPermissionOptionLabel else "Allow all the time"}”. Keep Precise location enabled. You can decline and still manage saved reminders."
            "notifications" -> "Allow notifications for reminder alerts. In Android Settings, enable Ariciu notifications and the Normal reminders channel. Android decides whether alerts pop up or make sound."
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
                    } else context.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).putExtra(Settings.EXTRA_CHANNEL_ID, REMINDER_CHANNEL))
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
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            if (ready) Icon(AriciuIcons.Check, contentDescription = "Allowed", tint = colors.primary, modifier = Modifier.size(22.dp))
        }
        TextButton(onClick = onClick, enabled = enabled, contentPadding = PaddingValues(0.dp),
            modifier = Modifier.testTag("setup_$title").heightIn(min = 48.dp)) {
            Text(if (ready) "Review" else "Set up")
        }
    }
}

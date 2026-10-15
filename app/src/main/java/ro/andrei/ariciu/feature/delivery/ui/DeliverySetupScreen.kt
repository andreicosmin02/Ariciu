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
fun DeliverySetupScreen(capabilities: DeliveryCapabilities, onBack: () -> Unit) {
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
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Reminder delivery", style = MaterialTheme.typography.headlineMedium)
            Text(if (current.ready) "Ready to monitor your active reminders" else "Setup needs attention",
                style = MaterialTheme.typography.titleLarge)
            Text("Ariciu stores reminders on this phone. Google Play services detects area crossings using the device’s location services; no Ariciu account or server is used.")
            SetupStep("Precise location", current.preciseLocation,
                "Needed to recognize your reminder areas.") { explanation = "foreground" }
            SetupStep("Background location", current.backgroundLocation,
                "Allows arrivals and departures while Ariciu is closed.", current.preciseLocation) { explanation = "background" }
            SetupStep("Device location", current.locationEnabled,
                "Device location must be switched on.") { explanation = "location" }
            SetupStep("Notifications", current.notifications,
                "Allows reminder alerts and their Done / Next time actions.") { explanation = "notifications" }
            SetupStep("Google Play services", current.playServices,
                "Required for low-power area monitoring.") { explanation = "play" }
            Text("Alerts can be delayed or missed by Android, battery restrictions, weak positioning, or unavailable network-assisted location. This is not a continuous GPS tracker. Sound and pop-ups follow your notification channel settings.",
                style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { DeliveryWork.reconcile(context, force = true) }) { Text("Retry monitoring") }
            TextButton(onClick = onBack) { Text("Back to map") }
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
        AlertDialog(onDismissRequest = { explanation = null }, title = { Text("Enable reminder delivery") }, text = { Text(text) },
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
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$title · ${if (ready) "Enabled" else "Needed"}", style = MaterialTheme.typography.titleMedium)
            Text(description)
            // Enabled channels can still be configured by the user.
            TextButton(onClick = onClick, enabled = enabled) { Text(if (ready) "Settings" else "Set up") }
        }
    }
}

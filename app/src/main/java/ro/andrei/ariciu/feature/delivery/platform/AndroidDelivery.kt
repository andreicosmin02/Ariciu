package ro.andrei.ariciu.feature.delivery.platform

import android.Manifest
import android.app.Notification
import android.media.AudioAttributes
import android.media.RingtoneManager
import ro.andrei.ariciu.feature.reminders.model.NotificationIntensity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import ro.andrei.ariciu.MainActivity
import ro.andrei.ariciu.R
import ro.andrei.ariciu.feature.delivery.model.*
import ro.andrei.ariciu.feature.reminders.model.ReminderTransition
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

const val REMINDER_CHANNEL = "normal_reminders"
const val EXTRA_REMINDER_ID = "reminder_id"

fun Context.deliveryCapabilities(): DeliveryCapabilities {
    fun granted(permission: String) = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    val location = getSystemService(LocationManager::class.java)
    val locationEnabled = if (Build.VERSION.SDK_INT >= 28) location.isLocationEnabled else
        location.isProviderEnabled(LocationManager.GPS_PROVIDER) || location.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    val manager = getSystemService(NotificationManager::class.java)
    return DeliveryCapabilities(
        preciseLocation = granted(Manifest.permission.ACCESS_FINE_LOCATION),
        backgroundLocation = Build.VERSION.SDK_INT < 29 || granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
        locationEnabled = locationEnabled,
        playServices = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS,
        notifications = (Build.VERSION.SDK_INT < 33 || granted(Manifest.permission.POST_NOTIFICATIONS)) &&
            NotificationManagerCompat.from(this).areNotificationsEnabled(),
        blockedIntensities = NotificationIntensity.entries.filter { manager.getNotificationChannel(AndroidReminderNotifications.channelId(it))?.importance == NotificationManager.IMPORTANCE_NONE }.toSet(),
    )
}

class PlayServicesGeofenceBackend(private val context: Context) : GeofenceBackend {
    private val client = LocationServices.getGeofencingClient(context)
    private val pendingIntent: PendingIntent
        get() = PendingIntent.getBroadcast(context, 0,
            Intent(context, GeofenceReceiver::class.java).setAction("ro.andrei.ariciu.GEOFENCE"),
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)

    @Suppress("MissingPermission")
    override suspend fun add(records: List<DeliveryRecord>) {
        check(context.deliveryCapabilities().ready) { "Delivery permissions unavailable" }
        val fences = records.map { record ->
            val location = record.location
            Geofence.Builder().setRequestId(record.requestId)
                .setCircularRegion(location.latitude, location.longitude, location.radiusMeters.toFloat())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .setNotificationResponsiveness(60_000).build()
        }
        // Registration/recovery is not itself a new visit, including when already inside.
        client.addGeofences(GeofencingRequest.Builder().setInitialTrigger(0).addGeofences(fences).build(), pendingIntent).awaitResult()
    }

    override suspend fun remove(requestIds: Set<String>) {
        client.removeGeofences(requestIds.toList()).awaitResult()
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}

/** Stop current platform alerting immediately; persisted WorkManager processing follows.
 * Compare both tokens before touching a notification, so an old button cannot mute a newer alert.
 */
fun Context.quietNotificationAction(requestId: String, occurrence: Int) {
    val manager = getSystemService(NotificationManager::class.java)
    val matching = manager.activeNotifications.firstOrNull { active ->
        active.tag?.startsWith("reminder:") == true &&
            "${active.tag.removePrefix("reminder:")}:${active.notification.extras.getString("ariciu_generation")}" == requestId &&
            active.notification.extras.getInt("ariciu_number", -1) == occurrence
    } ?: return
    val quiet = matching.notification.clone().apply {
        flags = (flags and Notification.FLAG_INSISTENT.inv()) or Notification.FLAG_ONLY_ALERT_ONCE
    }
    // Updating with ONLY_ALERT_ONCE stops an existing sound/vibration without discarding its actions.
    try { manager.notify(matching.tag, matching.id, quiet) } catch (e: SecurityException) {
        android.util.Log.w("AriciuDelivery", "Could not silence notification action", e)
    }
}

class AndroidReminderNotifications(private val context: Context) : ReminderNotifications {
    private val manager = context.getSystemService(NotificationManager::class.java)
    init {
        NotificationIntensity.entries.forEach { intensity ->
            manager.createNotificationChannel(NotificationChannel(channelId(intensity), "${intensity.label} reminders",
                if (intensity == NotificationIntensity.SILENT) NotificationManager.IMPORTANCE_LOW else NotificationManager.IMPORTANCE_HIGH).apply {
                description = intensity.explanation
                if (intensity == NotificationIntensity.SILENT) { setSound(null, null); enableVibration(false) }
                if (intensity == NotificationIntensity.ALARM) {
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    enableVibration(true); vibrationPattern = longArrayOf(0, 700, 250, 700, 250, 1000)
                }
            })
        }
    }

    companion object {
        fun channelId(intensity: NotificationIntensity) = if (intensity == NotificationIntensity.NORMAL) REMINDER_CHANNEL
            else "reminders_${intensity.name.lowercase()}_v1"
    }

    override fun requiresRefresh(record: DeliveryRecord): Boolean = record.reminder.intensity == NotificationIntensity.URGENT &&
        manager.activeNotifications.firstOrNull { it.tag == "reminder:${record.reminder.id}" }?.notification?.extras
            ?.getBoolean("ariciu_urgent_swipe_retry",false) == false

    private fun action(record: DeliveryRecord, action: OccurrenceAction): PendingIntent {
        val attempt = record.reminder.delivery.notificationAttempt
        // Preserve existing explicit-action identities. Only swipes address an individual alert attempt.
        val suffix = if (action == OccurrenceAction.DISMISS) "/$attempt" else ""
        val intent = Intent(context, NotificationActionReceiver::class.java)
            .setAction(action.name)
            .setData(Uri.parse("ariciu://occurrence/${Uri.encode(record.requestId)}/${record.reminder.delivery.occurrence}/${action.name}$suffix"))
            .putExtra("request_id", record.requestId).putExtra("occurrence", record.reminder.delivery.occurrence)
            .putExtra("notification_attempt",attempt)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    @Suppress("MissingPermission")
    override fun post(record: DeliveryRecord): Boolean {
        if (!context.deliveryCapabilities().notifications) return false
        val r = record.reminder
        if (manager.getNotificationChannel(channelId(r.intensity))?.importance == NotificationManager.IMPORTANCE_NONE) return false
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java)
            .setData(Uri.parse("ariciu://reminder/${Uri.encode(r.id)}"))
            .putExtra(EXTRA_REMINDER_ID, r.id)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val next = action(record, OccurrenceAction.NEXT_TIME)
        val existing = manager.activeNotifications.firstOrNull { it.tag == "reminder:${r.id}" }?.notification
        val sameOccurrence = existing?.extras?.getInt("ariciu_number", -1) == r.delivery.occurrence &&
            existing.extras.getInt("ariciu_attempt",0) == r.delivery.notificationAttempt && existing.channelId == channelId(r.intensity)
        if (existing != null && existing.channelId != channelId(r.intensity)) manager.cancel("reminder:${r.id}", 1)
        val notification = NotificationCompat.Builder(context, channelId(r.intensity))
            .setSmallIcon(R.drawable.ic_reminder_notification)
            .setContentTitle(r.title)
            .setContentText(if (r.intensity == NotificationIntensity.URGENT && r.delivery.notificationAttempt > 0) "Still pending. Choose Done or Next time." else if (r.chainName != null) "${if (r.transition == ReminderTransition.ENTER) "Near" else "Left"} a selected ${r.chainName} store." else if (r.transition == ReminderTransition.ENTER) "You arrived at your chosen place." else "You left your chosen place.")
            .setContentIntent(open).setAutoCancel(false).setDeleteIntent(if (r.intensity == NotificationIntensity.URGENT) action(record,OccurrenceAction.DISMISS) else next)
            .setOngoing(r.intensity in setOf(NotificationIntensity.URGENT, NotificationIntensity.ALARM))
            .setSilent(r.intensity == NotificationIntensity.SILENT || r.delivery.quiet || sameOccurrence)
            .setCategory(if (r.intensity == NotificationIntensity.ALARM) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH).setOnlyAlertOnce(sameOccurrence)
            .setExtras(Bundle().apply { putString("ariciu_generation", record.generation); putString("ariciu_occurrence", "${record.generation}:${r.delivery.occurrence}"); putInt("ariciu_number", r.delivery.occurrence); putInt("ariciu_attempt",r.delivery.notificationAttempt); putBoolean("ariciu_urgent_swipe_retry",r.intensity == NotificationIntensity.URGENT) })
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .apply {
                if (r.intensity == NotificationIntensity.ALARM && !r.delivery.quiet)
                    addAction(0, "Stop", action(record, OccurrenceAction.STOP))
                addAction(0, "Done", action(record, OccurrenceAction.DONE))
                if (r.intensity == NotificationIntensity.ALARM) addAction(0, "Snooze 10 min", action(record, OccurrenceAction.SNOOZE))
                else addAction(0, "Next time", next)
            }.build().apply {
                if (r.intensity == NotificationIntensity.ALARM && !r.delivery.quiet && !sameOccurrence)
                    flags = flags or Notification.FLAG_INSISTENT
            }
        return try { manager.notify("reminder:${r.id}", 1, notification); true }
        catch (_: SecurityException) { false }
    }

    override fun cancelExcept(reminderIds: Set<String>) {
        manager.activeNotifications.filter { it.tag?.startsWith("reminder:") == true &&
            it.tag.removePrefix("reminder:") !in reminderIds }.forEach { manager.cancel(it.tag, it.id) }
    }
}

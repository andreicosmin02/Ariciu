package ro.andrei.ariciu.feature.delivery.platform

import android.Manifest
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
    val manager = getSystemService(NotificationManager::class.java)
    val channel = manager.getNotificationChannel(REMINDER_CHANNEL)
    val location = getSystemService(LocationManager::class.java)
    val locationEnabled = if (Build.VERSION.SDK_INT >= 28) location.isLocationEnabled else
        location.isProviderEnabled(LocationManager.GPS_PROVIDER) || location.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    return DeliveryCapabilities(
        preciseLocation = granted(Manifest.permission.ACCESS_FINE_LOCATION),
        backgroundLocation = Build.VERSION.SDK_INT < 29 || granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
        locationEnabled = locationEnabled,
        playServices = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS,
        notifications = (Build.VERSION.SDK_INT < 33 || granted(Manifest.permission.POST_NOTIFICATIONS)) &&
            NotificationManagerCompat.from(this).areNotificationsEnabled() && channel?.importance != NotificationManager.IMPORTANCE_NONE,
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
            val location = record.reminder.location
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

class AndroidReminderNotifications(private val context: Context) : ReminderNotifications {
    private val manager = context.getSystemService(NotificationManager::class.java)
    init {
        manager.createNotificationChannel(NotificationChannel(REMINDER_CHANNEL, "Normal reminders", NotificationManager.IMPORTANCE_HIGH)
            .apply { description = "Arrival and departure reminders. Sound and pop-ups follow your Android settings." })
    }

    private fun action(record: DeliveryRecord, action: OccurrenceAction): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java)
            .setAction(action.name)
            .setData(Uri.parse("ariciu://occurrence/${Uri.encode(record.requestId)}/${record.reminder.delivery.occurrence}/${action.name}"))
            .putExtra("request_id", record.requestId).putExtra("occurrence", record.reminder.delivery.occurrence)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    @Suppress("MissingPermission")
    override fun post(record: DeliveryRecord): Boolean {
        if (!context.deliveryCapabilities().notifications) return false
        val r = record.reminder
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java)
            .setData(Uri.parse("ariciu://reminder/${Uri.encode(r.id)}"))
            .putExtra(EXTRA_REMINDER_ID, r.id)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val next = action(record, OccurrenceAction.NEXT_TIME)
        val sameOccurrence = manager.activeNotifications.firstOrNull { it.tag == "reminder:${r.id}" }
            ?.notification?.extras?.getInt("ariciu_number", -1) == r.delivery.occurrence
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL)
            .setSmallIcon(R.drawable.ic_reminder_notification)
            .setContentTitle(r.title)
            .setContentText(if (r.transition == ReminderTransition.ENTER) "You arrived at your chosen place." else "You left your chosen place.")
            .setContentIntent(open).setAutoCancel(false).setDeleteIntent(next)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH).setOnlyAlertOnce(sameOccurrence)
            .setExtras(Bundle().apply { putString("ariciu_occurrence", "${record.generation}:${r.delivery.occurrence}"); putInt("ariciu_number", r.delivery.occurrence) })
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .addAction(0, "Done", action(record, OccurrenceAction.DONE))
            .addAction(0, "Next time", next).build()
        return try { manager.notify("reminder:${r.id}", 1, notification); true }
        catch (_: SecurityException) { false }
    }

    override fun cancelExcept(reminderIds: Set<String>) {
        manager.activeNotifications.filter { it.tag?.startsWith("reminder:") == true &&
            it.tag.removePrefix("reminder:") !in reminderIds }.forEach { manager.cancel(it.tag, it.id) }
    }
}

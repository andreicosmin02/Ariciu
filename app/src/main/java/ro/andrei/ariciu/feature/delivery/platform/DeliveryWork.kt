package ro.andrei.ariciu.feature.delivery.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.work.*
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import ro.andrei.ariciu.feature.delivery.model.*
import ro.andrei.ariciu.feature.reminders.data.ReminderDatabase
import ro.andrei.ariciu.feature.reminders.data.RoomDeliveryStore
import ro.andrei.ariciu.feature.reminders.model.ReminderTransition

object DeliveryWork {
    internal val runtimeMutex = Mutex()

    /** Persist the occurrence before scheduling normal outbox dispatch. Never called by reconciliation. */
    suspend fun immediateArrival(context: Context, requestId: String, fix: ArrivalLocationFix,
        foreground: () -> Boolean): Boolean {
        val recorded = runtimeMutex.withLock {
            val capabilities = context.deliveryCapabilities()
            if (!foreground() || !capabilities.preciseLocation || !capabilities.locationEnabled ||
                !capabilities.playServices || !capabilities.notifications) return@withLock false
            val store = RoomDeliveryStore(ReminderDatabase.getInstance(context))
            val reminder = store.all().firstOrNull { it.requestId == requestId }?.reminder ?: return@withLock false
            if (reminder.intensity in capabilities.blockedIntensities) return@withLock false
            DeliveryEngine(store,PlayServicesGeofenceBackend(context),AndroidReminderNotifications(context)).immediateArrival(
                requestId,fix,SystemClock.elapsedRealtime(),Settings.Global.getInt(context.contentResolver,Settings.Global.BOOT_COUNT,0))
        }
        if (recorded) {
            val operation = command(context,workDataOf("command" to "dispatch"))
            kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
                operation.result.addListener({
                    try { operation.result.get(); if (continuation.isActive) continuation.resumeWith(kotlin.Result.success(Unit)) }
                    catch (e: Exception) { if (continuation.isActive) continuation.resumeWith(kotlin.Result.failure(e)) }
                },java.util.concurrent.Executor { it.run() })
            }
        }
        return recorded
    }

    private fun request(data: Data) = OneTimeWorkRequestBuilder<DeliveryWorker>()
        .setInputData(data).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()

    fun reconcile(context: Context, force: Boolean = false, restoreNotifications: Boolean = false) = WorkManager.getInstance(context).enqueueUniqueWork(
        "delivery-reconcile", ExistingWorkPolicy.APPEND_OR_REPLACE,
        request(workDataOf("command" to "reconcile", "force" to force, "restore_notifications" to restoreNotifications)))

    fun snoozeWake(context: Context, requestId: String, occurrence: Int, dueMs: Long) = WorkManager.getInstance(context).enqueueUniqueWork(
        "snooze:$requestId:$occurrence", ExistingWorkPolicy.KEEP,
        OneTimeWorkRequestBuilder<DeliveryWorker>().setInputData(workDataOf("command" to "snooze"))
            .setInitialDelay((dueMs - System.currentTimeMillis()).coerceAtLeast(0), TimeUnit.MILLISECONDS).build())

    fun command(context: Context, data: Data) = WorkManager.getInstance(context).enqueueUniqueWork(
        "delivery-events", ExistingWorkPolicy.APPEND_OR_REPLACE, request(data))
}

class DeliveryWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val command = inputData.getString("command") ?: "reconcile"
        return try {
            DeliveryWork.runtimeMutex.withLock {
                val store = RoomDeliveryStore(ReminderDatabase.getInstance(applicationContext))
                val engine = DeliveryEngine(store, PlayServicesGeofenceBackend(applicationContext), AndroidReminderNotifications(applicationContext))
                when (command) {
                    "transition" -> engine.transition(inputData.getStringArray("ids")?.toSet() ?: emptySet(),
                        ReminderTransition.valueOf(requireNotNull(inputData.getString("transition"))),
                        inputData.getLong("elapsed", -1), inputData.getInt("boot", -1))
                    "action" -> engine.action(requireNotNull(inputData.getString("request_id")),
                        inputData.getInt("occurrence", -1), OccurrenceAction.valueOf(requireNotNull(inputData.getString("action"))), System.currentTimeMillis())
                    "unavailable" -> engine.invalidateRegistrations("Location monitoring became unavailable. Check location settings, then Retry.")
                }
                if (command == "snooze" && store.all().any { it.reminder.delivery.snoozeUntilMs > System.currentTimeMillis() } &&
                    store.all().none { it.reminder.delivery.snoozeUntilMs in 1..System.currentTimeMillis() }) return Result.retry()
                engine.wakeSnoozed(System.currentTimeMillis())
                store.all().filter { it.reminder.delivery.snoozeUntilMs > 0 }.forEach {
                    DeliveryWork.snoozeWake(applicationContext, it.requestId, it.reminder.delivery.occurrence, it.reminder.delivery.snoozeUntilMs)
                }
                val capabilities = applicationContext.deliveryCapabilities()
                // Actions/notifications never wait behind a long backoff for geofence registration.
                engine.dispatchNotifications(capabilities.notifications,
                    restoreVisible = command == "reconcile" && inputData.getBoolean("restore_notifications", false))
                if (command == "reconcile") {
                    try {
                        withTimeout(20_000) { engine.reconcile(capabilities, inputData.getBoolean("force", false)) }
                    } catch (e: TimeoutCancellationException) {
                        engine.invalidateRegistrations("Monitoring timed out. Check Play services and location, then Retry.")
                        throw e
                    }
                } else DeliveryWork.reconcile(applicationContext)
            }
            Result.success()
        } catch (e: TimeoutCancellationException) {
            if (runAttemptCount < 4) Result.retry() else Result.success()
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            Log.w("AriciuDelivery", "Delivery work failed ($command), attempt $runAttemptCount", e)
            // Event/action mutations are idempotent and retryable. Bound registration retries; UI/boot/resume can retry again.
            if (command != "reconcile" || runAttemptCount < 4) Result.retry() else Result.success()
        }
    }

}

private fun BroadcastReceiver.finishAfter(operation: Operation, pending: BroadcastReceiver.PendingResult) {
    operation.result.addListener({
        try { operation.result.get() } catch (e: Exception) { Log.e("AriciuDelivery", "Could not persist delivery work", e) }
        finally { pending.finish() }
    }, java.util.concurrent.Executor { it.run() })
}

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        val data = if (event.hasError()) workDataOf("command" to "unavailable") else {
            val transition = when (event.geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> ReminderTransition.ENTER
                Geofence.GEOFENCE_TRANSITION_EXIT -> ReminderTransition.EXIT
                else -> return
            }
            workDataOf("command" to "transition", "transition" to transition.name,
                "ids" to event.triggeringGeofences.orEmpty().map { it.requestId }.toTypedArray(),
                "elapsed" to (event.triggeringLocation?.elapsedRealtimeNanos?.div(1_000_000) ?: SystemClock.elapsedRealtime()),
                "boot" to Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0))
        }
        val pending = goAsync()
        try { finishAfter(DeliveryWork.command(context, data), pending) }
        catch (e: Exception) { Log.e("AriciuDelivery", "Could not enqueue geofence event", e); pending.finish() }
    }
}

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action?.let { name -> OccurrenceAction.entries.firstOrNull { it.name == name } } ?: return
        val requestId = intent.getStringExtra("request_id") ?: return
        context.quietNotificationAction(requestId, intent.getIntExtra("occurrence", -1))
        val pending = goAsync()
        try { finishAfter(DeliveryWork.command(context, workDataOf("command" to "action", "action" to action.name,
            "request_id" to requestId, "occurrence" to intent.getIntExtra("occurrence", -1))), pending) }
        catch (e: Exception) { Log.e("AriciuDelivery", "Could not enqueue notification action", e); pending.finish() }
    }
}

class DeliveryRecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        val pending = goAsync()
        try { finishAfter(DeliveryWork.reconcile(context, force = true, restoreNotifications = intent.action == Intent.ACTION_BOOT_COMPLETED), pending) }
        catch (e: Exception) { Log.e("AriciuDelivery", "Could not enqueue recovery", e); pending.finish() }
    }
}

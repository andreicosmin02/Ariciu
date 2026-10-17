package ro.andrei.ariciu.feature.delivery.platform

import android.content.Context
import androidx.work.*
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ro.andrei.ariciu.feature.delivery.model.DeliveryRecord
import ro.andrei.ariciu.feature.delivery.model.DeliveryPhase
import ro.andrei.ariciu.feature.reminders.model.NotificationIntensity

/** One durable timer for each dismissed alert attempt; Room remains authoritative. */
internal object UrgentRetryWork {
    const val TAG = "urgent-realert"
    fun name(record: DeliveryRecord) = "urgent:${record.requestId}:${record.reminder.delivery.occurrence}:${record.reminder.delivery.notificationAttempt}"

    suspend fun synchronize(context: Context, records: List<DeliveryRecord>, runningId: UUID) = withContext(Dispatchers.IO) {
        val manager = WorkManager.getInstance(context)
        val desired = records.filter { it.reminder.intensity == NotificationIntensity.URGENT &&
            it.reminder.delivery.phase == DeliveryPhase.TRIGGERED && !it.reminder.delivery.notificationVisible &&
            it.reminder.delivery.urgentRetryAtMs > 0 }.associateBy(::name)
        manager.getWorkInfosByTag(TAG).get().filter { !it.state.isFinished && it.id != runningId &&
            it.tags.none { tag -> tag in desired.keys } }.forEach { manager.cancelWorkById(it.id).result.get() }
        desired.forEach { (name, record) ->
            val state = record.reminder.delivery
            manager.enqueueUniqueWork(name,ExistingWorkPolicy.KEEP,OneTimeWorkRequestBuilder<DeliveryWorker>()
                .addTag(TAG).addTag(name)
                .setInputData(workDataOf("command" to "urgent_retry", "request_id" to record.requestId,
                    "occurrence" to state.occurrence, "notification_attempt" to state.notificationAttempt))
                .setInitialDelay((state.urgentRetryAtMs - System.currentTimeMillis()).coerceAtLeast(0),TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build()).result.get()
        }
    }
}

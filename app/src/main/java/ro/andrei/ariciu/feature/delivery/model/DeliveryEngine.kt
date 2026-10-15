package ro.andrei.ariciu.feature.delivery.model

import ro.andrei.ariciu.feature.reminders.model.Reminder
import ro.andrei.ariciu.feature.reminders.model.ReminderTransition

data class DeliveryRecord(val reminder: Reminder, val generation: String) {
    val requestId: String get() = "${reminder.id}:$generation"
}

interface DeliveryStore {
    suspend fun all(): List<DeliveryRecord>
    suspend fun put(record: DeliveryRecord)
    suspend fun registeredIds(): Set<String>
    suspend fun track(ids: Set<String>)
    suspend fun untrack(ids: Set<String>)
    suspend fun <T> transaction(block: suspend () -> T): T
}

interface GeofenceBackend {
    suspend fun add(records: List<DeliveryRecord>)
    suspend fun remove(requestIds: Set<String>)
}

interface ReminderNotifications {
    fun post(record: DeliveryRecord): Boolean
    fun cancelExcept(reminderIds: Set<String>)
}

/** Call through the single runtime mutex; lifecycle mutations are also database transactions. */
class DeliveryEngine(
    private val store: DeliveryStore,
    private val backend: GeofenceBackend,
    private val notifications: ReminderNotifications,
) {
    suspend fun transition(requestIds: Set<String>, transition: ReminderTransition, elapsedMs: Long, boot: Int) {
        store.transaction {
            store.all().filter { it.requestId in requestIds }.forEach { record ->
                val reminder = record.reminder
                val next = reminder.delivery.onTransition(transition, reminder.transition, reminder.repeat, elapsedMs, boot)
                if (next != reminder.delivery) store.put(record.copy(reminder = reminder.copy(delivery = next)))
            }
        }
    }

    suspend fun action(requestId: String, occurrence: Int, action: OccurrenceAction) {
        store.transaction {
            store.all().firstOrNull { it.requestId == requestId }?.let { record ->
                val r = record.reminder
                val next = r.delivery.onAction(action, occurrence, r.transition)
                if (next != r.delivery) store.put(record.copy(reminder = r.copy(delivery = next)))
            }
        }
    }

    suspend fun invalidateRegistrations(message: String) {
        store.transaction {
            store.all().filter { it.reminder.delivery.active }.forEach {
                store.put(it.copy(reminder = it.reminder.copy(registration = RegistrationState.FAILED, deliveryError = message)))
            }
        }
    }

    suspend fun reconcile(capabilities: DeliveryCapabilities, force: Boolean = false) {
        val all = store.all()
        val selected = if (capabilities.ready) all.filter { it.reminder.delivery.active }
            .sortedBy { it.reminder.id }.take(100) else emptyList()
        val desiredIds = selected.map { it.requestId }.toSet()
        all.forEach { record ->
            val r = record.reminder
            val state = when {
                !r.delivery.active -> RegistrationState.INACTIVE
                !capabilities.ready -> RegistrationState.BLOCKED
                record.requestId !in desiredIds -> RegistrationState.LIMIT_REACHED
                force -> RegistrationState.PENDING
                else -> r.registration
            }
            val error = when (state) {
                RegistrationState.BLOCKED -> capabilities.reason
                RegistrationState.LIMIT_REACHED -> "100 areas are already monitored. Complete or delete another reminder."
                RegistrationState.FAILED -> r.deliveryError
                else -> null
            }
            store.put(record.copy(reminder = r.copy(registration = state, deliveryError = error)))
        }
        val obsolete = store.registeredIds() - desiredIds
        try {
            if (obsolete.isNotEmpty()) { backend.remove(obsolete); store.untrack(obsolete) }
            val current = store.all().filter { it.requestId in desiredIds }
            val known = store.registeredIds()
            val additions = current.filter { force || it.reminder.registration != RegistrationState.REGISTERED || it.requestId !in known }
            if (additions.isNotEmpty()) {
                // Journal before calling Play services: a crash during add remains safe to retry/remove.
                store.track(additions.map { it.requestId }.toSet())
                backend.add(additions)
                additions.forEach { store.put(it.copy(reminder = it.reminder.copy(
                    registration = RegistrationState.REGISTERED, deliveryError = null))) }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            selected.forEach { record ->
                store.put(record.copy(reminder = record.reminder.copy(registration = RegistrationState.FAILED,
                    deliveryError = "Monitoring could not be synchronized. Check location and Play services, then Retry.")))
            }
            throw e
        }
    }

    suspend fun dispatchNotifications(allowed: Boolean, restoreVisible: Boolean = false) {
        val all = store.all()
        notifications.cancelExcept(all.filter { it.reminder.delivery.notificationVisible }.map { it.reminder.id }.toSet())
        if (!allowed) return
        all.filter { it.reminder.delivery.notificationVisible &&
            (it.reminder.delivery.notificationPending || restoreVisible) }.forEach { record ->
            if (notifications.post(record)) store.put(record.copy(reminder = record.reminder.copy(
                delivery = record.reminder.delivery.copy(notificationPending = false))))
        }
    }
}

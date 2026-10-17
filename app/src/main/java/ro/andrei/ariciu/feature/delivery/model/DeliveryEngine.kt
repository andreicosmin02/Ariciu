package ro.andrei.ariciu.feature.delivery.model

import ro.andrei.ariciu.feature.reminders.model.Reminder
import ro.andrei.ariciu.feature.reminders.model.ReminderTransition
import ro.andrei.ariciu.feature.reminders.model.NotificationIntensity

data class DeliveryRecord(val reminder: Reminder, val generation: String, val area: ChainArea? = null) {
    val requestId: String get() = "${reminder.id}:$generation" + (area?.let { ":${it.id}" } ?: "")
    val monitoringAreas: List<DeliveryRecord> get() = if (reminder.chainName == null) listOf(this)
        else reminder.chainAreas.map { copy(area = it) }
    val location get() = area?.let { ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation(it.latitude, it.longitude, reminder.location.radiusMeters) } ?: reminder.location
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
    fun requiresRefresh(record: DeliveryRecord): Boolean = false
    fun post(record: DeliveryRecord): Boolean
    fun cancelExcept(reminderIds: Set<String>)
}

/** Call through the single runtime mutex; lifecycle mutations are also database transactions. */
class DeliveryEngine(
    private val store: DeliveryStore,
    private val backend: GeofenceBackend,
    private val notifications: ReminderNotifications,
    private val urgentDelayMs: Long = URGENT_RE_ALERT_DELAY_MS,
) {
    suspend fun transition(requestIds: Set<String>, transition: ReminderTransition, elapsedMs: Long, boot: Int) {
        store.transaction {
            store.all().forEach { record ->
                var reminder = record.reminder
                if (reminder.chainName == null) {
                    if (record.requestId in requestIds) reminder = reminder.copy(delivery = reminder.delivery.onTransition(
                        transition, reminder.transition, reminder.repeat, elapsedMs, boot, holdUnresolved = reminder.intensity == NotificationIntensity.URGENT))
                } else {
                    record.monitoringAreas.filter { it.requestId in requestIds }.sortedBy { it.requestId }.forEach {
                        reminder = reminder.crossChain(requireNotNull(it.area).id, transition, elapsedMs, boot)
                    }
                }
                if (reminder != record.reminder) store.put(record.copy(reminder = reminder))
            }
        }
    }

    /** Explicit foreground opt-in for the first occurrence of a new single-place arrival. */
    suspend fun immediateArrival(requestId: String, fix: ArrivalLocationFix, nowElapsedMs: Long, boot: Int): Boolean = store.transaction {
        val record = store.all().firstOrNull { it.requestId == requestId } ?: return@transaction false
        val r = record.reminder
        if (r.chainName != null || r.transition != ReminderTransition.ENTER || r.delivery.occurrence != 0 ||
            r.delivery.phase != DeliveryPhase.WAITING || assessArrival(r.location,fix,nowElapsedMs,boot) != ArrivalCheck.INSIDE)
            return@transaction false
        val next = r.delivery.onTransition(ReminderTransition.ENTER,r.transition,r.repeat,fix.elapsedMs,boot)
        if (next == r.delivery) return@transaction false
        store.put(record.copy(reminder = r.copy(delivery = next)))
        true
    }

    suspend fun action(requestId: String, occurrence: Int, action: OccurrenceAction, nowMs: Long = 0, notificationAttempt: Int = 0) {
        store.transaction {
            store.all().firstOrNull { it.requestId == requestId }?.let { record ->
                val r = record.reminder
                if (action in setOf(OccurrenceAction.STOP, OccurrenceAction.SNOOZE) &&
                    r.intensity != ro.andrei.ariciu.feature.reminders.model.NotificationIntensity.ALARM) return@let
                val next = if (action == OccurrenceAction.DISMISS) {
                    if (r.intensity != NotificationIntensity.URGENT) return@let
                    r.delivery.onUrgentDismiss(occurrence,notificationAttempt,nowMs,urgentDelayMs)
                } else r.delivery.onAction(action, occurrence, r.transition, nowMs,
                    allowUnresolvedUrgent = r.intensity == NotificationIntensity.URGENT)
                if (next != r.delivery) store.put(record.copy(reminder = r.copy(delivery = next)))
            }
        }
    }

    suspend fun wakeSnoozed(nowMs: Long) {
        store.transaction {
            store.all().forEach { record ->
                val next = record.reminder.delivery.onSnoozeDue(nowMs)
                if (next != record.reminder.delivery) store.put(record.copy(reminder = record.reminder.copy(delivery = next)))
            }
        }
    }

    /** Re-alerts remain the same occurrence; a separate attempt rejects old swipe broadcasts. */
    suspend fun wakeUrgentRetries(nowMs: Long, requestId: String? = null, occurrence: Int? = null, attempt: Int? = null) {
        store.transaction {
            store.all().filter { it.reminder.intensity == NotificationIntensity.URGENT &&
                (requestId == null || (it.requestId == requestId && it.reminder.delivery.occurrence == occurrence &&
                    it.reminder.delivery.notificationAttempt == attempt)) }.forEach { record ->
                val next = record.reminder.delivery.onUrgentRetryDue(nowMs)
                if (next != record.reminder.delivery) store.put(record.copy(reminder = record.reminder.copy(delivery = next)))
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
        val eligible = if (capabilities.ready) all.filter { it.reminder.delivery.active && it.reminder.intensity !in capabilities.blockedIntensities } else emptyList()
        val selected = allocateAreas(eligible)
        val desiredIds = selected.map { it.requestId }.toSet()
        val states = all.associate { record ->
            val r = record.reminder
            val count = record.monitoringAreas.count { it.requestId in desiredIds }
            r.id to when {
                !r.delivery.active -> RegistrationState.INACTIVE
                !capabilities.ready || r.intensity in capabilities.blockedIntensities -> RegistrationState.BLOCKED
                count == 0 -> RegistrationState.LIMIT_REACHED
                count < record.monitoringAreas.size -> RegistrationState.PARTIAL
                force -> RegistrationState.PENDING
                else -> r.registration
            }
        }
        all.forEach { record ->
            val state = states.getValue(record.reminder.id)
            val error = when (state) {
                RegistrationState.BLOCKED -> capabilities.reason ?: "Enable the ${record.reminder.intensity.label} reminders channel in Android settings."
                RegistrationState.LIMIT_REACHED -> "No monitoring capacity available, or no stores selected. Complete another reminder or change the chain area."
                RegistrationState.PARTIAL -> "Only some selected stores fit Android’s 100-area limit."
                RegistrationState.FAILED -> record.reminder.deliveryError
                else -> null
            }
            store.put(record.copy(reminder = record.reminder.copy(registration = state, deliveryError = error)))
        }
        val obsolete = store.registeredIds() - desiredIds
        try {
            if (obsolete.isNotEmpty()) { backend.remove(obsolete); store.untrack(obsolete) }
            val known = store.registeredIds()
            val current = store.all().filter { it.reminder.id in selected.map { s -> s.reminder.id } }
            val additions = current.flatMap { it.monitoringAreas }.filter { it.requestId in desiredIds &&
                (force || it.reminder.registration !in setOf(RegistrationState.REGISTERED, RegistrationState.PARTIAL) || it.requestId !in known) }
            if (additions.isNotEmpty()) {
                store.track(additions.map { it.requestId }.toSet())
                backend.add(additions)
            }
            current.forEach { record -> store.put(record.copy(reminder = record.reminder.copy(
                registration = if (record.monitoringAreas.any { it.requestId !in desiredIds }) RegistrationState.PARTIAL else RegistrationState.REGISTERED,
                deliveryError = null))) }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            store.all().filter { it.reminder.id in selected.map { s -> s.reminder.id } }.forEach { record ->
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
        suspend fun deliver(record: DeliveryRecord) {
            val refresh = notifications.requiresRefresh(record)
            if (!record.reminder.delivery.notificationVisible ||
                !(record.reminder.delivery.notificationPending || restoreVisible || refresh)) return
            val presentation = if (!record.reminder.delivery.notificationPending)
                record.copy(reminder = record.reminder.copy(delivery = record.reminder.delivery.copy(quiet = true))) else record
            val persisted = if (record.reminder.intensity == NotificationIntensity.ALARM) presentation else record
            if (notifications.post(presentation)) store.put(persisted.copy(reminder = persisted.reminder.copy(
                delivery = persisted.reminder.delivery.copy(notificationPending = false))))
        }
        all.filter { it.reminder.delivery.notificationVisible }.forEach { snapshot ->
            if (snapshot.reminder.intensity == NotificationIntensity.URGENT) store.transaction {
                // Serialize the final validation/post with card completion/edit/delete transactions.
                store.all().firstOrNull { it.requestId == snapshot.requestId }?.let { deliver(it) }
            } else deliver(snapshot)
        }
    }
}

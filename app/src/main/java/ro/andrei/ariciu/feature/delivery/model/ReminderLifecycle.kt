package ro.andrei.ariciu.feature.delivery.model

import ro.andrei.ariciu.feature.reminders.model.ReminderRepeat
import ro.andrei.ariciu.feature.reminders.model.ReminderTransition

enum class DeliveryPhase { WAITING, TRIGGERED, WAITING_NEXT_VISIT, COMPLETED }
enum class AreaPosition { UNKNOWN, INSIDE, OUTSIDE }
enum class RegistrationState { PENDING, REGISTERED, BLOCKED, FAILED, LIMIT_REACHED, INACTIVE }
enum class OccurrenceAction { DONE, NEXT_TIME }

data class DeliveryState(
    val phase: DeliveryPhase = DeliveryPhase.WAITING,
    val position: AreaPosition = AreaPosition.UNKNOWN,
    val occurrence: Int = 0,
    val notificationVisible: Boolean = false,
    val notificationPending: Boolean = false,
    val lastEventElapsedMs: Long = -1,
    val lastEventBoot: Int = -1,
) {
    val active: Boolean get() = phase != DeliveryPhase.COMPLETED

    fun onTransition(event: ReminderTransition, wanted: ReminderTransition, repeat: ReminderRepeat,
        elapsedMs: Long, boot: Int): DeliveryState {
        if (!active || (lastEventBoot == boot && elapsedMs <= lastEventElapsedMs)) return this
        val newPosition = if (event == ReminderTransition.ENTER) AreaPosition.INSIDE else AreaPosition.OUTSIDE
        // Do not advance the timestamp for a same-position duplicate: it carries no new crossing.
        if (position == newPosition) return this
        val updated = copy(position = newPosition, lastEventElapsedMs = elapsedMs, lastEventBoot = boot)
        if (event != wanted) {
            return if (phase == DeliveryPhase.WAITING_NEXT_VISIT ||
                (phase == DeliveryPhase.TRIGGERED && repeat == ReminderRepeat.EVERY_VISIT))
                updated.copy(phase = DeliveryPhase.WAITING) else updated
        }
        if (phase != DeliveryPhase.WAITING) return updated
        return updated.copy(phase = DeliveryPhase.TRIGGERED, occurrence = occurrence + 1,
            notificationVisible = true, notificationPending = true)
    }

    fun onAction(action: OccurrenceAction, token: Int, wanted: ReminderTransition): DeliveryState {
        if (!active || !notificationVisible || occurrence != token) return this
        if (action == OccurrenceAction.DONE) return copy(phase = DeliveryPhase.COMPLETED,
            notificationVisible = false, notificationPending = false)
        val triggeringPosition = if (wanted == ReminderTransition.ENTER) AreaPosition.INSIDE else AreaPosition.OUTSIDE
        return copy(phase = if (position == triggeringPosition || position == AreaPosition.UNKNOWN)
            DeliveryPhase.WAITING_NEXT_VISIT else DeliveryPhase.WAITING,
            notificationVisible = false, notificationPending = false)
    }
}

data class DeliveryCapabilities(
    val preciseLocation: Boolean,
    val backgroundLocation: Boolean,
    val locationEnabled: Boolean,
    val playServices: Boolean,
    val notifications: Boolean,
) {
    val ready: Boolean get() = preciseLocation && backgroundLocation && locationEnabled && playServices && notifications
    val reason: String? get() = when {
        !playServices -> "Google Play services is unavailable. Install or update it to enable reminders."
        !preciseLocation -> "Allow precise location so Ariciu can detect your chosen areas."
        !backgroundLocation -> "Allow location all the time so reminders can work while Ariciu is closed."
        !locationEnabled -> "Turn on device location to detect arrivals and departures."
        !notifications -> "Allow reminder notifications in Android Settings."
        else -> null
    }
}

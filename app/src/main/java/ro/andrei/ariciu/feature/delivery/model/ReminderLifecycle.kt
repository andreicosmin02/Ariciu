package ro.andrei.ariciu.feature.delivery.model

import ro.andrei.ariciu.feature.reminders.model.ReminderRepeat
import ro.andrei.ariciu.feature.reminders.model.ReminderTransition

enum class DeliveryPhase { WAITING, TRIGGERED, WAITING_NEXT_VISIT, COMPLETED }
enum class AreaPosition { UNKNOWN, INSIDE, OUTSIDE }
enum class RegistrationState { PENDING, REGISTERED, BLOCKED, FAILED, PARTIAL, LIMIT_REACHED, INACTIVE }
enum class OccurrenceAction { DONE, NEXT_TIME, STOP, SNOOZE }

data class DeliveryState(
    val phase: DeliveryPhase = DeliveryPhase.WAITING,
    val position: AreaPosition = AreaPosition.UNKNOWN,
    val occurrence: Int = 0,
    val notificationVisible: Boolean = false,
    val notificationPending: Boolean = false,
    val lastEventElapsedMs: Long = -1,
    val lastEventBoot: Int = -1,
    val quiet: Boolean = false,
    val snoozeUntilMs: Long = 0,
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
            notificationVisible = true, notificationPending = true, quiet = false, snoozeUntilMs = 0)
    }

    /** Explicit task completion also works before a crossing or while snoozed. */
    fun completed() = copy(phase = DeliveryPhase.COMPLETED, notificationVisible = false,
        notificationPending = false, quiet = true, snoozeUntilMs = 0)

    fun onAction(action: OccurrenceAction, token: Int, wanted: ReminderTransition, nowMs: Long = 0): DeliveryState {
        if (!active || !notificationVisible || occurrence != token) return this
        if (action == OccurrenceAction.STOP) return if (quiet) this else copy(quiet = true, notificationPending = true)
        if (action == OccurrenceAction.SNOOZE) return copy(notificationVisible = false, notificationPending = false,
            quiet = false, snoozeUntilMs = nowMs + 10 * 60_000)
        if (action == OccurrenceAction.DONE) return completed()
        val triggeringPosition = if (wanted == ReminderTransition.ENTER) AreaPosition.INSIDE else AreaPosition.OUTSIDE
        return copy(phase = if (position == triggeringPosition || position == AreaPosition.UNKNOWN)
            DeliveryPhase.WAITING_NEXT_VISIT else DeliveryPhase.WAITING,
            notificationVisible = false, notificationPending = false, snoozeUntilMs = 0)
    }

    fun onSnoozeDue(nowMs: Long): DeliveryState = if (active && snoozeUntilMs > 0 && nowMs >= snoozeUntilMs)
        copy(snoozeUntilMs = 0, occurrence = occurrence + 1, quiet = false,
            notificationVisible = true, notificationPending = true) else this
}

data class DeliveryCapabilities(
    val preciseLocation: Boolean,
    val backgroundLocation: Boolean,
    val locationEnabled: Boolean,
    val playServices: Boolean,
    val notifications: Boolean,
    val blockedIntensities: Set<ro.andrei.ariciu.feature.reminders.model.NotificationIntensity> = emptySet(),
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

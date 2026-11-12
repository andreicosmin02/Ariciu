package ro.andrei.arichu.feature.reminders.ui

import ro.andrei.arichu.feature.delivery.model.*
import ro.andrei.arichu.feature.reminders.model.Reminder

data class ReminderStatus(val label: String, val description: String, val needsAttention: Boolean = false)

fun Reminder.presentStatus(capabilities: DeliveryCapabilities): ReminderStatus = when {
    delivery.phase == DeliveryPhase.COMPLETED -> ReminderStatus("Completed", "Turned off.")
    unresolvedUrgent && (delivery.urgentRetryAtMs > 0 || (delivery.notificationAttempt > 0 && delivery.notificationPending)) -> if (!capabilities.notifications || intensity in capabilities.blockedIntensities)
        ReminderStatus("Re-alert waiting", "Allow Urgent notifications in Settings, or choose Mark done or Next time.",true)
    else if (delivery.urgentRetryAtMs == 0L) ReminderStatus("Re-alert waiting", "Urgent alert waiting. Android may delay it. Mark done or Next time stops re-alerts.")
    else ReminderStatus("Re-alert scheduled", "Next Urgent alert around ${java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(delivery.urgentRetryAtMs))}. Android may delay it. Mark done or Next time stops re-alerts.")
    !capabilities.ready || intensity in capabilities.blockedIntensities -> ReminderStatus("Setup needed", "Review delivery in Settings.", true)
    registration == RegistrationState.FAILED -> ReminderStatus("Needs attention", "Couldn’t turn this on. Try again in Settings.", true)
    registration == RegistrationState.PARTIAL -> ReminderStatus("Some stores active", "Only part of this chain fits Android’s 100-area limit. Complete or delete a reminder to make room.", true)
    registration == RegistrationState.LIMIT_REACHED -> ReminderStatus("On hold", "100 places are active. Complete one to make room.", true)
    registration == RegistrationState.BLOCKED -> ReminderStatus("Setup needed", "Review delivery in Settings.", true)
    registration == RegistrationState.PENDING -> ReminderStatus("Getting ready", "Setting up this place.")
    delivery.snoozeUntilMs > 0 -> ReminderStatus("Snoozed", "We’ll alert you again in about 10 minutes. Android may delay this.")
    delivery.quiet && delivery.notificationVisible -> ReminderStatus("Sound stopped", "Choose Done or Snooze in the notification.")
    delivery.notificationPending -> ReminderStatus("Reminder ready", "Notification waiting to be delivered.")
    unresolvedUrgent -> ReminderStatus("Urgent unresolved", "Swiping delays this by about 5 minutes. Mark done or Next time stops re-alerts.")
    delivery.phase == DeliveryPhase.TRIGGERED -> ReminderStatus("Reminded", "Mark done here, or use the notification.")
    delivery.phase == DeliveryPhase.WAITING_NEXT_VISIT -> ReminderStatus("Next visit", if (transition == ro.andrei.arichu.feature.reminders.model.ReminderTransition.ENTER) "Waiting for you to leave and return." else "Waiting for you to return and leave again.")
    else -> ReminderStatus("Ready", "Listening for your next ${if (transition == ro.andrei.arichu.feature.reminders.model.ReminderTransition.ENTER) "arrival" else "departure"}.")
}

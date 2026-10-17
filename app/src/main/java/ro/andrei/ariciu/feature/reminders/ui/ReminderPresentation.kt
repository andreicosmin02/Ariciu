package ro.andrei.ariciu.feature.reminders.ui

import ro.andrei.ariciu.feature.delivery.model.*
import ro.andrei.ariciu.feature.reminders.model.Reminder

data class ReminderStatus(val label: String, val description: String, val needsAttention: Boolean = false)

fun Reminder.presentStatus(capabilities: DeliveryCapabilities): ReminderStatus = when {
    delivery.phase == DeliveryPhase.COMPLETED -> ReminderStatus("Completed", "This reminder is turned off.")
    unresolvedUrgent && (delivery.urgentRetryAtMs > 0 || (delivery.notificationAttempt > 0 && delivery.notificationPending)) -> if (!capabilities.notifications || intensity in capabilities.blockedIntensities)
        ReminderStatus("Re-alert waiting", "Urgent is unresolved. Allow Urgent notifications in Settings, or choose Mark done or Next time.",true)
    else if (delivery.urgentRetryAtMs == 0L) ReminderStatus("Re-alert waiting", "This Urgent alert is waiting to be delivered. Android may delay it. Choose Mark done or Next time to stop re-alerts.")
    else ReminderStatus("Re-alert scheduled", "Another Urgent alert is due around ${java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(delivery.urgentRetryAtMs))}. Android may delay it. Choose Mark done or Next time to stop re-alerts.")
    !capabilities.ready || intensity in capabilities.blockedIntensities -> ReminderStatus("Setup needed", "Review reminder delivery in Settings.", true)
    registration == RegistrationState.FAILED -> ReminderStatus("Needs attention", "We couldn’t turn on this reminder. Try again in Settings.", true)
    registration == RegistrationState.PARTIAL -> ReminderStatus("Some stores active", "Only part of this chain fits Android’s 100-area limit. Complete or delete another reminder to make room.", true)
    registration == RegistrationState.LIMIT_REACHED -> ReminderStatus("On hold", "100 places are already active. Complete a reminder to make room.", true)
    registration == RegistrationState.BLOCKED -> ReminderStatus("Setup needed", "Review reminder delivery in Settings.", true)
    registration == RegistrationState.PENDING -> ReminderStatus("Getting ready", "We’re setting up this place.")
    delivery.snoozeUntilMs > 0 -> ReminderStatus("Snoozed", "We’ll alert you again in about 10 minutes. Android may delay this.")
    delivery.quiet && delivery.notificationVisible -> ReminderStatus("Sound stopped", "Choose Done or Snooze in your notification.")
    delivery.notificationPending -> ReminderStatus("Reminder ready", "Your notification is waiting to be delivered.")
    unresolvedUrgent -> ReminderStatus("Urgent unresolved", "Swiping delays this alert by about 5 minutes. Choose Mark done or Next time to stop re-alerts.")
    delivery.phase == DeliveryPhase.TRIGGERED -> ReminderStatus("Reminded", "Mark Done here, or choose an action in your notification.")
    delivery.phase == DeliveryPhase.WAITING_NEXT_VISIT -> ReminderStatus("Next visit", if (transition == ro.andrei.ariciu.feature.reminders.model.ReminderTransition.ENTER) "We’ll wait for you to leave and return." else "We’ll wait for you to return and leave again.")
    else -> ReminderStatus("Ready", "Listening for your next ${if (transition == ro.andrei.ariciu.feature.reminders.model.ReminderTransition.ENTER) "arrival" else "departure"}.")
}

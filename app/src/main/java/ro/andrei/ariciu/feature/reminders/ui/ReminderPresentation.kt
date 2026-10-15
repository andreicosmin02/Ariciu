package ro.andrei.ariciu.feature.reminders.ui

import ro.andrei.ariciu.feature.delivery.model.*
import ro.andrei.ariciu.feature.reminders.model.Reminder

data class ReminderStatus(val label: String, val description: String, val needsAttention: Boolean = false)

fun Reminder.presentStatus(capabilities: DeliveryCapabilities): ReminderStatus = when {
    delivery.phase == DeliveryPhase.COMPLETED -> ReminderStatus("Completed", "This reminder is turned off.")
    !capabilities.ready -> ReminderStatus("Setup needed", "Review reminder delivery in Settings.", true)
    registration == RegistrationState.FAILED -> ReminderStatus("Needs attention", "We couldn’t turn on this reminder. Try again in Settings.", true)
    registration == RegistrationState.LIMIT_REACHED -> ReminderStatus("On hold", "100 places are already active. Complete a reminder to make room.", true)
    registration == RegistrationState.BLOCKED -> ReminderStatus("Setup needed", "Review reminder delivery in Settings.", true)
    registration == RegistrationState.PENDING -> ReminderStatus("Getting ready", "We’re setting up this place.")
    delivery.notificationPending -> ReminderStatus("Reminder ready", "Your notification is waiting to be delivered.")
    delivery.phase == DeliveryPhase.TRIGGERED -> ReminderStatus("Reminded", "Choose Done or Next time in your notification.")
    delivery.phase == DeliveryPhase.WAITING_NEXT_VISIT -> ReminderStatus("Next visit", if (transition == ro.andrei.ariciu.feature.reminders.model.ReminderTransition.ENTER) "We’ll wait for you to leave and return." else "We’ll wait for you to return and leave again.")
    else -> ReminderStatus("Ready", "Listening for your next ${if (transition == ro.andrei.ariciu.feature.reminders.model.ReminderTransition.ENTER) "arrival" else "departure"}.")
}

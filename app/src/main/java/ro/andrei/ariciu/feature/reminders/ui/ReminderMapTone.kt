package ro.andrei.ariciu.feature.reminders.ui

import ro.andrei.ariciu.feature.delivery.model.*
import ro.andrei.ariciu.feature.reminders.model.Reminder

/** Map pin status. Always shown with [label]; colour is never the only signal. */
enum class MapTone(val label: String) { READY("Ready"), WAITING("Getting ready"), ATTENTION("Needs attention"), DONE("Completed") }

fun Reminder.mapTone(capabilities: DeliveryCapabilities): MapTone = when {
    !delivery.active -> MapTone.DONE
    presentStatus(capabilities).needsAttention -> MapTone.ATTENTION
    registration == RegistrationState.PENDING -> MapTone.WAITING
    else -> MapTone.READY
}

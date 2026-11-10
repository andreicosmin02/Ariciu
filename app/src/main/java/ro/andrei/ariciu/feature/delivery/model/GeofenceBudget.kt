package ro.andrei.ariciu.feature.delivery.model

import ro.andrei.ariciu.feature.reminders.model.Reminder

const val GEOFENCE_LIMIT = 100

fun Reminder.areaCount(): Int = if (chainName == null) 1 else chainAreas.size

/**
 * Areas currently taking part in Android's 100-area budget, from stored registration states.
 * PARTIAL and LIMIT_REACHED are only set when the budget was exhausted, so they mean "full".
 * This reflects Ariciu's last reconcile, not a live read of Google Play services.
 */
fun geofenceAreasUsed(reminders: List<Reminder>): Int {
    val active = reminders.filter { it.delivery.active }
    if (active.any { it.registration == RegistrationState.PARTIAL || it.registration == RegistrationState.LIMIT_REACHED }) return GEOFENCE_LIMIT
    return active.filter { it.registration == RegistrationState.REGISTERED }.sumOf { it.areaCount() }.coerceIn(0, GEOFENCE_LIMIT)
}

fun geofenceBudgetText(used: Int) = "${used.coerceIn(0, GEOFENCE_LIMIT)}/$GEOFENCE_LIMIT areas used"

/** Whether [adding] more areas fit; [replacing] are the areas the edited reminder already holds. */
fun geofenceFits(used: Int, adding: Int, replacing: Int = 0) = (used - replacing).coerceAtLeast(0) + adding <= GEOFENCE_LIMIT

fun geofenceBlockMessage(used: Int, adding: Int, replacing: Int = 0): String {
    val free = (GEOFENCE_LIMIT - (used - replacing).coerceAtLeast(0)).coerceAtLeast(0)
    return "This needs $adding areas but only $free of $GEOFENCE_LIMIT are free. Complete or delete a reminder, or pick fewer places."
}

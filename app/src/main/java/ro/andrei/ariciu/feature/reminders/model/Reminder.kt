package ro.andrei.ariciu.feature.reminders.model

import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import ro.andrei.ariciu.feature.delivery.model.DeliveryState
import ro.andrei.ariciu.feature.delivery.model.RegistrationState

enum class ReminderTransition { ENTER, EXIT }
enum class ReminderRepeat { ONCE, EVERY_VISIT }

data class Reminder(
    val id: String,
    val title: String,
    val location: SelectedLocation,
    val transition: ReminderTransition,
    val repeat: ReminderRepeat,
    val delivery: DeliveryState = DeliveryState(),
    val registration: RegistrationState = RegistrationState.PENDING,
    val deliveryError: String? = null,
) {
    fun toDraft() = ReminderDraft(title, location, transition, repeat)
}

data class ReminderDraft(
    val title: String = "",
    val location: SelectedLocation? = null,
    val transition: ReminderTransition = ReminderTransition.ENTER,
    val repeat: ReminderRepeat = ReminderRepeat.ONCE,
) {
    val canSave: Boolean
        get() = title.isNotBlank() && location?.let {
            it.latitude.isFinite() && it.latitude in -90.0..90.0 &&
                it.longitude.isFinite() && it.longitude in -180.0..180.0 &&
                it.radiusMeters in 100..1000 && it.radiusMeters % 100 == 0
        } == true

    fun toReminder(id: String): Reminder {
        require(canSave) { "A title and a valid location are required" }
        return Reminder(id, title.trim(), requireNotNull(location), transition, repeat)
    }
}

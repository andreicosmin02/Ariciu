package ro.andrei.ariciu.feature.reminders.model

import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import ro.andrei.ariciu.feature.delivery.model.ChainArea
import ro.andrei.ariciu.feature.delivery.model.DeliveryState
import ro.andrei.ariciu.feature.delivery.model.RegistrationState

enum class ReminderTransition { ENTER, EXIT }
enum class ReminderRepeat { ONCE, EVERY_VISIT }
enum class NotificationIntensity(val label: String, val explanation: String) {
    SILENT("Silent", "In the notification list. No sound, vibration or pop-up."),
    NORMAL("Normal", "A standard reminder alert, following your phone settings."),
    URGENT("Urgent", "An ongoing alert. If you swipe it away, it alerts again after about 5 minutes until you choose Done or Next time. Android may delay it; sound follows your phone settings."),
    ALARM("Alarm", "Repeating notification sound and strong vibration, where permitted. Stop, Done or Snooze for 10 minutes. Opening notifications may stop the sound. No automatic full-screen launch.");
    companion object { val available = listOf(SILENT, NORMAL, URGENT) }
}

data class Reminder(
    val id: String,
    val title: String,
    val location: SelectedLocation,
    val transition: ReminderTransition,
    val repeat: ReminderRepeat,
    val delivery: DeliveryState = DeliveryState(),
    val registration: RegistrationState = RegistrationState.PENDING,
    val deliveryError: String? = null,
    val intensity: NotificationIntensity = NotificationIntensity.NORMAL,
    val chainName: String? = null,
    val chainAreas: List<ChainArea> = emptyList(),
) {
    val unresolvedUrgent: Boolean get() = intensity == NotificationIntensity.URGENT && delivery.phase == ro.andrei.ariciu.feature.delivery.model.DeliveryPhase.TRIGGERED
    fun toDraft() = ReminderDraft(title, location, transition, repeat, intensity, chainName, chainAreas)
}

data class ReminderDraft(
    val title: String = "",
    val location: SelectedLocation? = null,
    val transition: ReminderTransition = ReminderTransition.ENTER,
    val repeat: ReminderRepeat = ReminderRepeat.ONCE,
    val intensity: NotificationIntensity = NotificationIntensity.NORMAL,
    val chainName: String? = null,
    val chainAreas: List<ChainArea> = emptyList(),
    val notifyOnSave: Boolean = false,
) {
    val canCheckArrival: Boolean get() = chainName == null && transition == ReminderTransition.ENTER
    val canSave: Boolean
        get() = title.isNotBlank() && (chainName == null || (chainName.isNotBlank() && chainAreas.size in 1..40 &&
            chainAreas.map { it.id }.distinct().size == chainAreas.size && chainAreas.all { area ->
                area.id.matches(Regex("[nw][0-9]+")) && area.latitude.isFinite() && area.latitude in -90.0..90.0 &&
                    area.longitude.isFinite() && area.longitude in -180.0..180.0 })) && location?.let {
            it.latitude.isFinite() && it.latitude in -90.0..90.0 &&
                it.longitude.isFinite() && it.longitude in -180.0..180.0 &&
                it.radiusMeters in 100..1000 && it.radiusMeters % 100 == 0
        } == true

    fun toReminder(id: String): Reminder {
        require(canSave) { "A title and a valid location are required" }
        return Reminder(id, title.trim(), requireNotNull(location), transition, repeat, intensity = intensity, chainName = chainName, chainAreas = chainAreas)
    }
}

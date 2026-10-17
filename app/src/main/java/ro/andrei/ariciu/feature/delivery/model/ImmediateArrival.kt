package ro.andrei.ariciu.feature.delivery.model

import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import ro.andrei.ariciu.feature.places.distanceMeters
import ro.andrei.ariciu.feature.reminders.model.Reminder

const val ARRIVAL_MAX_AGE_MS = 10_000L
/** Ephemeral evidence only; no continuous tracking or new persisted location history. */
data class ArrivalLocationFix(val latitude: Double, val longitude: Double, val accuracyMeters: Double,
    val elapsedMs: Long, val boot: Int)
enum class ArrivalCheck { INSIDE, OUTSIDE, UNCERTAIN, STALE }

fun assessArrival(area: SelectedLocation, fix: ArrivalLocationFix, nowElapsedMs: Long, boot: Int): ArrivalCheck {
    if (fix.boot != boot || fix.elapsedMs < 0 || nowElapsedMs < fix.elapsedMs || nowElapsedMs - fix.elapsedMs > ARRIVAL_MAX_AGE_MS)
        return ArrivalCheck.STALE
    if (!fix.latitude.isFinite() || fix.latitude !in -90.0..90.0 || !fix.longitude.isFinite() || fix.longitude !in -180.0..180.0 ||
        !fix.accuracyMeters.isFinite() || fix.accuracyMeters < 0 || fix.accuracyMeters > 50) return ArrivalCheck.UNCERTAIN
    val distance = distanceMeters(area.latitude,area.longitude,fix.latitude,fix.longitude)
    // Conservative heuristic, not a guarantee: the doubled reported uncertainty must fit inside.
    return when {
        distance + 2 * fix.accuracyMeters <= area.radiusMeters -> ArrivalCheck.INSIDE
        distance - 2 * fix.accuracyMeters > area.radiusMeters -> ArrivalCheck.OUTSIDE
        else -> ArrivalCheck.UNCERTAIN
    }
}

enum class ImmediateArrivalResult(val message: String) {
    REQUESTED("Reminder saved. You’re inside; an alert was requested using your chosen intensity."),
    OUTSIDE("Reminder saved. You’re outside this area; it will wait for an arrival."),
    UNCERTAIN("Reminder saved. Location was not accurate enough to confirm you’re inside. No immediate alert requested."),
    STALE("Reminder saved. A fresh enough location was unavailable. No immediate alert requested."),
    PRECISE_REQUIRED("Reminder saved. Allow precise location in Settings to use the immediate check."),
    LOCATION_OFF("Reminder saved. Phone Location is off; no immediate check was made."),
    NOTIFICATIONS_OFF("Reminder saved. Enable notifications for this alert type in Settings."),
    UNAVAILABLE("Reminder saved. Could not check your location now. Normal monitoring is unchanged."),
    BACKGROUNDED("Reminder saved. The immediate check stopped when Ariciu left the foreground."),
    NO_LONGER_ELIGIBLE("Reminder saved. The immediate alert was skipped because the reminder or location changed."),
}

fun interface ImmediateArrivalAttempt {
    suspend fun check(requestId: String, reminder: Reminder): ImmediateArrivalResult
}

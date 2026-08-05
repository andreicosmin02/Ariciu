package ro.andrei.ariciu

import kotlin.math.roundToInt

const val MIN_RADIUS_METERS = 100
const val MAX_RADIUS_METERS = 1000
const val RADIUS_STEP_METERS = 100
const val DEFAULT_RADIUS_METERS = 200
const val RADIUS_SLIDER_STEPS =
    (MAX_RADIUS_METERS - MIN_RADIUS_METERS) / RADIUS_STEP_METERS - 1

fun snapRadiusMeters(rawRadiusMeters: Float): Int {
    val clampedRadius = rawRadiusMeters.coerceIn(
        MIN_RADIUS_METERS.toFloat(),
        MAX_RADIUS_METERS.toFloat(),
    )
    val stepCount = ((clampedRadius - MIN_RADIUS_METERS) / RADIUS_STEP_METERS).roundToInt()
    return MIN_RADIUS_METERS + stepCount * RADIUS_STEP_METERS
}

fun formatRadiusMeters(radiusMeters: Int): String = "Radius: $radiusMeters m"

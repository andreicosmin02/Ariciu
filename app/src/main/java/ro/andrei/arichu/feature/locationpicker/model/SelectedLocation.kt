package ro.andrei.arichu.feature.locationpicker.model

data class SelectedLocation(
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
)

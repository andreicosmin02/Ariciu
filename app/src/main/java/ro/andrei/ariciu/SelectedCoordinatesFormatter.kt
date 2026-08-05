package ro.andrei.ariciu

import java.util.Locale

fun SelectedCoordinates.formatLatitude(): String =
    String.format(Locale.ROOT, "Latitude: %.5f", latitude)

fun SelectedCoordinates.formatLongitude(): String =
    String.format(Locale.ROOT, "Longitude: %.5f", longitude)

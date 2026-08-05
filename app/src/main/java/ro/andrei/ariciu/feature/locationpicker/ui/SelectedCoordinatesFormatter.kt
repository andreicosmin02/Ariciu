package ro.andrei.ariciu.feature.locationpicker.ui

import java.util.Locale
import ro.andrei.ariciu.feature.locationpicker.model.SelectedCoordinates

fun SelectedCoordinates.formatLatitude(): String =
    String.format(Locale.ROOT, "Latitude: %.5f", latitude)

fun SelectedCoordinates.formatLongitude(): String =
    String.format(Locale.ROOT, "Longitude: %.5f", longitude)

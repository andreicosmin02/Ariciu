package ro.andrei.ariciu.feature.locationpicker.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import ro.andrei.ariciu.feature.locationpicker.model.SelectedCoordinates
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun Context.hasMapLocationPermission() = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    .any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

@Suppress("MissingPermission")
suspend fun Context.currentMapPosition(): SelectedCoordinates? {
    check(hasMapLocationPermission()) { "Allow location to find where you are." }
    val manager = getSystemService(LocationManager::class.java)
    val enabled = if (android.os.Build.VERSION.SDK_INT >= 28) manager.isLocationEnabled else
        manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    check(enabled) { "Turn on phone Location, then try again." }
    return withTimeoutOrNull(20_000) {
        suspendCancellableCoroutine { continuation ->
            val cancellation = CancellationTokenSource()
            continuation.invokeOnCancellation { cancellation.cancel() }
            LocationServices.getFusedLocationProviderClient(this@currentMapPosition).getCurrentLocation(
                CurrentLocationRequest.Builder().setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setMaxUpdateAgeMillis(30_000).setDurationMillis(18_000).build(), cancellation.token)
                .addOnSuccessListener { location -> if (continuation.isActive) continuation.resume(location?.let { SelectedCoordinates(it.latitude, it.longitude) }) }
                .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
                .addOnCanceledListener { continuation.cancel() }
        }
    }
}

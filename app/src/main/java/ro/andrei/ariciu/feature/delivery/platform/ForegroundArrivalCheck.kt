package ro.andrei.ariciu.feature.delivery.platform

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import ro.andrei.ariciu.feature.delivery.model.*
import ro.andrei.ariciu.feature.reminders.model.Reminder

/** A single bounded foreground fix; stopping the visible UI cancels the provider request. */
class ForegroundArrivalCheck(private val context: Context, private val lifecycle: Lifecycle,
    private val locate: suspend () -> android.location.Location? = { context.arrivalLocationFix() },
    private val availability: () -> DeliveryCapabilities = { context.deliveryCapabilities() },
    private val timeoutMillis: Long = 20_000) : ImmediateArrivalAttempt {
    @Suppress("MissingPermission")
    override suspend fun check(requestId: String, reminder: Reminder): ImmediateArrivalResult = withContext(Dispatchers.Main.immediate) {
        if (!foreground()) return@withContext ImmediateArrivalResult.BACKGROUNDED
        val capabilities = availability()
        if (!capabilities.preciseLocation) return@withContext ImmediateArrivalResult.PRECISE_REQUIRED
        if (!capabilities.locationEnabled) return@withContext ImmediateArrivalResult.LOCATION_OFF
        if (!capabilities.playServices) return@withContext ImmediateArrivalResult.UNAVAILABLE
        if (!capabilities.notifications || reminder.intensity in capabilities.blockedIntensities)
            return@withContext ImmediateArrivalResult.NOTIFICATIONS_OFF
        var stopped = false
        withTimeoutOrNull(timeoutMillis) {
            val location = coroutineScope {
                val fix = async {
                    locate()
                }
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_DESTROY) { stopped = true; fix.cancel() }
                }
                lifecycle.addObserver(observer)
                try { fix.await() }
                catch (e: CancellationException) { currentCoroutineContext().ensureActive(); null }
                finally { lifecycle.removeObserver(observer) }
            }
            if (stopped || !foreground()) return@withTimeoutOrNull ImmediateArrivalResult.BACKGROUNDED
            if (location == null) return@withTimeoutOrNull ImmediateArrivalResult.UNAVAILABLE
            val boot = Settings.Global.getInt(context.contentResolver,Settings.Global.BOOT_COUNT,0)
            val evidence = ArrivalLocationFix(location.latitude,location.longitude,
                if (location.hasAccuracy()) location.accuracy.toDouble() else Double.NaN, location.elapsedRealtimeNanos / 1_000_000,boot)
            when (assessArrival(reminder.location,evidence,SystemClock.elapsedRealtime(),boot)) {
                ArrivalCheck.OUTSIDE -> ImmediateArrivalResult.OUTSIDE
                ArrivalCheck.UNCERTAIN -> ImmediateArrivalResult.UNCERTAIN
                ArrivalCheck.STALE -> ImmediateArrivalResult.STALE
                ArrivalCheck.INSIDE -> if (DeliveryWork.immediateArrival(context,requestId,evidence,::foreground))
                    ImmediateArrivalResult.REQUESTED else if (!foreground()) ImmediateArrivalResult.BACKGROUNDED else ImmediateArrivalResult.NO_LONGER_ELIGIBLE
            }
        } ?: if (stopped || !foreground()) ImmediateArrivalResult.BACKGROUNDED else ImmediateArrivalResult.UNAVAILABLE
    }
    private fun foreground() = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
}

@Suppress("MissingPermission")
private suspend fun Context.arrivalLocationFix(): android.location.Location? = suspendCancellableCoroutine { continuation ->
    val token = CancellationTokenSource()
    continuation.invokeOnCancellation { token.cancel() }
    LocationServices.getFusedLocationProviderClient(this).getCurrentLocation(
        CurrentLocationRequest.Builder().setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setGranularity(Granularity.GRANULARITY_FINE).setMaxUpdateAgeMillis(5_000).setDurationMillis(18_000).build(),token.token)
        .addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
        .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
        .addOnCanceledListener { if (continuation.isActive) continuation.resume(null) }
}

package ro.andrei.ariciu.feature.delivery.platform

import androidx.lifecycle.*
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import ro.andrei.ariciu.feature.delivery.model.*
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import ro.andrei.ariciu.feature.reminders.model.*

class ForegroundArrivalCheckTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val ready = DeliveryCapabilities(true,true,true,true,true)
    private val reminder = Reminder("new","Here",SelectedLocation(45.0,25.0,200),ReminderTransition.ENTER,ReminderRepeat.ONCE)
    private class Owner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }
    private fun owner(): Owner = Owner().also { owner -> instrumentation.runOnMainSync {
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE); owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    } }
    @Test fun missingOrApproximatePermissionAndUnavailableServicesNeverRequestGps() = runBlocking {
        var requests = 0
        val owner = owner()
        for ((capabilities,result) in listOf(ready.copy(preciseLocation = false) to ImmediateArrivalResult.PRECISE_REQUIRED,
            ready.copy(locationEnabled = false) to ImmediateArrivalResult.LOCATION_OFF,
            ready.copy(playServices = false) to ImmediateArrivalResult.UNAVAILABLE,
            ready.copy(notifications = false) to ImmediateArrivalResult.NOTIFICATIONS_OFF)) {
            val check = ForegroundArrivalCheck(context,owner.lifecycle, { requests++; null },{ capabilities })
            assertEquals(result,check.check("new:generation",reminder))
        }
        assertEquals(0,requests)
    }
    @Test fun leavingForegroundCancelsInFlightProviderRequest() = runBlocking {
        val owner = owner(); val started = CompletableDeferred<Unit>(); var cancelled = false
        val check = ForegroundArrivalCheck(context,owner.lifecycle, { started.complete(Unit); try { awaitCancellation() } finally { cancelled = true } },{ ready })
        val result = async { check.check("new:generation",reminder) }
        started.await()
        instrumentation.runOnMainSync { owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP) }
        assertEquals(ImmediateArrivalResult.BACKGROUNDED,result.await())
        assertTrue(cancelled)
    }
    @Test fun timeoutCancelsTheSingleFixAndNullLocationIsGraceful() = runBlocking {
        val owner = owner(); var cancelled = false
        val check = ForegroundArrivalCheck(context,owner.lifecycle,{ try { awaitCancellation() } finally { cancelled = true } },{ ready },timeoutMillis = 30)
        assertEquals(ImmediateArrivalResult.UNAVAILABLE,check.check("new:generation",reminder))
        assertTrue(cancelled)
        assertEquals(ImmediateArrivalResult.UNAVAILABLE,ForegroundArrivalCheck(context,owner.lifecycle,{ null },{ ready }).check("new:generation",reminder))
    }
}

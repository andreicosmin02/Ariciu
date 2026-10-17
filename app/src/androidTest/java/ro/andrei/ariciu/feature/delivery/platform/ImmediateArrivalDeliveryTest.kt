package ro.andrei.ariciu.feature.delivery.platform

import android.Manifest
import android.app.NotificationManager
import android.os.SystemClock
import android.provider.Settings
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.workDataOf
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import ro.andrei.ariciu.feature.delivery.model.*
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import ro.andrei.ariciu.feature.reminders.data.*
import ro.andrei.ariciu.feature.reminders.model.*
import java.util.UUID

/** Emulator fixture injects evidence, never a claim of real GPS/geofence verification. */
class ImmediateArrivalDeliveryTest {
    @Test fun foregroundOccurrenceUsesRoomOutboxRealActionsAndRejectsRetryAfterSwipe() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation(); val context = instrumentation.targetContext
        for (permission in listOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION))
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName,permission)
        if (android.os.Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.grantRuntimePermission(context.packageName,Manifest.permission.POST_NOTIFICATIONS)
        val dao = ReminderDatabase.getInstance(context).reminderDao(); val repository = RoomReminderRepository(dao) { DeliveryWork.reconcile(context) }
        val manager = context.getSystemService(NotificationManager::class.java); val id = "immediate-${UUID.randomUUID()}"
        val reminder = Reminder(id,"Immediate arrival check",SelectedLocation(45.0,25.0,200),ReminderTransition.ENTER,ReminderRepeat.EVERY_VISIT,intensity = NotificationIntensity.URGENT)
        suspend fun row() = requireNotNull(dao.find(id))
        suspend fun settle() = withTimeout(20_000) { while (androidx.work.WorkManager.getInstance(context).getWorkInfosForUniqueWork("delivery-events").get().any { !it.state.isFinished }) delay(50) }
        val boot = Settings.Global.getInt(context.contentResolver,Settings.Global.BOOT_COUNT,0)
        try {
            val generation = repository.save(reminder); val requestId = "$id:$generation"
            val fix = ArrivalLocationFix(45.0,25.0,10.0,SystemClock.elapsedRealtime(),boot)
            assertFalse(DeliveryWork.immediateArrival(context,requestId,fix,{ false }))
            assertEquals(0,row().occurrence)
            assertTrue(DeliveryWork.immediateArrival(context,requestId,fix,{ true }))
            withTimeout(20_000) { while (row().notificationPending || manager.activeNotifications.none { it.tag == "reminder:$id" }) delay(50) }
            assertEquals(1,row().occurrence); assertEquals(AreaPosition.INSIDE.name,row().position)
            val notification = manager.activeNotifications.single { it.tag == "reminder:$id" }.notification
            assertEquals(AndroidReminderNotifications.channelId(NotificationIntensity.URGENT),notification.channelId)
            assertFalse(DeliveryWork.immediateArrival(context,requestId,fix,{ true }))
            notification.deleteIntent.send()
            withTimeout(20_000) { while (row().phase != DeliveryPhase.WAITING_NEXT_VISIT.name || manager.activeNotifications.any { it.tag == "reminder:$id" }) delay(50) }
            settle()
            assertEquals(DeliveryPhase.WAITING_NEXT_VISIT.name,row().phase)
            DeliveryWork.reconcile(context,force = true).result.get()
            withTimeout(20_000) { while (manager.activeNotifications.any { it.tag == "reminder:$id" }) delay(50) }
            assertFalse(DeliveryWork.immediateArrival(context,requestId,fix,{ true }))
            assertEquals(1,row().occurrence)
            DeliveryWork.command(context,workDataOf("command" to "transition","ids" to arrayOf(requestId),"transition" to "ENTER","elapsed" to SystemClock.elapsedRealtime(),"boot" to boot)).result.get(); settle()
            assertEquals(1,row().occurrence)
            repository.complete(id,1)
            assertFalse(DeliveryWork.immediateArrival(context,requestId,fix,{ true }))
            assertEquals(DeliveryPhase.COMPLETED.name,row().phase)
        } finally { repository.delete(id); manager.cancel("reminder:$id",1) }
    }
}

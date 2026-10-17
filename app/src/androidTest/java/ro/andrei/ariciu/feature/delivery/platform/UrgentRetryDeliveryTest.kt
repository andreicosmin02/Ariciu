package ro.andrei.ariciu.feature.delivery.platform

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.*
import org.junit.Test
import ro.andrei.ariciu.feature.delivery.model.*
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import ro.andrei.ariciu.feature.reminders.data.*
import ro.andrei.ariciu.feature.reminders.model.*
import java.util.UUID

/** Emulator only: real Room, PendingIntents and WorkManager; crossings/timer configuration are fixtures. */
class UrgentRetryDeliveryTest {
    private class Fixture(val delayMs: Long = URGENT_RE_ALERT_DELAY_MS) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val dao = ReminderDatabase.getInstance(context).reminderDao()
        val repository = RoomReminderRepository(dao) { DeliveryWork.reconcile(context) }
        val store = RoomDeliveryStore(ReminderDatabase.getInstance(context))
        val manager = context.getSystemService(NotificationManager::class.java)
        val work = WorkManager.getInstance(context)
        val id = "urgent-test-${UUID.randomUUID()}"
        val tag = "reminder:$id"
        lateinit var requestId: String
        val engine = DeliveryEngine(store,PlayServicesGeofenceBackend(context),AndroidReminderNotifications(context),delayMs)
        suspend fun row() = requireNotNull(dao.find(id))
        fun notification() = manager.activeNotifications.firstOrNull { it.tag == tag }?.notification
        suspend fun await(condition: suspend () -> Boolean) = withTimeout(40_000) { while (!condition()) delay(50) }
        suspend fun activeRetries() = work.getWorkInfosByTag(UrgentRetryWork.TAG).get().filter { !it.state.isFinished &&
            it.tags.any { t -> t.startsWith("urgent:$id:") } }
        suspend fun start(immediate: Boolean = false) {
            if (android.os.Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.grantRuntimePermission(context.packageName,Manifest.permission.POST_NOTIFICATIONS)
            val generation = repository.save(Reminder(id,"Urgent retry fixture",SelectedLocation(45.0,25.0,200),ReminderTransition.ENTER,ReminderRepeat.EVERY_VISIT,intensity = NotificationIntensity.URGENT))
            requestId = "$id:$generation"
            if (immediate) {
                for (permission in listOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION))
                    instrumentation.uiAutomation.grantRuntimePermission(context.packageName,permission)
                assertTrue(DeliveryWork.immediateArrival(context,requestId,ArrivalLocationFix(45.0,25.0,10.0,
                    android.os.SystemClock.elapsedRealtime(),android.provider.Settings.Global.getInt(context.contentResolver,android.provider.Settings.Global.BOOT_COUNT,0)),{ true }))
            } else DeliveryWork.runtimeMutex.withLock { engine.transition(setOf(requestId),ReminderTransition.ENTER,10,123); engine.dispatchNotifications(true) }
            await { notification() != null && !row().notificationPending }
        }
        suspend fun cleanup() {
            repository.delete(id); manager.cancel(tag,1)
            await { activeRetries().isEmpty() && notification() == null }
        }
    }

    @Test fun actualSwipeIntentSchedulesFiveMinutesOnceAndExplicitDoneResolvesHiddenAlert() = runBlocking<Unit> {
        val f = Fixture()
        try {
            f.start(); val first = requireNotNull(f.notification())
            assertEquals(0,first.flags and Notification.FLAG_INSISTENT)
            assertEquals(0,first.flags and Notification.FLAG_ONLY_ALERT_ONCE)
            assertEquals(0,first.extras.getInt("ariciu_attempt"))
            assertNotEquals(first.deleteIntent,first.actions.first { it.title == "Next time" }.actionIntent)
            val before = System.currentTimeMillis(); first.deleteIntent.send()
            f.await { f.row().urgentRetryAtMs > 0 && f.notification() == null && f.activeRetries().size == 1 }
            val deadline = f.row().urgentRetryAtMs
            assertTrue(deadline in (before + URGENT_RE_ALERT_DELAY_MS)..(System.currentTimeMillis() + URGENT_RE_ALERT_DELAY_MS))
            first.deleteIntent.send()
            DeliveryWork.command(f.context,workDataOf("command" to "dispatch")).result.get()
            f.await { f.work.getWorkInfosForUniqueWork("delivery-events").get().all { it.state.isFinished } }
            assertEquals(deadline,f.row().urgentRetryAtMs); assertEquals(1,f.activeRetries().size)
            DeliveryWork.reconcile(f.context,force = true,restoreNotifications = true).result.get()
            f.await { f.work.getWorkInfosForUniqueWork("delivery-reconcile").get().all { it.state.isFinished } }
            assertNull(f.notification()); assertEquals(deadline,f.row().urgentRetryAtMs)
            first.actions.first { it.title == "Done" }.actionIntent.send()
            f.await { f.row().phase == "COMPLETED" && f.activeRetries().isEmpty() && f.notification() == null }
            assertEquals(0L,f.row().urgentRetryAtMs)
        } finally { f.cleanup() }
    }

    @Test fun realDelayedOneTimeWorkRealertsTwiceWithoutNewOccurrencesOrVisibleRepetition() = runBlocking<Unit> {
        val f = Fixture(delayMs = 6_000) // Constructor configuration only; shipping default remains five minutes.
        try {
            f.start(immediate = true); val oldDismiss = requireNotNull(f.notification()).deleteIntent
            repeat(2) { attempt ->
                f.manager.cancel(f.tag,1); f.await { f.notification() == null }
                DeliveryWork.runtimeMutex.withLock { f.engine.action(f.requestId,1,OccurrenceAction.DISMISS,System.currentTimeMillis(),attempt) }
                DeliveryWork.reconcile(f.context).result.get()
                f.await { f.activeRetries().size == 1 }
                assertEquals(attempt,f.row().notificationAttempt)
                f.await { f.row().notificationAttempt == attempt + 1 && !f.row().notificationPending && f.notification()?.extras?.getInt("ariciu_attempt") == attempt + 1 }
                assertEquals(1,f.row().occurrence)
                assertEquals("Still pending. Choose Done or Next time.",requireNotNull(f.notification()).extras.getCharSequence("android.text").toString())
                assertEquals(0,requireNotNull(f.notification()).flags and Notification.FLAG_ONLY_ALERT_ONCE)
                assertEquals(0,requireNotNull(f.notification()).flags and Notification.FLAG_INSISTENT)
                f.await { f.activeRetries().isEmpty() }
                oldDismiss.send() // Old attempt cannot dismiss the re-alert, even within the same occurrence.
                DeliveryWork.command(f.context,workDataOf("command" to "dispatch")).result.get()
                f.await { f.work.getWorkInfosForUniqueWork("delivery-events").get().all { it.state.isFinished } }
                assertNotNull(f.notification()); assertEquals(0L,f.row().urgentRetryAtMs)
            }
            f.repository.nextTime(f.id,1)
            f.await { f.row().phase == "WAITING_NEXT_VISIT" && f.notification() == null && f.activeRetries().isEmpty() }
        } finally { f.cleanup() }
    }

    @Test fun nextTimeAfterSwipeAndOldRetryCannotReviveAnEditedOrCompletedReminder() = runBlocking<Unit> {
        val f = Fixture()
        try {
            f.start(); val first = requireNotNull(f.notification()); first.deleteIntent.send()
            f.await { f.row().urgentRetryAtMs > 0 && f.activeRetries().size == 1 }
            val pending = f.row(); val deadline = pending.urgentRetryAtMs
            f.repository.save(pending.toReminder().copy(title = "Edited urgent fixture"))
            f.await { f.activeRetries().size == 1 && f.activeRetries().single().tags.none { it == "urgent:${f.requestId}:1:0" } }
            assertEquals(deadline,f.row().urgentRetryAtMs)
            DeliveryWork.command(f.context,workDataOf("command" to "urgent_retry","request_id" to f.requestId,"occurrence" to 1,"notification_attempt" to 0)).result.get()
            f.await { f.work.getWorkInfosForUniqueWork("delivery-events").get().all { it.state.isFinished } }
            assertNull(f.notification()); assertEquals(deadline,f.row().urgentRetryAtMs)
            f.repository.nextTime(f.id,1)
            f.await { f.row().urgentRetryAtMs == 0L && f.activeRetries().isEmpty() }
            assertEquals("WAITING_NEXT_VISIT",f.row().phase)
            first.actions.first { it.title == "Done" }.actionIntent.send()
            f.repository.complete(f.id,1)
            DeliveryWork.reconcile(f.context,force = true,restoreNotifications = true).result.get()
            f.await { f.row().phase == "COMPLETED" && f.notification() == null && f.activeRetries().isEmpty() }
        } finally { f.cleanup() }
    }
    @Test fun upgradeRefreshRebindsTheOldSwipeIntentQuietlyWithoutQuietingFutureRetries() = runBlocking<Unit> {
        val f = Fixture()
        try {
            f.start()
            DeliveryWork.runtimeMutex.withLock {
                val legacy = requireNotNull(f.notification()).clone().apply {
                    extras.remove("ariciu_urgent_swipe_retry")
                    deleteIntent = actions.first { it.title == "Next time" }.actionIntent
                    flags = flags or Notification.FLAG_ONLY_ALERT_ONCE
                }
                f.manager.notify(f.tag,1,legacy)
                f.await { f.notification()?.extras?.getBoolean("ariciu_urgent_swipe_retry",false) == false }
                val record = f.store.all().single { it.reminder.id == f.id }
                val notifications = AndroidReminderNotifications(f.context)
                assertTrue(notifications.requiresRefresh(record))
                f.engine.dispatchNotifications(true)
                f.await { f.notification()?.extras?.getBoolean("ariciu_urgent_swipe_retry",false) == true }
                val rebound = requireNotNull(f.notification())
                assertNotEquals(rebound.deleteIntent,rebound.actions.first { it.title == "Next time" }.actionIntent)
                assertTrue(rebound.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)
                assertFalse(f.row().quiet)
                assertFalse(notifications.requiresRefresh(f.store.all().single { it.reminder.id == f.id }))
            }
        } finally { f.cleanup() }
    }
}

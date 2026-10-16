package ro.andrei.ariciu.feature.delivery.platform

import android.Manifest
import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import ro.andrei.ariciu.feature.delivery.model.*
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import ro.andrei.ariciu.feature.reminders.data.*
import ro.andrei.ariciu.feature.reminders.model.*
import java.util.UUID

/** Injects a crossing at the worker boundary; does not simulate Play services positioning. */
class NotificationDeliveryTest {
    @Test fun intensityChannelsAndAlarmStopSnoozeUsePersistedTokens() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        if (android.os.Build.VERSION.SDK_INT >= 33) InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val notifications = AndroidReminderNotifications(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val repository = RoomReminderRepository(ReminderDatabase.getInstance(context).reminderDao())
        val store = RoomDeliveryStore(ReminderDatabase.getInstance(context))
        val id = "intensity-${UUID.randomUUID()}"
        try {
            NotificationIntensity.entries.forEach { intensity ->
                val state = DeliveryState().onTransition(ReminderTransition.ENTER, ReminderTransition.ENTER, ReminderRepeat.ONCE, 1, 1)
                repository.save(Reminder(id, "Intensity test", SelectedLocation(45.0,25.0,200), ReminderTransition.ENTER,
                    ReminderRepeat.ONCE, intensity = intensity))
                val record = store.all().single { it.reminder.id == id }
                store.put(record.copy(reminder = record.reminder.copy(delivery = state)))
                assertTrue(notifications.post(record.copy(reminder = record.reminder.copy(delivery = state))))
                withTimeout(20_000) { while (manager.activeNotifications.firstOrNull { it.tag == "reminder:$id" }?.notification?.let {
                    it.channelId == AndroidReminderNotifications.channelId(intensity) &&
                        it.extras.getString("ariciu_occurrence") == "${record.generation}:1" } != true) delay(100) }
                val n = manager.activeNotifications.single { it.tag == "reminder:$id" }.notification
                assertEquals(AndroidReminderNotifications.channelId(intensity), n.channelId)
                assertEquals(intensity in setOf(NotificationIntensity.URGENT, NotificationIntensity.ALARM),
                    n.flags and android.app.Notification.FLAG_ONGOING_EVENT != 0)
                if (intensity == NotificationIntensity.SILENT) {
                    val channel = manager.getNotificationChannel(n.channelId)
                    assertEquals(NotificationManager.IMPORTANCE_LOW, channel.importance)
                    assertNull(channel.sound)
                    assertFalse(channel.shouldVibrate())
                }
                if (intensity == NotificationIntensity.ALARM) {
                    assertTrue(n.flags and android.app.Notification.FLAG_INSISTENT != 0)
                    assertEquals(listOf("Stop", "Done", "Snooze 10 min"), n.actions.map { it.title.toString() })
                    n.actions[0].actionIntent.send()
                    withTimeout(20_000) { while (!store.all().single { it.reminder.id == id }.reminder.delivery.quiet) delay(100) }
                    withTimeout(20_000) { while (manager.activeNotifications.single { it.tag == "reminder:$id" }.notification.actions.size != 2) delay(100) }
                    val quiet = manager.activeNotifications.single { it.tag == "reminder:$id" }.notification
                    assertEquals(0, quiet.flags and android.app.Notification.FLAG_INSISTENT)
                    quiet.actions.last().actionIntent.send()
                    withTimeout(20_000) { while (store.all().single { it.reminder.id == id }.reminder.delivery.snoozeUntilMs == 0L) delay(100) }
                    assertFalse(store.all().single { it.reminder.id == id }.reminder.delivery.notificationVisible)
                }
                manager.cancel("reminder:$id", 1)
            }
        } finally { repository.delete(id); manager.cancel("reminder:$id", 1) }
    }

    @Test fun persistentWorkerPostsNotificationAndReceiverHandlesSwipeNextTimeAndDone() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        val dao = ReminderDatabase.getInstance(context).reminderDao()
        val repository = RoomReminderRepository(dao)
        val id = "notification-test-${UUID.randomUUID()}"
        val manager = context.getSystemService(NotificationManager::class.java)
        AndroidReminderNotifications(context)
        suspend fun record() = dao.getAll().single { it.id == id }
        suspend fun awaitCondition(predicate: suspend () -> Boolean) = withTimeout(20_000) {
            while (!predicate()) delay(100)
        }
        suspend fun crossing(transition: String, time: Long) {
            val row = record()
            DeliveryWork.command(context, workDataOf("command" to "transition", "transition" to transition,
                "ids" to arrayOf("$id:${row.generation}"), "elapsed" to time, "boot" to 123)).result.get()
        }
        fun notification() = manager.activeNotifications.firstOrNull { it.tag == "reminder:$id" }
        try {
            repository.save(Reminder(id, "Test arrival", SelectedLocation(45.0,25.0,200), ReminderTransition.ENTER, ReminderRepeat.ONCE))
            crossing("ENTER", 10)
            awaitCondition { notification() != null && !record().notificationPending }
            val first = requireNotNull(notification()).notification
            assertEquals("Test arrival", first.extras.getCharSequence("android.title").toString())
            assertEquals(listOf("Done", "Next time"), first.actions.map { it.title.toString() })
            assertTrue(first.contentIntent.isActivity)
            crossing("ENTER", 10)
            awaitCondition { record().occurrence == 1 }
            assertEquals(1, manager.activeNotifications.count { it.tag == "reminder:$id" })

            // Android sends this exact PendingIntent when the user swipes the notification away.
            first.deleteIntent.send()
            awaitCondition { record().phase == DeliveryPhase.WAITING_NEXT_VISIT.name && notification() == null }
            crossing("EXIT", 20); crossing("ENTER", 30)
            awaitCondition { record().occurrence == 2 && notification() != null }
            requireNotNull(notification()).notification.actions.first { it.title == "Next time" }.actionIntent.send()
            awaitCondition { record().phase == DeliveryPhase.WAITING_NEXT_VISIT.name && notification() == null }
            crossing("EXIT", 40); crossing("ENTER", 50)
            awaitCondition { record().occurrence == 3 && notification() != null }
            requireNotNull(notification()).notification.actions.first { it.title == "Done" }.actionIntent.send()
            awaitCondition { record().phase == DeliveryPhase.COMPLETED.name && notification() == null }
            crossing("EXIT", 60); crossing("ENTER", 70)
            assertEquals(3, record().occurrence)
        } finally {
            repository.delete(id)
            manager.cancel("reminder:$id", 1)
        }
    }
}

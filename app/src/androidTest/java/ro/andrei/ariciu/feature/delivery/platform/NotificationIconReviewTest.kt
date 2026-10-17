package ro.andrei.ariciu.feature.delivery.platform

import android.Manifest
import android.app.NotificationManager
import android.app.UiModeManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import ro.andrei.ariciu.R
import ro.andrei.ariciu.feature.delivery.model.*
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import ro.andrei.ariciu.feature.reminders.data.*
import ro.andrei.ariciu.feature.reminders.model.*
import java.io.File
import java.util.UUID

/** Run only on a disposable emulator, never automatically on the owner's everyday phone. */
class NotificationIconReviewTest {
    @Test fun foxSmallIconHasTransparentBackgroundAndRendersInBothSystemShades() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val drawable = requireNotNull(context.getDrawable(R.drawable.ic_reminder_notification))
        val bitmap = Bitmap.createBitmap(24,24,Bitmap.Config.ARGB_8888)
        drawable.setBounds(0,0,24,24); drawable.draw(Canvas(bitmap))
        assertEquals(0,bitmap.getPixel(0,0))
        assertTrue((0..23).sumOf { y -> (0..23).count { x -> android.graphics.Color.alpha(bitmap.getPixel(x,y)) > 128 } } > 80)
        bitmap.recycle()
        if (android.os.Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.grantRuntimePermission(context.packageName,Manifest.permission.POST_NOTIFICATIONS)
        val manager = context.getSystemService(NotificationManager::class.java)
        val dao = ReminderDatabase.getInstance(context).reminderDao()
        val repository = RoomReminderRepository(dao)
        val id = "icon-review-${UUID.randomUUID()}"
        val oldMode = context.getSystemService(UiModeManager::class.java).nightMode
        fun shell(command: String) { android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes() } }
        try {
            repository.save(Reminder(id,"Ariciu fox icon check",SelectedLocation(45.0,25.0,200),ReminderTransition.ENTER,ReminderRepeat.ONCE,intensity = NotificationIntensity.URGENT))
            val record = RoomDeliveryStore(ReminderDatabase.getInstance(context)).all().single { it.reminder.id == id }
            val visible = record.copy(reminder = record.reminder.copy(delivery = DeliveryState().onTransition(ReminderTransition.ENTER,ReminderTransition.ENTER,ReminderRepeat.ONCE,1,1).copy(notificationPending = false)))
            RoomDeliveryStore(ReminderDatabase.getInstance(context)).put(visible)
            assertTrue(AndroidReminderNotifications(context).post(visible))
            withTimeout(10_000) { while (manager.activeNotifications.none { it.tag == "reminder:$id" }) delay(50) }
            assertEquals(R.drawable.ic_reminder_notification,manager.activeNotifications.single { it.tag == "reminder:$id" }.notification.smallIcon.resId)
            for ((name,mode) in listOf("notification-dark" to "yes","notification-light" to "no")) {
                shell("cmd uimode night $mode")
                delay(500) // SystemUI reconfigures asynchronously after a theme change.
                manager.cancel("reminder:$id",1)
                withTimeout(10_000) { while (manager.activeNotifications.any { it.tag == "reminder:$id" }) delay(50) }
                assertTrue(AndroidReminderNotifications(context).post(visible))
                withTimeout(10_000) { while (manager.activeNotifications.none { it.tag == "reminder:$id" }) delay(50) }
                shell("cmd statusbar collapse")
                delay(500)
                fun capture(name: String) {
                    val file = File(context.filesDir,"review2/$name.png").apply { parentFile!!.mkdirs() }
                    instrumentation.uiAutomation.takeScreenshot().let { shot -> file.outputStream().use { shot.compress(Bitmap.CompressFormat.PNG,100,it) };shot.recycle() }
                }
                capture(name.replace("notification-","notification-status-"))
                shell("cmd statusbar expand-notifications")
                instrumentation.uiAutomation.waitForIdle(500,5_000)
                delay(500) // Capture only after actual shade content is visible and animation settles.
                capture(name)
                shell("cmd statusbar collapse")
            }
        } finally {
            shell("cmd statusbar collapse")
            shell("cmd uimode night ${when (oldMode) { UiModeManager.MODE_NIGHT_YES -> "yes"; UiModeManager.MODE_NIGHT_AUTO -> "auto"; else -> "no" }}")
            repository.delete(id);manager.cancel("reminder:$id",1)
        }
    }
}

package ro.andrei.ariciu.feature.reminders.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import ro.andrei.ariciu.feature.reminders.model.*
import java.util.UUID

class ReminderDatabaseTest {
    @Test fun completionPreservesRowInvalidatesStaleEngineWritesAndClearsSnooze() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, ReminderDatabase::class.java).build()
        try {
            var changes = 0
            val repo = RoomReminderRepository(db.reminderDao()) { changes++ }
            repo.save(Reminder("done", "Bread", SelectedLocation(45.0,25.0,200), ReminderTransition.ENTER, ReminderRepeat.ONCE))
            val store = RoomDeliveryStore(db)
            val old = store.all().single()
            repo.complete("done", 99) // A stale card cannot complete a newer occurrence.
            assertTrue(store.all().single().reminder.delivery.active)
            repo.complete("done", 0)
            store.put(old) // An interrupted worker must not resurrect the reminder.
            val completed = store.all().single()
            assertFalse(completed.reminder.delivery.active)
            assertFalse(completed.reminder.delivery.notificationVisible)
            assertFalse(completed.reminder.delivery.notificationPending)
            assertEquals(0L, completed.reminder.delivery.snoozeUntilMs)
            assertNotEquals(old.generation, completed.generation)
            assertEquals("Bread", completed.reminder.title)
            assertTrue(changes >= 2)
        } finally { db.close() }
    }

    @Test fun remindersSurviveDatabaseReopenAndCanBeUpdatedAndDeleted() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "test-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, ReminderDatabase::class.java, name).build()
        var db = open()
        try {
            val reminder = Reminder("id", "Bread", SelectedLocation(45.0, 25.0, 200),
                ReminderTransition.EXIT, ReminderRepeat.EVERY_VISIT)
            RoomReminderRepository(db.reminderDao()).save(reminder)
            db.close()
            db = open()
            val repository = RoomReminderRepository(db.reminderDao())
            assertEquals(listOf(reminder), repository.observeReminders().first())
            repository.save(reminder.copy(title = "Milk"))
            assertEquals(listOf(reminder.copy(title = "Milk")), repository.observeReminders().first())
            repository.delete("id")
            assertTrue(repository.observeReminders().first().isEmpty())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}

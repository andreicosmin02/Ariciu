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

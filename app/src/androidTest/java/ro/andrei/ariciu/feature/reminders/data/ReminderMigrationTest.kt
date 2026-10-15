package ro.andrei.ariciu.feature.reminders.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ro.andrei.ariciu.feature.delivery.model.*
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import ro.andrei.ariciu.feature.reminders.model.*
import java.util.UUID

class ReminderMigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), ReminderDatabase::class.java)

    @Test fun migrationPreservesVersionOneRemindersAndValidatesSchema() = runBlocking<Unit> {
        val name = "migration-${UUID.randomUUID()}"
        helper.createDatabase(name, 1).use {
            it.execSQL("INSERT INTO reminders VALUES ('old','Bread',45.0,25.0,300,'EXIT','EVERY_VISIT')")
        }
        helper.runMigrationsAndValidate(name, 2, true, ReminderDatabase.MIGRATION_1_2).use {
            it.query("SELECT * FROM reminders WHERE id='old'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Bread", cursor.getString(cursor.getColumnIndexOrThrow("title")))
                assertEquals("EXIT", cursor.getString(cursor.getColumnIndexOrThrow("transition")))
                assertEquals("EVERY_VISIT", cursor.getString(cursor.getColumnIndexOrThrow("repeat")))
                assertEquals("WAITING", cursor.getString(cursor.getColumnIndexOrThrow("phase")))
                assertFalse(cursor.getString(cursor.getColumnIndexOrThrow("generation")).isBlank())
            }
        }
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(name)
    }

    @Test fun editingCannotRearmATriggeredOnceReminderOrUndoDone() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, ReminderDatabase::class.java).build()
        try {
            val repository = RoomReminderRepository(db.reminderDao())
            val store = RoomDeliveryStore(db)
            val original = Reminder("id", "Bread", SelectedLocation(45.0,25.0,200), ReminderTransition.ENTER, ReminderRepeat.ONCE)
            repository.save(original)
            val record = store.all().single()
            val triggered = record.reminder.delivery.onTransition(ReminderTransition.ENTER, ReminderTransition.ENTER, ReminderRepeat.ONCE, 10, 1)
            store.put(record.copy(reminder = record.reminder.copy(delivery = triggered)))
            repository.save(original.copy(title = "Milk"))
            val edited = store.all().single()
            assertEquals(DeliveryPhase.TRIGGERED, edited.reminder.delivery.phase)
            assertEquals(1, edited.reminder.delivery.occurrence)
            assertNotEquals(record.generation, edited.generation)
            val done = edited.reminder.delivery.onAction(OccurrenceAction.DONE, 1, ReminderTransition.ENTER)
            store.put(edited.copy(reminder = edited.reminder.copy(delivery = done)))
            repository.save(original)
            assertEquals(DeliveryPhase.COMPLETED, store.all().single().reminder.delivery.phase)
        } finally { db.close() }
    }

    @Test fun staleEngineWritesCannotResurrectDeletedRowsOrOverwriteAnEdit() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, ReminderDatabase::class.java).build()
        try {
            val repository = RoomReminderRepository(db.reminderDao())
            val store = RoomDeliveryStore(db)
            val reminder = Reminder("id", "Bread", SelectedLocation(45.0,25.0,200), ReminderTransition.ENTER, ReminderRepeat.ONCE)
            repository.save(reminder)
            val stale = store.all().single()
            repository.save(reminder.copy(title = "Milk"))
            store.put(stale.copy(reminder = stale.reminder.copy(registration = RegistrationState.REGISTERED)))
            assertEquals("Milk", store.all().single().reminder.title)
            assertEquals(RegistrationState.PENDING, store.all().single().reminder.registration)
            repository.delete("id")
            store.put(stale)
            assertTrue(store.all().isEmpty())
        } finally { db.close() }
    }
}

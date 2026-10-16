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
        helper.runMigrationsAndValidate(name, 4, true, ReminderDatabase.MIGRATION_1_2, ReminderDatabase.MIGRATION_2_3, ReminderDatabase.MIGRATION_3_4).use {
            it.query("SELECT * FROM reminders WHERE id='old'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Bread", cursor.getString(cursor.getColumnIndexOrThrow("title")))
                assertEquals("EXIT", cursor.getString(cursor.getColumnIndexOrThrow("transition")))
                assertEquals("EVERY_VISIT", cursor.getString(cursor.getColumnIndexOrThrow("repeat")))
                assertEquals("NORMAL", cursor.getString(cursor.getColumnIndexOrThrow("intensity")))
                assertEquals(0L, cursor.getLong(cursor.getColumnIndexOrThrow("snoozeUntilMs")))
                assertEquals("WAITING", cursor.getString(cursor.getColumnIndexOrThrow("phase")))
                assertFalse(cursor.getString(cursor.getColumnIndexOrThrow("generation")).isBlank())
            }
        }
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(name)
    }

    @Test fun migrationFromDeliveryBaselineKeepsTriggeredOccurrenceAndRegistration() = runBlocking<Unit> {
        val name = "migration-v2-${UUID.randomUUID()}"
        helper.createDatabase(name, 2).use {
            it.execSQL("INSERT INTO reminders (id,title,latitude,longitude,radiusMeters,transition,repeat,generation,phase,position,occurrence,notificationVisible,notificationPending,registration) VALUES ('old','Milk',45,25,200,'ENTER','ONCE','g1','TRIGGERED','INSIDE',7,1,0,'REGISTERED')")
            it.execSQL("INSERT INTO registered_geofences VALUES ('old:g1')")
        }
        helper.runMigrationsAndValidate(name, 4, true, ReminderDatabase.MIGRATION_2_3, ReminderDatabase.MIGRATION_3_4).use {
            it.query("SELECT * FROM reminders").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(7, c.getInt(c.getColumnIndexOrThrow("occurrence")))
                assertEquals("TRIGGERED", c.getString(c.getColumnIndexOrThrow("phase")))
                assertEquals("g1", c.getString(c.getColumnIndexOrThrow("generation")))
                assertEquals("NORMAL", c.getString(c.getColumnIndexOrThrow("intensity")))
                assertEquals("", c.getString(c.getColumnIndexOrThrow("chainAreas")))
            }
            it.query("SELECT COUNT(*) FROM registered_geofences").use { c -> assertTrue(c.moveToFirst()); assertEquals(1, c.getInt(0)) }
        }
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(name)
    }

    @Test fun chainVisitAndSnoozeSurviveStoreRecreationAndTitleEdit() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "chain-${UUID.randomUUID()}"
        fun open() = Room.databaseBuilder(context, ReminderDatabase::class.java, name).build()
        var db = open()
        try {
            val repository = RoomReminderRepository(db.reminderDao())
            val chain = Reminder("id", "Bread", SelectedLocation(45.0,25.0,200), ReminderTransition.ENTER, ReminderRepeat.ONCE,
                intensity = NotificationIntensity.ALARM, chainName = "Kaufland", chainAreas = listOf(ChainArea("n1",45.0,25.0),ChainArea("n2",45.1,25.1)))
            repository.save(chain)
            var store = RoomDeliveryStore(db)
            val record = store.all().single()
            val crossed = record.reminder.crossChain("n1",ReminderTransition.ENTER,10,1)
            val snoozed = crossed.copy(delivery = crossed.delivery.onAction(OccurrenceAction.SNOOZE,1,ReminderTransition.ENTER,1000))
            store.put(record.copy(reminder = snoozed))
            db.close(); db = open(); store = RoomDeliveryStore(db)
            assertEquals(snoozed, store.all().single().reminder)
            RoomReminderRepository(db.reminderDao()).save(chain.copy(title = "Milk"))
            val edited = store.all().single()
            assertEquals(601000L, edited.reminder.delivery.snoozeUntilMs)
            assertEquals(AreaPosition.INSIDE, edited.reminder.chainAreas.first().position)
            assertNotEquals(record.generation, edited.generation)
        } finally { db.close(); context.deleteDatabase(name) }
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

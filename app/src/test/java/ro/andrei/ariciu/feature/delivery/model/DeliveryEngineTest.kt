package ro.andrei.ariciu.feature.delivery.model

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import ro.andrei.ariciu.feature.reminders.model.*

class DeliveryEngineTest {
    private val ready = DeliveryCapabilities(true, true, true, true, true)
    private fun record(id: String = "id", generation: String = "v1", transition: ReminderTransition = ReminderTransition.ENTER,
        repeat: ReminderRepeat = ReminderRepeat.ONCE) = DeliveryRecord(Reminder(id, "Bread", SelectedLocation(45.0, 25.0, 200), transition, repeat), generation)

    @Test fun repeatedReconciliationRegistersOnceAndEditingRemovesOldGeneration() = runBlocking {
        val f = Fixture(record())
        f.engine.reconcile(ready); f.engine.reconcile(ready)
        assertEquals(1, f.backend.addCalls)
        assertEquals(setOf("id:v1"), f.backend.registered)
        f.store.records["id"] = record(generation = "v2")
        f.engine.reconcile(ready)
        assertEquals(setOf("id:v2"), f.backend.registered)
        f.engine.transition(setOf("id:v1"), ReminderTransition.ENTER, 10, 1)
        assertEquals(0, f.store.records.getValue("id").reminder.delivery.occurrence)
    }

    @Test fun deletionAndCompletionRemoveRegistrationsAndNotifications() = runBlocking {
        val f = Fixture(record())
        f.engine.reconcile(ready)
        f.engine.transition(setOf("id:v1"), ReminderTransition.ENTER, 10, 1)
        f.engine.dispatchNotifications(true)
        f.engine.action("id:v1", 1, OccurrenceAction.DONE)
        f.engine.dispatchNotifications(true); f.engine.reconcile(ready)
        assertTrue(f.backend.registered.isEmpty())
        assertTrue(f.notifications.visible.isEmpty())
        f.store.records.clear(); f.engine.reconcile(ready)
        assertTrue(f.store.tracked.isEmpty())
    }

    @Test fun duplicatesAreNotRepostedAndStaleActionsCannotDismissNewOccurrence() = runBlocking {
        val f = Fixture(record(repeat = ReminderRepeat.EVERY_VISIT))
        f.engine.transition(setOf("id:v1"), ReminderTransition.ENTER, 10, 1)
        f.engine.dispatchNotifications(true)
        f.engine.transition(setOf("id:v1"), ReminderTransition.ENTER, 10, 1)
        f.engine.dispatchNotifications(true)
        assertEquals(1, f.notifications.posts)
        f.engine.transition(setOf("id:v1"), ReminderTransition.EXIT, 20, 1)
        f.engine.transition(setOf("id:v1"), ReminderTransition.ENTER, 30, 1)
        f.engine.action("id:v1", 1, OccurrenceAction.NEXT_TIME)
        f.engine.dispatchNotifications(true)
        assertEquals(2, f.notifications.posts)
        assertEquals(2, f.notifications.visible.getValue("id"))
    }

    @Test fun deniedCapabilitiesBlockRegistrationAndPendingNotificationsRecover() = runBlocking {
        for (denied in listOf(ready.copy(preciseLocation = false), ready.copy(backgroundLocation = false),
            ready.copy(locationEnabled = false), ready.copy(playServices = false), ready.copy(notifications = false))) {
            val f = Fixture(record())
            f.engine.reconcile(denied)
            assertEquals(0, f.backend.addCalls)
            assertEquals(RegistrationState.BLOCKED, f.store.records.getValue("id").reminder.registration)
            assertNotNull(denied.reason)
        }
        val f = Fixture(record())
        f.engine.transition(setOf("id:v1"), ReminderTransition.ENTER, 10, 1)
        f.engine.dispatchNotifications(false)
        assertTrue(f.store.records.getValue("id").reminder.delivery.notificationPending)
        f.engine.dispatchNotifications(true)
        assertEquals(1, f.notifications.posts)
    }

    @Test fun registrationFailureIsJournaledAndRecoveryIsIdempotent() = runBlocking {
        val f = Fixture(record())
        f.backend.fail = true
        try { f.engine.reconcile(ready); fail("Expected registration failure") } catch (_: IllegalStateException) { }
        assertEquals(RegistrationState.FAILED, f.store.records.getValue("id").reminder.registration)
        assertEquals(setOf("id:v1"), f.store.tracked)
        f.backend.fail = false
        f.engine.reconcile(ready)
        assertEquals(RegistrationState.REGISTERED, f.store.records.getValue("id").reminder.registration)
        f.backend.registered.clear() // Device reboot loses platform registrations.
        f.engine.reconcile(ready, force = true)
        assertEquals(setOf("id:v1"), f.backend.registered)
    }

    @Test fun failedRemovalIsRetriedWithoutForgettingDeletedFence() = runBlocking {
        val f = Fixture(record())
        f.engine.reconcile(ready)
        f.store.records.clear()
        f.backend.fail = true
        try { f.engine.reconcile(ready) } catch (_: IllegalStateException) { }
        assertEquals(setOf("id:v1"), f.store.tracked)
        f.backend.fail = false; f.engine.reconcile(ready)
        assertTrue(f.store.tracked.isEmpty()); assertTrue(f.backend.registered.isEmpty())
    }

    @Test fun atMost100AreasAreRegisteredAndOverflowPromotesAfterDeletion() = runBlocking {
        val f = Fixture(*((0..100).map { record("id%03d".format(it)) }.toTypedArray()))
        f.engine.reconcile(ready)
        assertEquals(100, f.backend.registered.size)
        assertEquals(RegistrationState.LIMIT_REACHED, f.store.records.getValue("id100").reminder.registration)
        f.store.records.remove("id000"); f.engine.reconcile(ready)
        assertEquals(100, f.backend.registered.size)
        assertEquals(RegistrationState.REGISTERED, f.store.records.getValue("id100").reminder.registration)
    }

    @Test fun repeatedNextTimeActionIsIdempotentAndEditedGenerationRejectsOldAction() = runBlocking {
        val f = Fixture(record(transition = ReminderTransition.EXIT))
        f.engine.transition(setOf("id:v1"), ReminderTransition.EXIT, 10, 1)
        f.engine.action("id:v1", 1, OccurrenceAction.NEXT_TIME)
        f.engine.action("id:v1", 1, OccurrenceAction.NEXT_TIME)
        f.engine.transition(setOf("id:v1"), ReminderTransition.EXIT, 20, 1)
        assertEquals(1, f.store.records.getValue("id").reminder.delivery.occurrence)
        f.engine.transition(setOf("id:v1"), ReminderTransition.ENTER, 30, 1)
        f.engine.transition(setOf("id:v1"), ReminderTransition.EXIT, 40, 1)
        assertEquals(2, f.store.records.getValue("id").reminder.delivery.occurrence)
        f.store.records["id"] = record(generation = "v2")
        f.engine.action("id:v1", 2, OccurrenceAction.DONE)
        assertTrue(f.store.records.getValue("id").reminder.delivery.active)
    }

    private class Fixture(vararg records: DeliveryRecord) {
        val store = FakeStore(records.toList())
        val backend = FakeBackend()
        val notifications = FakeNotifications()
        val engine = DeliveryEngine(store, backend, notifications)
    }
    private class FakeStore(initial: List<DeliveryRecord>) : DeliveryStore {
        val records = initial.associateBy { it.reminder.id }.toMutableMap()
        val tracked = mutableSetOf<String>()
        override suspend fun all() = records.values.toList()
        override suspend fun put(record: DeliveryRecord) { records[record.reminder.id] = record }
        override suspend fun registeredIds() = tracked.toSet()
        override suspend fun track(ids: Set<String>) { tracked.addAll(ids) }
        override suspend fun untrack(ids: Set<String>) { tracked.removeAll(ids) }
        override suspend fun <T> transaction(block: suspend () -> T) = block()
    }
    private class FakeBackend : GeofenceBackend {
        val registered = mutableSetOf<String>()
        var addCalls = 0
        var fail = false
        override suspend fun add(records: List<DeliveryRecord>) {
            if (fail) error("Play services unavailable")
            addCalls++; registered.addAll(records.map { it.requestId })
        }
        override suspend fun remove(requestIds: Set<String>) {
            if (fail) error("Removal unavailable")
            registered.removeAll(requestIds)
        }
    }
    private class FakeNotifications : ReminderNotifications {
        var posts = 0
        val visible = mutableMapOf<String, Int>()
        override fun post(record: DeliveryRecord): Boolean {
            posts++; visible[record.reminder.id] = record.reminder.delivery.occurrence; return true
        }
        override fun cancelExcept(reminderIds: Set<String>) { visible.keys.retainAll(reminderIds) }
    }
}

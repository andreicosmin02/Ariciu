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

    @Test fun explicitCardCompletionCleansEveryChainAreaAndVisibleNotification() = runBlocking {
        val original = record().let { it.copy(reminder = it.reminder.copy(chainName = "Kaufland",
            chainAreas = listOf(ChainArea("n1", 45.0, 25.0), ChainArea("n2", 45.01, 25.01)))) }
        val f = Fixture(original)
        f.engine.reconcile(ready)
        f.engine.transition(setOf("id:v1:n1"), ReminderTransition.ENTER, 10, 1)
        f.engine.dispatchNotifications(true)
        val current = f.store.records.getValue("id")
        f.store.records["id"] = current.copy(generation = "completed", reminder = current.reminder.copy(delivery = current.reminder.delivery.completed()))
        f.engine.dispatchNotifications(true)
        f.engine.reconcile(ready)
        assertTrue(f.notifications.visible.isEmpty())
        assertTrue(f.backend.registered.isEmpty())
        assertTrue(f.store.tracked.isEmpty())
        f.engine.transition(setOf("id:v1:n2"), ReminderTransition.ENTER, 20, 1)
        f.engine.action("id:v1", 1, OccurrenceAction.SNOOZE, 20)
        assertFalse(f.store.records.getValue("id").reminder.delivery.active)
    }

    @Test fun chainEventsKeepOneNotificationAndDoneRemovesEveryStore() = runBlocking {
        val chain = record().copy(reminder = record().reminder.copy(chainName = "Kaufland",
            chainAreas = listOf(ChainArea("n1",45.0,25.0), ChainArea("n2",45.1,25.1)), repeat = ReminderRepeat.EVERY_VISIT))
        val f = Fixture(chain)
        f.engine.reconcile(ready)
        assertEquals(setOf("id:v1:n1", "id:v1:n2"), f.backend.registered)
        f.engine.transition(setOf("id:v1:n1", "id:v1:n2"), ReminderTransition.ENTER, 10, 1)
        f.engine.dispatchNotifications(true)
        assertEquals(1, f.notifications.posts)
        f.engine.action("id:v1",1,OccurrenceAction.DONE)
        f.engine.reconcile(ready); f.engine.dispatchNotifications(true)
        assertTrue(f.backend.registered.isEmpty())
        assertTrue(f.notifications.visible.isEmpty())
    }

    @Test fun chainCapacityIsPartialAndPromotesWhenCapacityReturns() = runBlocking {
        val chain = record("zchain").copy(reminder = record("zchain").reminder.copy(chainName = "Kaufland",
            chainAreas = listOf(ChainArea("n1",45.0,25.0), ChainArea("n2",45.1,25.1))))
        val f = Fixture(*((0..98).map { record("r$it") } + chain).toTypedArray())
        f.engine.reconcile(ready)
        assertEquals(100, f.backend.registered.size)
        assertEquals(RegistrationState.PARTIAL, f.store.records.getValue("zchain").reminder.registration)
        f.store.records.remove("r0"); f.engine.reconcile(ready)
        assertEquals(100, f.backend.registered.size)
        assertEquals(RegistrationState.REGISTERED, f.store.records.getValue("zchain").reminder.registration)
    }

    @Test fun blockedIntensityDoesNotBlockOtherReminderChannels() = runBlocking {
        val f = Fixture(record("normal"), record("silent").let { it.copy(reminder = it.reminder.copy(intensity = NotificationIntensity.SILENT)) })
        f.engine.reconcile(ready.copy(blockedIntensities = setOf(NotificationIntensity.NORMAL)))
        assertEquals(setOf("silent:v1"), f.backend.registered)
        assertEquals(RegistrationState.BLOCKED, f.store.records.getValue("normal").reminder.registration)
    }

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

    @Test fun optedInArrivalIsARealOccurrenceAndNeedsOppositeCrossingBeforeNextVisit() = runBlocking {
        val f = Fixture(record(repeat = ReminderRepeat.EVERY_VISIT))
        val fix = ArrivalLocationFix(45.0,25.0,10.0,1_000,1)
        assertTrue(f.engine.immediateArrival("id:v1", fix,2_000,1))
        val triggered = f.store.records.getValue("id").reminder.delivery
        assertEquals(DeliveryPhase.TRIGGERED, triggered.phase)
        assertEquals(AreaPosition.INSIDE, triggered.position)
        assertEquals(1,triggered.occurrence)
        assertTrue(triggered.notificationPending)
        f.engine.dispatchNotifications(true)
        f.engine.reconcile(ready); f.engine.dispatchNotifications(true)
        assertEquals(1,f.notifications.posts)
        f.engine.action("id:v1",1,OccurrenceAction.NEXT_TIME)
        assertFalse(f.engine.immediateArrival("id:v1",fix,2_000,1))
        f.engine.transition(setOf("id:v1"),ReminderTransition.ENTER,3_000,1)
        f.engine.dispatchNotifications(true)
        assertEquals(1,f.notifications.posts)
        f.engine.transition(setOf("id:v1"),ReminderTransition.EXIT,4_000,1)
        f.engine.transition(setOf("id:v1"),ReminderTransition.ENTER,5_000,1)
        f.engine.dispatchNotifications(true)
        assertEquals(2,f.notifications.posts)
        assertFalse(f.engine.immediateArrival("id:v1",fix.copy(elapsedMs = 6_000),6_000,1))
    }

    @Test fun immediateArrivalRejectsEditedDeletedCompletedChainExitAndOldFix() = runBlocking {
        val original = record()
        val fix = ArrivalLocationFix(45.0,25.0,10.0,1_000,1)
        for (invalid in listOf(record(generation = "v2"), record(transition = ReminderTransition.EXIT),
            original.copy(reminder = original.reminder.copy(delivery = DeliveryState().completed())),
            original.copy(reminder = original.reminder.copy(chainName = "Kaufland", chainAreas = listOf(ChainArea("n1",45.0,25.0)))))) {
            val f = Fixture(invalid)
            assertFalse(f.engine.immediateArrival("id:v1",fix,2_000,1))
            assertEquals(invalid, f.store.records.getValue("id"))
        }
        assertFalse(Fixture().engine.immediateArrival("id:v1",fix,2_000,1))
        val f = Fixture(original)
        assertFalse(f.engine.immediateArrival("id:v1",fix,ARRIVAL_MAX_AGE_MS + 1_001,1))
        f.engine.transition(setOf("id:v1"),ReminderTransition.EXIT,3_000,1)
        assertFalse(f.engine.immediateArrival("id:v1",fix,4_000,1)) // A newer real event takes precedence.
    }

    @Test fun urgentEveryVisitRemainsUnresolvedAcrossExitAndReentry() = runBlocking {
        val original = record(repeat = ReminderRepeat.EVERY_VISIT)
        val f = Fixture(original.copy(reminder = original.reminder.copy(intensity = ro.andrei.ariciu.feature.reminders.model.NotificationIntensity.URGENT)))
        f.engine.transition(setOf("id:v1"),ReminderTransition.ENTER,10,1)
        f.engine.dispatchNotifications(true)
        f.engine.transition(setOf("id:v1"),ReminderTransition.EXIT,20,1)
        assertEquals(DeliveryPhase.TRIGGERED,f.store.records.getValue("id").reminder.delivery.phase)
        f.engine.transition(setOf("id:v1"),ReminderTransition.ENTER,30,1)
        f.engine.dispatchNotifications(true)
        assertEquals(1,f.store.records.getValue("id").reminder.delivery.occurrence)
        assertEquals(1,f.notifications.posts)
    }

    @Test fun immediateUrgentArrivalSwipeRetryAndExplicitActionsKeepOneOccurrence() = runBlocking {
        val original = record().let { it.copy(reminder = it.reminder.copy(intensity = NotificationIntensity.URGENT)) }
        val f = Fixture(original)
        assertTrue(f.engine.immediateArrival("id:v1",ArrivalLocationFix(45.0,25.0,10.0,100,1),200,1))
        f.engine.dispatchNotifications(true)
        f.engine.action("id:old",1,OccurrenceAction.DISMISS,1_000)
        assertEquals(0L,f.store.records.getValue("id").reminder.delivery.urgentRetryAtMs)
        f.engine.action("id:v1",1,OccurrenceAction.DISMISS,1_000)
        f.engine.action("id:v1",1,OccurrenceAction.DISMISS,2_000)
        assertEquals(301_000L,f.store.records.getValue("id").reminder.delivery.urgentRetryAtMs)
        f.engine.dispatchNotifications(true); assertTrue(f.notifications.visible.isEmpty())
        f.engine.wakeUrgentRetries(300_999); f.engine.dispatchNotifications(true); assertEquals(1,f.notifications.posts)
        f.engine.wakeUrgentRetries(301_000); f.engine.dispatchNotifications(true)
        assertEquals(2,f.notifications.posts)
        assertEquals(1,f.store.records.getValue("id").reminder.delivery.occurrence)
        f.engine.action("id:v1",1,OccurrenceAction.DISMISS,302_000,notificationAttempt = 0)
        assertTrue(f.store.records.getValue("id").reminder.delivery.notificationVisible)
        f.engine.wakeUrgentRetries(900_000); f.engine.dispatchNotifications(true)
        assertEquals(2,f.notifications.posts)
        f.engine.action("id:v1",1,OccurrenceAction.DISMISS,303_000,notificationAttempt = 1)
        f.engine.action("id:v1",1,OccurrenceAction.DONE)
        f.engine.wakeUrgentRetries(1_000_000); f.engine.dispatchNotifications(true)
        assertEquals(DeliveryPhase.COMPLETED,f.store.records.getValue("id").reminder.delivery.phase)
        assertEquals(2,f.notifications.posts)
    }

    @Test fun dueRetryWithNoNotificationAccessWaitsInOutboxAndStaleWakeCannotTargetEditedGeneration() = runBlocking {
        val original = record().let { it.copy(reminder = it.reminder.copy(intensity = NotificationIntensity.URGENT)) }
        val f = Fixture(original)
        f.engine.transition(setOf("id:v1"),ReminderTransition.ENTER,10,1)
        f.engine.action("id:v1",1,OccurrenceAction.DISMISS,1_000)
        val pending = f.store.records.getValue("id").copy(generation = "edited")
        f.store.records["id"] = pending
        f.engine.wakeUrgentRetries(900_000,"id:v1",1,0)
        assertEquals(pending,f.store.records.getValue("id"))
        f.engine.wakeUrgentRetries(900_000,"id:edited",1,0)
        f.engine.dispatchNotifications(false)
        assertTrue(f.store.records.getValue("id").reminder.delivery.notificationPending)
        assertEquals(0,f.notifications.posts)
        f.engine.dispatchNotifications(true); f.engine.dispatchNotifications(true)
        assertEquals(1,f.notifications.posts)
        f.store.records.remove("id")
        f.engine.wakeUrgentRetries(1_000_000,"id:edited",1,0)
        assertTrue(f.store.records.isEmpty())
    }

    @Test fun normalAndAlarmDismissEventsCannotStartUrgentRetry() = runBlocking {
        for (intensity in listOf(NotificationIntensity.SILENT,NotificationIntensity.NORMAL,NotificationIntensity.ALARM)) {
            val original = record().let { it.copy(reminder = it.reminder.copy(intensity = intensity)) }
            val f = Fixture(original)
            f.engine.transition(setOf("id:v1"),ReminderTransition.ENTER,10,1)
            val before = f.store.records.getValue("id")
            f.engine.action("id:v1",1,OccurrenceAction.DISMISS,1_000)
            assertEquals(before,f.store.records.getValue("id"))
        }
    }

    @Test fun blockedChannelBackendKeepsTheDueAttemptInOutboxWithoutBurstingOnRecovery() = runBlocking {
        val original = record().let { it.copy(reminder = it.reminder.copy(intensity = NotificationIntensity.URGENT)) }
        val f = Fixture(original)
        f.engine.transition(setOf("id:v1"),ReminderTransition.ENTER,10,1)
        f.engine.action("id:v1",1,OccurrenceAction.DISMISS,1_000)
        f.engine.wakeUrgentRetries(301_000)
        f.notifications.allowed = false
        f.engine.dispatchNotifications(true)
        assertTrue(f.store.records.getValue("id").reminder.delivery.notificationPending)
        assertEquals(0,f.notifications.posts)
        f.notifications.allowed = true
        f.engine.dispatchNotifications(true); f.engine.dispatchNotifications(true)
        assertEquals(1,f.notifications.posts)
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
        var allowed = true
        val visible = mutableMapOf<String, Int>()
        override fun post(record: DeliveryRecord): Boolean {
            if (!allowed) return false
            posts++; visible[record.reminder.id] = record.reminder.delivery.occurrence; return true
        }
        override fun cancelExcept(reminderIds: Set<String>) { visible.keys.retainAll(reminderIds) }
    }
}

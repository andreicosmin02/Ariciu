package ro.andrei.ariciu.feature.reminders.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import ro.andrei.ariciu.feature.delivery.model.ImmediateArrivalAttempt
import ro.andrei.ariciu.feature.delivery.model.ImmediateArrivalResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import ro.andrei.ariciu.feature.reminders.model.*

@OptIn(ExperimentalCoroutinesApi::class)
class RemindersViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    private val location = SelectedLocation(45.6579, 25.6012, 300)

    @Test fun savesOnlyValidDraftAndSuppressesDoubleTap() = runTest(dispatcher) {
        val repo = FakeRepository()
        val vm = RemindersViewModel(repo, SavedStateHandle())
        vm.save()
        advanceUntilIdle()
        assertEquals(0, repo.saves)
        vm.chooseLocation(location)
        vm.updateDraft(vm.draft.value.copy(title = " Bread "))
        vm.save(); vm.save()
        advanceUntilIdle()
        assertEquals(1, repo.saves)
        assertEquals("Bread", repo.rows.value.single().title)
        assertEquals(ReminderRoute.LIST.name, vm.route.value)
        assertEquals(ReminderDraft(), vm.draft.value)
    }

    @Test fun saveFailureRetainsDraftAndRetriesWithSameIdentity() = runTest(dispatcher) {
        val repo = FakeRepository()
        val vm = RemindersViewModel(repo, SavedStateHandle())
        vm.chooseLocation(location)
        vm.updateDraft(vm.draft.value.copy(title = "Bread"))
        repo.failSave = true
        vm.save(); advanceUntilIdle()
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.busy)
        assertEquals("Bread", vm.draft.value.title)
        val firstId = repo.lastId
        repo.failSave = false
        vm.save(); advanceUntilIdle()
        assertEquals(firstId, repo.rows.value.single().id)
    }

    @Test fun restoresDraftRouteAndEditIdentityFromSavedState() = runTest(dispatcher) {
        val repo = FakeRepository()
        val savedState = SavedStateHandle()
        val vm = RemindersViewModel(repo, savedState)
        vm.edit(Reminder("existing", "Bread", location, ReminderTransition.EXIT, ReminderRepeat.EVERY_VISIT, intensity = NotificationIntensity.ALARM, chainName = "Kaufland",
            chainAreas = listOf(ro.andrei.ariciu.feature.delivery.model.ChainArea("n1",45.0,25.0))))
        val restored = RemindersViewModel(repo, SavedStateHandle(savedState.keys().associateWith { savedState.get<Any?>(it) }))
        assertEquals(vm.draft.value, restored.draft.value)
        assertEquals(ReminderRoute.EDITOR.name, restored.route.value)
        restored.updateDraft(restored.draft.value.copy(title = "Milk"))
        restored.save(); advanceUntilIdle()
        assertEquals("existing", repo.rows.value.single().id)
        assertEquals(ReminderTransition.EXIT, repo.rows.value.single().transition)
    }

    @Test fun editUpdatesRowAndDeleteRemovesIt() = runTest(dispatcher) {
        val repo = FakeRepository()
        val existing = Reminder("id", "Bread", location, ReminderTransition.ENTER, ReminderRepeat.ONCE)
        repo.rows.value = listOf(existing)
        val vm = RemindersViewModel(repo, SavedStateHandle())
        vm.edit(existing)
        vm.updateDraft(vm.draft.value.copy(title = "Milk"))
        vm.save(); advanceUntilIdle()
        assertEquals(1, repo.rows.value.size)
        assertEquals("Milk", repo.rows.value.single().title)
        vm.delete(repo.rows.value.single()); advanceUntilIdle()
        assertTrue(vm.state.value.reminders.isEmpty())
    }

    @Test fun immediateAttemptRunsOnlyAfterSaveAndFailureNeverLosesTheReminder() = runTest(dispatcher) {
        val repo = FakeRepository(); val vm = RemindersViewModel(repo, SavedStateHandle())
        vm.chooseLocation(location); vm.updateDraft(vm.draft.value.copy(title = "Here", notifyOnSave = true))
        var calls = 0
        val attempt = ImmediateArrivalAttempt { requestId, reminder ->
            calls++; assertEquals(reminder.id + ":test-generation",requestId)
            assertEquals(reminder.id,repo.rows.value.single().id)
            assertTrue(vm.state.value.checkingArrival)
            error("GPS unavailable")
        }
        vm.save(attempt); vm.save(attempt); advanceUntilIdle()
        assertEquals(1,calls); assertEquals(1,repo.saves)
        assertEquals(ReminderRoute.LIST.name,vm.route.value)
        assertNull(vm.state.value.error)
        assertEquals(ImmediateArrivalResult.UNAVAILABLE.message,vm.state.value.notice)
        assertFalse(vm.state.value.busy); assertFalse(vm.state.value.checkingArrival)
    }

    @Test fun immediateAttemptIsOffByDefaultAndNeverRunsForEditChainOrExit() = runTest(dispatcher) {
        var calls = 0
        val attempt = ImmediateArrivalAttempt { _, _ -> calls++; ImmediateArrivalResult.REQUESTED }
        for (draft in listOf(ReminderDraft("Default",location), ReminderDraft("Exit",location, transition = ReminderTransition.EXIT, notifyOnSave = true),
            ReminderDraft("Chain",location,chainName = "Kaufland",chainAreas = listOf(ro.andrei.ariciu.feature.delivery.model.ChainArea("n1",45.0,25.0)),notifyOnSave = true))) {
            val vm = RemindersViewModel(FakeRepository(), SavedStateHandle()); vm.updateDraft(draft); vm.save(attempt); advanceUntilIdle()
        }
        val vm = RemindersViewModel(FakeRepository(), SavedStateHandle())
        vm.edit(Reminder("existing","Existing",location,ReminderTransition.ENTER,ReminderRepeat.ONCE))
        vm.updateDraft(vm.draft.value.copy(notifyOnSave = true)); vm.save(attempt); advanceUntilIdle()
        assertEquals(0,calls)
    }

    @Test fun newSaveRetryKeepsOptInButChangingToDepartureClearsIt() = runTest(dispatcher) {
        val repo = FakeRepository(); val handle = SavedStateHandle(); val vm = RemindersViewModel(repo,handle)
        vm.updateDraft(ReminderDraft("Here",location,notifyOnSave = true)); repo.failSave = true; vm.save(); advanceUntilIdle()
        assertFalse(vm.editing)
        val restored = RemindersViewModel(repo,SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) }))
        assertTrue(restored.draft.value.notifyOnSave)
        repo.failSave = false; var calls = 0
        restored.save(ImmediateArrivalAttempt { _, _ -> calls++; ImmediateArrivalResult.OUTSIDE }); advanceUntilIdle()
        assertEquals(1,calls)
        restored.updateDraft(ReminderDraft("Other",location,notifyOnSave = true))
        restored.updateDraft(restored.draft.value.copy(transition = ReminderTransition.EXIT))
        assertFalse(restored.draft.value.notifyOnSave)
    }

    @Test fun newAlarmDraftsUseUrgentButExistingAlarmEditsKeepTheirLevel() = runTest(dispatcher) {
        val repo = FakeRepository()
        val vm = RemindersViewModel(repo,SavedStateHandle(mapOf("intensity" to "ALARM")))
        assertEquals(NotificationIntensity.URGENT,vm.draft.value.intensity)
        vm.updateDraft(ReminderDraft("New",location,intensity = NotificationIntensity.ALARM))
        assertEquals(NotificationIntensity.URGENT,vm.draft.value.intensity)
        vm.edit(Reminder("legacy","Legacy",location,ReminderTransition.ENTER,ReminderRepeat.ONCE,intensity = NotificationIntensity.ALARM))
        assertEquals(NotificationIntensity.ALARM,vm.draft.value.intensity)
        vm.save(); advanceUntilIdle()
        assertEquals(NotificationIntensity.ALARM,repo.rows.value.single().intensity)
    }

    @Test fun cardNextTimeTargetsTheDisplayedOccurrenceAndPreservesOtherReminders() = runTest(dispatcher) {
        val repo = FakeRepository()
        val pending = Reminder("urgent","Urgent",location,ReminderTransition.ENTER,ReminderRepeat.EVERY_VISIT,
            intensity = NotificationIntensity.URGENT,delivery = ro.andrei.ariciu.feature.delivery.model.DeliveryState(
                phase = ro.andrei.ariciu.feature.delivery.model.DeliveryPhase.TRIGGERED,occurrence = 2,urgentRetryAtMs = 301000))
        val other = pending.copy(id = "other")
        repo.rows.value = listOf(pending,other)
        val vm = RemindersViewModel(repo,SavedStateHandle()); advanceUntilIdle()
        vm.nextTime(pending); advanceUntilIdle()
        assertEquals(ro.andrei.ariciu.feature.delivery.model.DeliveryPhase.WAITING_NEXT_VISIT,repo.rows.value.first().delivery.phase)
        assertEquals(other,repo.rows.value.last())
        assertEquals(0L,repo.rows.value.first().delivery.urgentRetryAtMs)
    }

    private class FakeRepository : ReminderRepository {
        val rows = MutableStateFlow(emptyList<Reminder>())
        var failSave = false
        var saves = 0
        var lastId: String? = null
        override fun observeReminders() = rows
        override suspend fun save(reminder: Reminder): String {
            saves++
            lastId = reminder.id
            if (failSave) error("Disk unavailable")
            rows.value = rows.value.filterNot { it.id == reminder.id } + reminder
            return "test-generation"
        }
        override suspend fun complete(id: String, occurrence: Int) {
                rows.value = rows.value.map { if (it.id == id && it.delivery.occurrence == occurrence)
                    it.copy(delivery = it.delivery.completed()) else it }
            }
            override suspend fun nextTime(id: String, occurrence: Int) {
            rows.value = rows.value.map { if (it.id == id && it.unresolvedUrgent)
                it.copy(delivery = it.delivery.onAction(ro.andrei.ariciu.feature.delivery.model.OccurrenceAction.NEXT_TIME,occurrence,it.transition,allowUnresolvedUrgent = true)) else it }
        }
        override suspend fun delete(id: String) { rows.value = rows.value.filterNot { it.id == id } }
    }
}

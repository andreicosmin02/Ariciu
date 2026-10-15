package ro.andrei.ariciu.feature.reminders.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
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
        vm.edit(Reminder("existing", "Bread", location, ReminderTransition.EXIT, ReminderRepeat.EVERY_VISIT))
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

    private class FakeRepository : ReminderRepository {
        val rows = MutableStateFlow(emptyList<Reminder>())
        var failSave = false
        var saves = 0
        var lastId: String? = null
        override fun observeReminders() = rows
        override suspend fun save(reminder: Reminder) {
            saves++
            lastId = reminder.id
            if (failSave) error("Disk unavailable")
            rows.value = rows.value.filterNot { it.id == reminder.id } + reminder
        }
        override suspend fun delete(id: String) { rows.value = rows.value.filterNot { it.id == id } }
    }
}

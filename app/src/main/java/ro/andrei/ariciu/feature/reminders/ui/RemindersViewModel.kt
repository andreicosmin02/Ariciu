package ro.andrei.ariciu.feature.reminders.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import ro.andrei.ariciu.feature.reminders.model.Reminder
import ro.andrei.ariciu.feature.reminders.model.ReminderDraft
import ro.andrei.ariciu.feature.reminders.model.ReminderRepeat
import ro.andrei.ariciu.feature.reminders.model.ReminderRepository
import ro.andrei.ariciu.feature.reminders.model.ReminderTransition

enum class ReminderRoute { MAP, EDITOR, LIST, DELIVERY }

data class RemindersUiState(
    val reminders: List<Reminder> = emptyList(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
)

class RemindersViewModel(
    private val repository: ReminderRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(RemindersUiState())
    val state = mutableState.asStateFlow()
    val route = savedState.getStateFlow("route", ReminderRoute.MAP.name)
    private val mutableDraft = MutableStateFlow(restoreDraft())
    val draft = mutableDraft.asStateFlow()
    val editing: Boolean get() = savedState.get<String>("editingId") != null

    private var loadStarted = false
    private var loadJob: kotlinx.coroutines.Job? = null

    init { reload() }

    fun reload() {
        if (mutableState.value.loading && loadStarted) return
        loadStarted = true
        mutableState.value = mutableState.value.copy(loading = true, error = null)
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                repository.observeReminders().collect {
                    mutableState.value = mutableState.value.copy(reminders = it, loading = false)
                }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(loading = false,
                    error = "Could not load reminders. Please retry.")
            }
        }
    }

    fun updateDraft(value: ReminderDraft) {
        if (state.value.busy) return
        mutableDraft.value = value
        savedState["title"] = value.title
        savedState["latitude"] = value.location?.latitude
        savedState["longitude"] = value.location?.longitude
        savedState["radius"] = value.location?.radiusMeters
        savedState["transition"] = value.transition.name
        savedState["repeat"] = value.repeat.name
    }

    fun chooseLocation(location: SelectedLocation) {
        updateDraft(draft.value.copy(location = location))
        navigate(ReminderRoute.EDITOR)
    }

    fun edit(reminder: Reminder) {
        if (state.value.busy) return
        savedState["editingId"] = reminder.id
        updateDraft(reminder.toDraft())
        navigate(ReminderRoute.EDITOR)
    }

    fun openReminder(id: String) {
        savedState["openReminderId"] = id
        navigate(ReminderRoute.LIST)
        // Listing keeps any existing draft intact; the matching reminder is highlighted in the UI.
    }

    val openReminderId get() = savedState.get<String>("openReminderId")

    fun navigate(destination: ReminderRoute) {
        if (!state.value.busy) savedState["route"] = destination.name
    }

    fun discard() {
        if (state.value.busy) return
        savedState["editingId"] = null
        updateDraft(ReminderDraft())
        navigate(ReminderRoute.MAP)
    }

    fun save() {
        if (state.value.busy || !draft.value.canSave) return
        // Reuse this ID on retries and after recreation; an interrupted save cannot duplicate a row.
        val id = savedState.get<String>("editingId") ?: UUID.randomUUID().toString().also {
            savedState["editingId"] = it
        }
        val reminder = draft.value.toReminder(id)
        perform("Could not save reminder. Your draft is preserved; please retry.") {
            repository.save(reminder)
            savedState["editingId"] = null
            mutableState.value = state.value.copy(busy = false)
            updateDraft(ReminderDraft())
            navigate(ReminderRoute.LIST)
        }
    }

    fun delete(reminder: Reminder) {
        perform("Could not delete reminder. Please retry.") { repository.delete(reminder.id) }
    }

    fun clearError() { mutableState.value = state.value.copy(error = null) }

    private fun perform(errorMessage: String, action: suspend () -> Unit) {
        if (state.value.busy) return
        mutableState.value = state.value.copy(busy = true, error = null)
        viewModelScope.launch {
            try { action()
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { mutableState.value = state.value.copy(error = errorMessage)
            } finally { mutableState.value = state.value.copy(busy = false) }
        }
    }

    private fun restoreDraft(): ReminderDraft {
        val latitude = savedState.get<Double>("latitude")
        val longitude = savedState.get<Double>("longitude")
        val radius = savedState.get<Int>("radius")
        return ReminderDraft(
            title = savedState["title"] ?: "",
            location = if (latitude != null && longitude != null && radius != null)
                SelectedLocation(latitude, longitude, radius) else null,
            transition = ReminderTransition.valueOf(savedState["transition"] ?: ReminderTransition.ENTER.name),
            repeat = ReminderRepeat.valueOf(savedState["repeat"] ?: ReminderRepeat.ONCE.name),
        )
    }
}

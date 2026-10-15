package ro.andrei.ariciu.feature.reminders.model

import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun observeReminders(): Flow<List<Reminder>>
    suspend fun save(reminder: Reminder)
    suspend fun delete(id: String)
}

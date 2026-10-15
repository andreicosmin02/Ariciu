package ro.andrei.ariciu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ro.andrei.ariciu.feature.reminders.data.ReminderDatabase
import ro.andrei.ariciu.feature.reminders.data.RoomReminderRepository
import ro.andrei.ariciu.feature.reminders.ui.AriciuApp
import ro.andrei.ariciu.feature.reminders.ui.RemindersViewModel
import ro.andrei.ariciu.ui.theme.AriciuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AriciuTheme {
                val model: RemindersViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        RemindersViewModel(
                            RoomReminderRepository(ReminderDatabase.getInstance(applicationContext).reminderDao()),
                            createSavedStateHandle(),
                        )
                    }
                })
                AriciuApp(model)
            }
        }
    }
}

package ro.andrei.ariciu

import android.os.Bundle
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import ro.andrei.ariciu.feature.delivery.platform.*
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
import ro.andrei.ariciu.ui.theme.ThemePreferences
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val notificationReminder = mutableStateOf<Pair<Int, String?>?>(null)
    private var notificationOpenSequence = 0
    private val themePreferences by lazy { ThemePreferences(applicationContext) }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationReminder.value = ++notificationOpenSequence to intent.getStringExtra(EXTRA_REMINDER_ID)
        intent.removeExtra(EXTRA_REMINDER_ID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationReminder.value = ++notificationOpenSequence to intent.getStringExtra(EXTRA_REMINDER_ID)
        intent.removeExtra(EXTRA_REMINDER_ID)
        AndroidReminderNotifications(applicationContext)
        DeliveryWork.reconcile(applicationContext, force = true)
        enableEdgeToEdge()
        setContent {
            val appearance by themePreferences.preference.collectAsStateWithLifecycle()
            AriciuTheme(darkTheme = appearance.isDark(isSystemInDarkTheme())) {
                val model: RemindersViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        RemindersViewModel(
                            RoomReminderRepository(ReminderDatabase.getInstance(applicationContext).reminderDao()) {
                                DeliveryWork.reconcile(applicationContext)
                            },
                            createSavedStateHandle(),
                        )
                    }
                })
                androidx.compose.runtime.LaunchedEffect(notificationReminder.value) {
                    notificationReminder.value?.second?.let { model.openReminder(it) }
                }
                AriciuApp(model, appearance, themePreferences::select)
            }
        }
    }
}

package ro.andrei.ariciu.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemePreferences(context: Context, name: String = "appearance") {
    private val preferences = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val mutablePreference = MutableStateFlow(ThemePreference.fromStored(preferences.getString("theme", null)))
    val preference = mutablePreference.asStateFlow()

    fun select(value: ThemePreference) {
        preferences.edit().putString("theme", value.name).apply()
        mutablePreference.value = value
    }
}

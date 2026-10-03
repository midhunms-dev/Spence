package com.example.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val title: String) {
    SYSTEM("Device Setting (System Default)"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

class ThemePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private fun loadThemeMode(): ThemeMode {
        val savedName = prefs.getString("selected_theme_mode", ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(savedName ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("selected_theme_mode", mode.name).apply()
        _themeMode.value = mode
    }
}

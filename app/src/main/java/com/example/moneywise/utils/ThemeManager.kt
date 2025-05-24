package com.example.moneywise.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    const val THEME_LIGHT = 0
    const val THEME_DARK = 1
    const val THEME_SYSTEM = 2

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveThemeMode(context: Context, themeMode: Int) {
        getPreferences(context).edit()
            .putInt(KEY_THEME_MODE, themeMode)
            .apply()
    }

    fun getThemeMode(context: Context): Int {
        return getPreferences(context).getInt(KEY_THEME_MODE, THEME_SYSTEM)
    }

    fun applyTheme(themeMode: Int) {
        when (themeMode) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun toggleTheme(context: Context) {
        val currentMode = getThemeMode(context)
        val newMode = if (currentMode == THEME_LIGHT) THEME_DARK else THEME_LIGHT
        saveThemeMode(context, newMode)
        applyTheme(newMode)
    }

    fun isDarkMode(context: Context): Boolean {
        return getThemeMode(context) == THEME_DARK
    }

    fun initializeTheme(context: Context) {
        val savedTheme = getThemeMode(context)
        applyTheme(savedTheme)
    }
}
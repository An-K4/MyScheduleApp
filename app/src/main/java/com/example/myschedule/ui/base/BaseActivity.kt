package com.example.myschedule.ui.base

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

abstract class BaseActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "app_prefs"
        const val KEY_DARK_MODE = "dark_mode"
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        initTheme()
        super.onCreate(savedInstanceState)
    }

    private fun initTheme() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean(KEY_DARK_MODE, true)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun saveTheme(isDark: Boolean) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_DARK_MODE, isDark) }
    }

    fun isDarkMode(): Boolean =
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK_MODE, true)
}
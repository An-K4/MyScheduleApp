package com.example.myschedule.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

abstract class BaseActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "app_prefs"
        const val KEY_DARK_MODE = "dark_mode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        initTheme()
        super.onCreate(savedInstanceState)
    }

    private fun initTheme() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isDark = prefs.getBoolean(KEY_DARK_MODE, true)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun saveTheme(isDark: Boolean) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit { putBoolean(KEY_DARK_MODE, isDark) }
    }

    fun isDarkMode(): Boolean =
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getBoolean(KEY_DARK_MODE, true)
}
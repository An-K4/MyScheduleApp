package com.example.myschedule.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.myschedule.R
import com.example.myschedule.databinding.ActivityMainBinding
import com.example.myschedule.ui.agenda.AgendaFragment
import com.example.myschedule.ui.day.DayFragment
import com.example.myschedule.ui.event.AddEditEventFragment
import com.example.myschedule.ui.month.MonthFragment
import com.example.myschedule.ui.source.SourceManagerActivity
import com.example.myschedule.ui.year.YearFragment
import com.example.myschedule.viewmodel.MainViewModel
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_DARK_MODE = "dark_mode"
    }

    private lateinit var binding: ActivityMainBinding
    val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initTheme()
        setupDrawer()
        setupClickListeners()

        onBackPressedDispatcher.addCallback(this) {
            when {
                binding.drawerLayout.isDrawerOpen(GravityCompat.START) ->
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                supportFragmentManager.backStackEntryCount > 1 ->
                    supportFragmentManager.popBackStack()
                else -> finish()
            }
        }

        // Load MonthFragment mặc định
        if (savedInstanceState == null) {
            showFragment(MonthFragment(), "MONTH")
            binding.navigationView.setCheckedItem(R.id.nav_month)
        }
    }

    private fun initTheme() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean(KEY_DARK_MODE, true)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        updateThemeIcon(isDark)
    }

    private fun updateThemeIcon(isDark: Boolean) {
        binding.btnToggleTheme.setImageResource(
            if (isDark) R.drawable.ic_theme_toggle else R.drawable.ic_sun
        )
    }

    private fun setupDrawer() {
        binding.btnDrawerToggle.setOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                binding.drawerLayout.openDrawer(GravityCompat.START)
            }
        }
        binding.navigationView.setNavigationItemSelectedListener(this)
    }

    private fun setupClickListeners() {
        binding.btnToggleTheme.setOnClickListener {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isDark = prefs.getBoolean(KEY_DARK_MODE, true)
            val newIsDark = !isDark
            prefs.edit { putBoolean(KEY_DARK_MODE, newIsDark) }
            AppCompatDelegate.setDefaultNightMode(
                if (newIsDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        binding.btnSourceManager.setOnClickListener {
            startActivity(Intent(this, SourceManagerActivity::class.java))
        }

        // 1.8 — Nút + → AddEditEventFragment
        binding.btnAddEvent.setOnClickListener {
            showFragment(AddEditEventFragment.newInstance(), "ADD_EDIT_EVENT")
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val fragment: Fragment = when (item.itemId) {
            R.id.nav_month -> MonthFragment()
            R.id.nav_day -> DayFragment()
            R.id.nav_year -> YearFragment()
            R.id.nav_agenda -> AgendaFragment()
            else -> return false
        }
        showFragment(fragment, item.itemId.toString())
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    fun showFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }

    fun updateMonthYearTitle(text: String) {
        binding.tvMonthYear.text = text
    }
}
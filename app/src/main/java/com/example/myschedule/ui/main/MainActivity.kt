package com.example.myschedule.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
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
import java.time.LocalDate

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_DARK_MODE = "dark_mode"
    }

    lateinit var binding: ActivityMainBinding
    val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initTheme()
        setupDrawer()
        setupClickListeners()
        askNotificationPermission()

        binding.btnToday.text = LocalDate.now().dayOfMonth.toString()

        onBackPressedDispatcher.addCallback(this) {
            when {
                binding.drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                }
                supportFragmentManager.backStackEntryCount > 0 -> {
                    supportFragmentManager.popBackStack()
                }
                else -> {
                    finish()
                }
            }
        }

        // Load MonthFragment mặc định
        if (savedInstanceState == null) {
            switchTab(MonthFragment(), "MONTH")
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

        binding.btnToday.setOnClickListener {
            viewModel.goToToday()
        }

        // 1.8 — Nút + → AddEditEventFragment
        binding.btnAddEvent.setOnClickListener {
            pushFragment(AddEditEventFragment.newInstance(), "ADD_EDIT_EVENT")
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
        switchTab(fragment, item.itemId.toString())
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    fun switchTab(fragment: Fragment, tag: String) {
        // Xóa toàn bộ backstack
        repeat(supportFragmentManager.backStackEntryCount) {
            supportFragmentManager.popBackStackImmediate()
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, tag)
            .commit()
    }

    fun pushFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }

    fun updateMonthYearTitle(text: String) {
        binding.tvMonthYear.text = text
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
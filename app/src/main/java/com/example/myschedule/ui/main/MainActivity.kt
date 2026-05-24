package com.example.myschedule.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.myschedule.R
import com.example.myschedule.databinding.ActivityMainBinding
import com.example.myschedule.ui.agenda.AgendaFragment
import com.example.myschedule.ui.base.BaseActivity
import com.example.myschedule.ui.day.DayFragment
import com.example.myschedule.ui.event.AddEventActivity
import com.example.myschedule.ui.month.MonthFragment
import com.example.myschedule.ui.source.SourceManagerActivity
import com.example.myschedule.ui.year.YearFragment
import com.example.myschedule.viewmodel.MainViewModel
import com.google.android.material.navigation.NavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import java.time.LocalDate

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit var binding: ActivityMainBinding
    val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDrawer()
        setupDrawerThemeSwitch()
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
                else -> finish()
            }
        }

        if (savedInstanceState == null) {
            switchTab(MonthFragment(), "MONTH")
            binding.navigationView.setCheckedItem(R.id.nav_month)
        }
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

    private fun setupDrawerThemeSwitch() {
        val menuItem = binding.navigationView.menu.findItem(R.id.nav_theme)
        val switchView = menuItem.actionView as SwitchMaterial

        switchView.isChecked = isDarkMode()

        switchView.setOnCheckedChangeListener { _, isChecked ->
            saveTheme(isChecked)
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun setupClickListeners() {
        binding.btnToday.setOnClickListener {
            viewModel.goToToday()
        }

        binding.btnAddEvent.setOnClickListener {
            startActivity(Intent(this, AddEventActivity::class.java))
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_month -> switchTab(MonthFragment(), item.itemId.toString())
            R.id.nav_day -> switchTab(DayFragment(), item.itemId.toString())
            R.id.nav_year -> switchTab(YearFragment(), item.itemId.toString())
            R.id.nav_agenda -> switchTab(AgendaFragment(), item.itemId.toString())
            R.id.nav_sources -> startActivity(Intent(this, SourceManagerActivity::class.java))
            R.id.nav_theme -> return false
            else -> return false
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    fun switchTab(fragment: Fragment, tag: String) {
        repeat(supportFragmentManager.backStackEntryCount) {
            supportFragmentManager.popBackStackImmediate()
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, tag)
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
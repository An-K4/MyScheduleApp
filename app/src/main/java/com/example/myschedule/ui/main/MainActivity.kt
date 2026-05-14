package com.example.myschedule.ui.main

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myschedule.R
import com.example.myschedule.data.entity.CalendarEvent
import com.example.myschedule.data.repository.CalendarRepository
import com.example.myschedule.databinding.ActivityMainBinding
import com.example.myschedule.databinding.CalendarDayLayoutBinding
import com.example.myschedule.databinding.CalendarHeaderLayoutBinding
import com.example.myschedule.receiver.NotificationReceiver
import com.example.myschedule.viewmodel.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_DARK_MODE = "dark_mode"
    }

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var eventAdapter: EventAdapter

    // Cache local để calendar binder đọc đồng bộ (không cần suspend)
    private var eventDatesCache: Set<LocalDate> = emptySet()
    private var selectedDate: LocalDate = LocalDate.now()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    private val selectIcsFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val fileName = getFileName(it)
                viewModel.importIcsFile(it, fileName)
                Toast.makeText(this, "Đã nhập: $fileName", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initTheme()
        setupRecyclerView()
        setupCalendar()
        observeViewModel()
        setupClickListeners()
        askNotificationPermission()
    }

    private fun initTheme() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean(KEY_DARK_MODE, true) // default: dark
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        updateThemeIcon(isDark)
    }

    private fun updateThemeIcon(isDark: Boolean) {
        binding.btnToggleTheme.setImageResource(
            if (isDark) R.drawable.ic_theme_toggle  // icon mặt trăng
            else R.drawable.ic_sun                  // icon mặt trời (tạo ở bước sau)
        )
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter()
        eventAdapter.onItemClick = { event -> showEventDetailsDialog(event) }
        binding.rvEvents.apply {
            adapter = eventAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun observeViewModel() {
        // Quan sát danh sách ngày có sự kiện → cập nhật dấu chấm trên lịch
        viewModel.eventDates.observe(this) { dates ->
            eventDatesCache = dates
            binding.calendarView.notifyCalendarChanged()
        }

        // Quan sát sự kiện của ngày đang chọn → cập nhật danh sách bên dưới
        viewModel.eventsForSelectedDate.observe(this) { events ->
            updateEventList(events)
        }

        // Quan sát ngày được chọn → cập nhật highlight trên lịch
        viewModel.selectedDate.observe(this) { newDate ->
            val oldDate = selectedDate
            selectedDate = newDate
            binding.calendarView.notifyDateChanged(newDate)
            if (oldDate != newDate) binding.calendarView.notifyDateChanged(oldDate)
        }
    }

    private fun setupClickListeners() {
        binding.btnAddIcs.setOnClickListener {
            selectIcsFileLauncher.launch(arrayOf("text/calendar", "application/octet-stream"))
        }

        binding.btnToggleTheme.setOnClickListener {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isDark = prefs.getBoolean(KEY_DARK_MODE, true)
            val newIsDark = !isDark

            prefs.edit { putBoolean(KEY_DARK_MODE, newIsDark) }
            AppCompatDelegate.setDefaultNightMode(
                if (newIsDark) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    // ── Calendar Setup ────────────────────────────────────────────────────────

    inner class DayViewContainer(view: View) : ViewContainer(view) {
        val dayBinding = CalendarDayLayoutBinding.bind(view)
        lateinit var day: CalendarDay

        init {
            view.setOnClickListener {
                if (day.position == DayPosition.MonthDate) {
                    if (day.date != selectedDate) {
                        viewModel.selectDate(day.date)
                    }
                }
            }
        }
    }

    class MonthViewContainer(view: View) : ViewContainer(view) {
        val titlesContainer = CalendarHeaderLayoutBinding.bind(view)
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    private fun setupCalendar() {
        val currentMonth = YearMonth.now()
        val daysOfWeek = daysOfWeek(firstDayOfWeek = DayOfWeek.MONDAY)

        binding.calendarView.setup(
            currentMonth.minusMonths(100),
            currentMonth.plusMonths(100),
            daysOfWeek.first()
        )

        // scroll đến tháng người dùng đã chọn trước đó, nếu không có thì chọn tháng hiện tại
        val monthToShow = viewModel.currentMonth.value ?: currentMonth
        binding.calendarView.scrollToMonth(monthToShow)

        binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data
                val textView = container.dayBinding.tvDayText
                val dotView = container.dayBinding.vEventDot
                val rootView = container.view

                textView.text = data.date.dayOfMonth.toString()

                if (data.position == DayPosition.MonthDate) {
                    textView.visibility = View.VISIBLE
                    dotView.visibility =
                        if (eventDatesCache.contains(data.date)) View.VISIBLE else View.INVISIBLE

                    if (data.date == selectedDate) {
                        rootView.setBackgroundResource(R.drawable.selected_day_background)
                        textView.setTextColor(resources.getColor(R.color.white, null)) // giữ trắng khi selected
                    } else {
                        rootView.background = null
                        textView.setTextColor(
                            if (data.date.dayOfWeek == DayOfWeek.SUNDAY)
                                resources.getColor(R.color.sunday_text_color, null)
                            else
                                getThemeColor(com.google.android.material.R.attr.colorOnBackground)
                        )
                    }
                } else {
                    textView.visibility = View.INVISIBLE
                    dotView.visibility = View.INVISIBLE
                }
            }
        }

        binding.calendarView.monthScrollListener = { month ->
            viewModel.setCurrentMonth(month.yearMonth)  // ← thêm dòng này
            val monthName = month.yearMonth.month.getDisplayName(TextStyle.FULL, Locale("vi"))
            binding.tvMonthYear.text = "${
                monthName.replaceFirstChar { it.titlecase(Locale("vi")) }
            } ${month.yearMonth.year}"
        }

        binding.calendarView.monthHeaderBinder =
            object : MonthHeaderFooterBinder<MonthViewContainer> {
                override fun create(view: View) = MonthViewContainer(view)
                override fun bind(container: MonthViewContainer, data: CalendarMonth) {
                    val daysOfWeek = daysOfWeek(firstDayOfWeek = DayOfWeek.MONDAY)
                    container.titlesContainer.root.children.map { it as TextView }
                        .forEachIndexed { index, textView ->
                            textView.text = daysOfWeek[index]
                                .getDisplayName(TextStyle.SHORT, Locale("vi"))
                        }
                }
            }

        // Trigger hiển thị sự kiện ngày hôm nay, nếu chưa chọn ngày thì lấy ngày hiện tại
        if (viewModel.selectedDate.value == null) {
            viewModel.selectDate(LocalDate.now())
        }
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────

    private fun updateEventList(events: List<CalendarEvent>) {
        binding.tvNoEvent.visibility = View.GONE
        binding.rvEvents.visibility = View.GONE

        if (events.isEmpty()) {
            binding.tvNoEvent.text = "Không có sự kiện nào"
            binding.tvNoEvent.visibility = View.VISIBLE
        } else {
            binding.rvEvents.visibility = View.VISIBLE
            eventAdapter.submitList(events)
        }
    }

    private fun showEventDetailsDialog(event: CalendarEvent) {
        MaterialAlertDialogBuilder(this)
            .setTitle(event.title)
            .setMessage(buildString {
                if (!event.location.isNullOrBlank()) appendLine("📍 ${event.location}\n")
                append(event.description ?: "Không có mô tả chi tiết.")
            })
            .setPositiveButton("Đóng") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun getFileName(uri: Uri): String {
        var name = "Lịch mới"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx != -1) name = cursor.getString(idx).removeSuffix(".ics")
            }
        }
        return name
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
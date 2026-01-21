package com.example.myschedule

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import com.example.myschedule.databinding.ActivityMainBinding
import com.example.myschedule.databinding.CalendarDayLayoutBinding
import com.example.myschedule.databinding.CalendarHeaderLayoutBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kizitonwose.calendar.core.*
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.Description
import net.fortuna.ical4j.model.property.DtStart
import java.io.InputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import net.fortuna.ical4j.model.property.Uid
import java.util.Calendar
import androidx.core.content.edit
import androidx.core.net.toUri

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedDate: LocalDate? = null
    private val events = mutableMapOf<LocalDate, List<VEvent>>()
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            Toast.makeText(this, "Bạn tày rồi", Toast.LENGTH_SHORT).show()
        }

    private val selectIcsFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(it, flags)

                saveLastUsedIcsUri(it)
                loadAndParseIcs(it)
            }
        }

    private lateinit var eventAdapter: EventAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        eventAdapter = EventAdapter()
        eventAdapter.onItemClick = { event ->
            showEventDetailsDialog(event) // Gọi hàm hiển thị dialog
        }

        binding.rvEvents.apply {
            adapter = eventAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@MainActivity)
        }

        loadLastUsedIcsFile()
        setupCalendar()

        binding.btnAddIcs.setOnClickListener {
            selectIcsFileLauncher.launch(arrayOf("text/calendar", "application/octet-stream"))
        }

        askNotificationPermission()
    }

    private fun parseIcsFile(inputStream: InputStream) {
        try {
            val builder = CalendarBuilder()
            val calendar = builder.build(inputStream)
            events.clear()

            val allParsedEvents = mutableListOf<VEvent>()

            for (component in calendar.getComponents<VEvent>("VEVENT")) {
                val dtStart = component.getProperty<DtStart>(Property.DTSTART)
                if (dtStart != null) {
                    val localDate = dtStart.date.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()

                    val originalEventsForDate = events[localDate]?.toMutableList() ?: mutableListOf()
                    originalEventsForDate.add(component)

                    // Sắp xếp sự kiện trong ngày theo thứ tự thời gian tăng dần
                    val sortedEventsForDate = originalEventsForDate.sortedBy { event ->
                        event.getProperty<DtStart>(Property.DTSTART)?.date?.toInstant()
                    }

                    allParsedEvents.add(component)
                    events[localDate] = sortedEventsForDate
                }
            }

            // Sau khi parse xong, lên lịch thông báo cho tất cả sự kiện
            allParsedEvents.forEach { event ->
                scheduleNotification(event)
            }

            binding.calendarView.notifyCalendarChanged()
            updateEventList(selectedDate)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateEventList(date: LocalDate?) {
        // Ẩn cả 2 view trước khi quyết định hiển thị cái nào
        binding.tvNoEvent.visibility = View.GONE
        binding.rvEvents.visibility = View.GONE

        if (date == null) {
            binding.tvNoEvent.text = "Chọn một ngày để xem sự kiện"
            binding.tvNoEvent.visibility = View.VISIBLE
            return
        }

        val eventsForDate = events[date]
        if (eventsForDate.isNullOrEmpty()) {
            binding.tvNoEvent.text = "Không có sự kiện nào"
            binding.tvNoEvent.visibility = View.VISIBLE
        } else {
            // Có sự kiện -> Hiển thị RecyclerView
            binding.rvEvents.visibility = View.VISIBLE
            // Gửi danh sách sự kiện cho Adapter để hiển thị
            eventAdapter.submitList(eventsForDate)
        }
    }

    inner class DayViewContainer(view: View) : ViewContainer(view) {
        val dayBinding = CalendarDayLayoutBinding.bind(view)
        lateinit var day: CalendarDay

        init {
            view.setOnClickListener {
                if (day.position == DayPosition.MonthDate) {
                    val oldDate = selectedDate
                    if (oldDate == day.date) {
                        // Người dùng click lại vào ngày đang chọn -> không làm gì cả
                        return@setOnClickListener
                    }

                    // Cập nhật ngày mới được chọn
                    selectedDate = day.date

                    // Cập nhật danh sách sự kiện bên dưới
                    updateEventList(selectedDate)

                    binding.calendarView.notifyDateChanged(day.date)
                    oldDate?.let {
                        binding.calendarView.notifyDateChanged(it)
                    }
                }
            }
        }
    }

    class MonthViewContainer(view: View) : ViewContainer(view) {
        val titlesContainer = CalendarHeaderLayoutBinding.bind(view)
    }

    private fun setupCalendar() {
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(100)
        val endMonth = currentMonth.plusMonths(100)
        val daysOfWeek = daysOfWeek(firstDayOfWeek = DayOfWeek.MONDAY)

        binding.calendarView.setup(startMonth, endMonth, daysOfWeek.first())
        binding.calendarView.scrollToMonth(currentMonth)

        binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data
                val textView = container.dayBinding.tvDayText
                val dotView = container.dayBinding.vEventDot

                // Lấy tham chiếu đến view gốc của toàn bộ ô thông qua thuộc tính "view"
                val rootView = container.view

                textView.text = data.date.dayOfMonth.toString()

                if (data.position == DayPosition.MonthDate) {
                    textView.visibility = View.VISIBLE

                    // --- LOGIC 1: DẤU CHẤM (Đã đúng, không đổi) ---
                    dotView.visibility = if (events.containsKey(data.date)) View.VISIBLE else View.INVISIBLE

                    // --- LOGIC 2: TÔ NỀN (Sửa lại để dùng rootView) ---
                    if (data.date == selectedDate) {
                        // SỬA Ở ĐÂY: Tô nền cho TOÀN BỘ Ô (rootView)
                        rootView.setBackgroundResource(R.drawable.selected_day_background)
                        textView.setTextColor(resources.getColor(R.color.white, null))
                    } else {
                        // SỬA Ở ĐÂY: Xóa nền của TOÀN BỘ Ô (rootView)
                        rootView.background = null
                        textView.setTextColor(
                            if (data.date.dayOfWeek == DayOfWeek.SUNDAY)
                                resources.getColor(R.color.sunday_text_color, null)
                            else
                                resources.getColor(R.color.white, null)
                        )
                    }
                } else {
                    textView.visibility = View.INVISIBLE
                    dotView.visibility = View.INVISIBLE
                }
            }
        }

        binding.calendarView.monthScrollListener = { month ->
            val monthName = month.yearMonth.month.getDisplayName(
                TextStyle.FULL,
                Locale("vi")
            )
            val title = "${
                monthName.replaceFirstChar { it.titlecase(Locale("vi")) }
            } ${month.yearMonth.year}"
            binding.tvMonthYear.text = title
        }

        binding.calendarView.monthHeaderBinder =
            object : MonthHeaderFooterBinder<MonthViewContainer> {
                override fun create(view: View) = MonthViewContainer(view)
                override fun bind(container: MonthViewContainer, data: CalendarMonth) {
                    container.titlesContainer.root.children.map { it as TextView }
                        .forEachIndexed { index, textView ->
                            val dayOfWeek = daysOfWeek[index]
                            val title = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("vi"))
                            textView.text = title
                        }
                }
            }

        val today = LocalDate.now()
        selectedDate = today
        updateEventList(today)
        binding.calendarView.notifyDateChanged(today)
    }

    private fun showEventDetailsDialog(event: VEvent) {
        // Lấy tiêu đề từ thuộc tính SUMMARY
        val title = event.summary?.value ?: "Chi tiết sự kiện"

        // Lấy mô tả/ghi chú từ thuộc tính DESCRIPTION
        val description = event.getProperty<Description>("DESCRIPTION")?.value ?: "Không có mô tả chi tiết."

        // Sử dụng MaterialAlertDialogBuilder để tạo dialog theo theme của app
        MaterialAlertDialogBuilder(this)
            .setTitle(title) // Đặt tiêu đề cho dialog
            .setMessage(description) // Đặt nội dung chi tiết
            .setPositiveButton("Đóng") { dialog, _ ->
                dialog.dismiss() // Thêm nút "Đóng" để tắt dialog
            }
            .show() // Hiển thị dialog
    }

    private fun scheduleNotification(event: VEvent) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        val dtStart = event.getProperty<DtStart>(Property.DTSTART) ?: return
        val startTime = dtStart.date.time

        val notificationTime = Calendar.getInstance().apply {
            timeInMillis = startTime
            add(Calendar.MINUTE, -30)
        }

        if (notificationTime.timeInMillis > System.currentTimeMillis()) {
            val intent = Intent(this, NotificationReceiver::class.java)
            val eventTitle = event.summary?.value ?: "Sự kiện sắp tới"
            val uniqueId = event.getProperty<Uid>("UID")?.value?.hashCode() ?: eventTitle.hashCode()

            intent.putExtra(NotificationReceiver.EVENT_TITLE_KEY, eventTitle)
            intent.putExtra(NotificationReceiver.NOTIFICATION_ID_KEY, uniqueId)

            val pendingIntent = PendingIntent.getBroadcast(
                this,
                uniqueId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Đặt báo thức chính xác
            val canScheduleExactAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                // Trên các phiên bản Android cũ hơn, quyền này được cấp mặc định
                true
            }

            if (canScheduleExactAlarms) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    notificationTime.timeInMillis,
                    pendingIntent
                )
            } else {
                Toast.makeText(this, "Sẽ có những con cá phải giả chó", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun askNotificationPermission() {
        // Chỉ áp dụng cho Android 13 (Tiramisu) trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun saveLastUsedIcsUri(uri: Uri) {
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit { putString("last_ics_uri", uri.toString()) }
    }

    private fun loadLastUsedIcsFile() {
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val uriString = sharedPrefs.getString("last_ics_uri", null)

        if (uriString != null) {
            val uri = uriString.toUri()
            loadAndParseIcs(uri)
        }
    }

    private fun loadAndParseIcs(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.let { stream ->
                parseIcsFile(stream)
            }
        } catch (e: SecurityException) {
            // Có thể xảy ra nếu file bị xóa hoặc không còn truy cập được
            e.printStackTrace()
            // Xóa URI đã lưu để không cố gắng tải lại vào lần sau
            val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit { remove("last_ics_uri") }
        }
    }
}
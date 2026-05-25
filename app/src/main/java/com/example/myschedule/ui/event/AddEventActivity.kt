package com.example.myschedule.ui.event

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import com.example.myschedule.R
import com.example.myschedule.data.entity.CalendarEvent
import com.example.myschedule.databinding.ActivityAddEventBinding
import com.example.myschedule.ui.BaseActivity
import com.example.myschedule.viewmodel.AddEventViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class AddEventActivity : BaseActivity() {

    companion object {
        private val DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")
    }

    private lateinit var binding: ActivityAddEventBinding
    private val viewModel: AddEventViewModel by viewModels()

    private var startDate: LocalDate = LocalDate.now()
    private var startTime: LocalTime = LocalTime.now().withSecond(0).withNano(0)
    private var endDate: LocalDate = LocalDate.now()
    private var endTime: LocalTime = LocalTime.now().plusHours(1).withSecond(0).withNano(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateDateTimeViews()
        setupClickListeners()
        observeViewModel()
    }

    private fun updateDateTimeViews() {
        binding.tvStartDate.text = startDate.format(DATE_FMT)
        binding.tvStartTime.text = startTime.format(TIME_FMT)
        binding.tvEndDate.text = endDate.format(DATE_FMT)
        binding.tvEndTime.text = endTime.format(TIME_FMT)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.checkboxAllDay.setOnCheckedChangeListener { _, isChecked ->
            binding.tvStartTime.visibility = if (isChecked) View.GONE else View.VISIBLE
            binding.tvEndTime.visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        binding.tvStartDate.setOnClickListener { showDatePicker(isStart = true) }
        binding.tvEndDate.setOnClickListener { showDatePicker(isStart = false) }
        binding.tvStartTime.setOnClickListener { showTimePicker(isStart = true) }
        binding.tvEndTime.setOnClickListener { showTimePicker(isStart = false) }

        binding.checkboxDisableNotification.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutNotification.visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        binding.btnSave.setOnClickListener { saveEvent() }
    }

    private fun observeViewModel() {
        viewModel.saveSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Đã lưu sự kiện", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        viewModel.errorMessage.observe(this) { msg ->
            if (!msg.isNullOrBlank()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun showDatePicker(isStart: Boolean) {
        val date = if (isStart) startDate else endDate
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val picked = LocalDate.of(year, month + 1, day)
                if (isStart) {
                    startDate = picked
                    if (endDate.isBefore(startDate)) endDate = startDate
                } else {
                    endDate = picked
                }
                updateDateTimeViews()
            },
            date.year, date.monthValue - 1, date.dayOfMonth
        ).show()
    }

    private fun showTimePicker(isStart: Boolean) {
        val time = if (isStart) startTime else endTime
        TimePickerDialog(
            this,
            { _, hour, minute ->
                if (isStart) startTime = LocalTime.of(hour, minute)
                else endTime = LocalTime.of(hour, minute)
                updateDateTimeViews()
            },
            time.hour, time.minute, true
        ).show()
    }

    private fun saveEvent() {
        val title = binding.etTitle.text?.toString()?.trim()
        if (title.isNullOrBlank()) {
            Toast.makeText(this, "Tên sự kiện không được để trống", Toast.LENGTH_SHORT).show()
            return
        }

        val isAllDay = binding.checkboxAllDay.isChecked
        val start: LocalDateTime
        val end: LocalDateTime

        if (isAllDay) {
            start = startDate.atStartOfDay()
            end = endDate.atTime(23, 59, 59)
        } else {
            start = LocalDateTime.of(startDate, startTime)
            end = LocalDateTime.of(endDate, endTime)
            if (!end.isAfter(start)) {
                Toast.makeText(
                    this,
                    "Thời gian kết thúc phải sau thời gian bắt đầu",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        val zone = ZoneId.systemDefault()
        val event = CalendarEvent(
            sourceId = 1,
            uid = UUID.randomUUID().toString(),
            title = title,
            startTime = start.atZone(zone).toInstant().toEpochMilli(),
            endTime = end.atZone(zone).toInstant().toEpochMilli(),
            location = binding.etLocation.text?.toString()?.trim()?.ifBlank { null },
            description = binding.etDescription.text?.toString()?.trim()?.ifBlank { null },
            reminderMinutes = getReminderMinutes()
        )

        viewModel.addEvent(event)
    }

    private fun getReminderMinutes(): Long? {
        if (binding.checkboxDisableNotification.isChecked) return null
        val duration = binding.edNotificationDuration.text
            ?.toString()?.trim()?.toLongOrNull() ?: 0L
        return when (binding.rgReminderUnit.checkedRadioButtonId) {
            R.id.rbHour -> duration * 60
            R.id.rbDay -> duration * 60 * 24
            R.id.rbWeek -> duration * 60 * 24 * 7
            else -> duration
        }
    }
}
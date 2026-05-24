package com.example.myschedule.ui.event

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import com.example.myschedule.R
import com.example.myschedule.data.entity.CalendarEvent
import com.example.myschedule.databinding.ActivityEventDetailBinding
import com.example.myschedule.ui.base.BaseActivity
import com.example.myschedule.viewmodel.EventDetailViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class EventDetailActivity : BaseActivity() {

    companion object {
        const val EXTRA_EVENT_ID = "event_id"

        private val DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")
    }

    private lateinit var binding: ActivityEventDetailBinding
    private val viewModel: EventDetailViewModel by viewModels()

    private var isEditMode = false
    private var startDate: LocalDate = LocalDate.now()
    private var startTime: LocalTime = LocalTime.now()
    private var endDate: LocalDate = LocalDate.now()
    private var endTime: LocalTime = LocalTime.now().plusHours(1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val eventId = intent.getIntExtra(EXTRA_EVENT_ID, -1)
        if (eventId == -1) {
            finish()
            return
        }

        viewModel.loadEvent(eventId)
        setupClickListeners()
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.event.observe(this) { event ->
            if (event == null) {
                finish()
                return@observe
            }
            bindEvent(event)
        }

        viewModel.sourceName.observe(this) { name ->
            binding.tvSource.text = "Nguồn: $name"
        }

        viewModel.finishSignal.observe(this) { shouldFinish ->
            if (shouldFinish) finish()
        }

        viewModel.errorMessage.observe(this) { msg ->
            if (!msg.isNullOrBlank()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun bindEvent(event: CalendarEvent) {
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(event.startTime).atZone(zone)
        val end = Instant.ofEpochMilli(event.endTime).atZone(zone)

        startDate = start.toLocalDate()
        startTime = start.toLocalTime()
        endDate = end.toLocalDate()
        endTime = end.toLocalTime()

        binding.etTitle.setText(event.title)
        binding.etLocation.setText(event.location ?: "")
        binding.etDescription.setText(event.description ?: "")

        updateDateTimeViews()

        val reminderMinutes = event.reminderMinutes
        if (reminderMinutes == null) {
            binding.checkboxDisableNotification.isChecked = true
            binding.layoutNotification.visibility = View.GONE
        } else {
            binding.checkboxDisableNotification.isChecked = false
            binding.layoutNotification.visibility = View.VISIBLE
            when {
                reminderMinutes % (60 * 24 * 7) == 0L -> {
                    binding.edNotificationDuration.setText((reminderMinutes / (60 * 24 * 7)).toString())
                    binding.rgReminderUnit.check(R.id.rbWeek)
                }
                reminderMinutes % (60 * 24) == 0L -> {
                    binding.edNotificationDuration.setText((reminderMinutes / (60 * 24)).toString())
                    binding.rgReminderUnit.check(R.id.rbDay)
                }
                reminderMinutes % 60 == 0L -> {
                    binding.edNotificationDuration.setText((reminderMinutes / 60).toString())
                    binding.rgReminderUnit.check(R.id.rbHour)
                }
                else -> {
                    binding.edNotificationDuration.setText(reminderMinutes.toString())
                    binding.rgReminderUnit.check(R.id.rbMinute)
                }
            }
        }

        setFormEnabled(false)
    }

    private fun updateDateTimeViews() {
        binding.tvStartDate.text = startDate.format(DATE_FMT)
        binding.tvStartTime.text = startTime.format(TIME_FMT)
        binding.tvEndDate.text = endDate.format(DATE_FMT)
        binding.tvEndTime.text = endTime.format(TIME_FMT)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnEdit.setOnClickListener {
            if (!isEditMode) enterEditMode() else saveEdits()
        }

        binding.btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Xóa sự kiện?")
                .setMessage("Sự kiện sẽ bị xóa vĩnh viễn.")
                .setNegativeButton("Hủy") { d, _ -> d.dismiss() }
                .setPositiveButton("Xóa") { _, _ ->
                    viewModel.deleteEvent()
                }
                .show()
        }

        binding.tvStartDate.setOnClickListener { if (isEditMode) showDatePicker(true) }
        binding.tvStartTime.setOnClickListener { if (isEditMode) showTimePicker(true) }
        binding.tvEndDate.setOnClickListener { if (isEditMode) showDatePicker(false) }
        binding.tvEndTime.setOnClickListener { if (isEditMode) showTimePicker(false) }

        binding.checkboxDisableNotification.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutNotification.visibility = if (isChecked) View.GONE else View.VISIBLE
        }
    }

    private fun enterEditMode() {
        isEditMode = true
        setFormEnabled(true)
        binding.btnEdit.setImageResource(R.drawable.ic_check)
        binding.etTitle.requestFocus()
    }

    private fun exitEditMode() {
        isEditMode = false
        setFormEnabled(false)
        binding.btnEdit.setImageResource(R.drawable.ic_edit)
    }

    private fun setFormEnabled(enabled: Boolean) {
        val alpha = if (enabled) 1f else 0.5f
        listOf(
            binding.etTitle,
            binding.etLocation,
            binding.etDescription,
            binding.tvStartDate,
            binding.tvStartTime,
            binding.tvEndDate,
            binding.tvEndTime,
            binding.checkboxDisableNotification,
            binding.edNotificationDuration
        ).forEach {
            it.isEnabled = enabled
            it.alpha = alpha
        }
        listOf(binding.rbMinute, binding.rbHour, binding.rbDay, binding.rbWeek).forEach {
            it.isEnabled = enabled
        }
        binding.rgReminderUnit.isEnabled = enabled
        binding.layoutNotification.alpha = alpha
    }

    private fun saveEdits() {
        val title = binding.etTitle.text?.toString()?.trim()
        if (title.isNullOrBlank()) {
            Toast.makeText(this, "Tên sự kiện không được để trống", Toast.LENGTH_SHORT).show()
            return
        }

        val zone = ZoneId.systemDefault()
        val startMillis = startDate.atTime(startTime).atZone(zone).toInstant().toEpochMilli()
        val endMillis = endDate.atTime(endTime).atZone(zone).toInstant().toEpochMilli()

        if (endMillis <= startMillis) {
            Toast.makeText(
                this,
                "Thời gian kết thúc phải sau thời gian bắt đầu",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        viewModel.updateEvent(
            title = title,
            startMillis = startMillis,
            endMillis = endMillis,
            location = binding.etLocation.text?.toString()?.trim()?.ifBlank { null },
            description = binding.etDescription.text?.toString()?.trim()?.ifBlank { null },
            reminderMinutes = getReminderMinutes()
        )

        exitEditMode()
        Toast.makeText(this, "Đã lưu", Toast.LENGTH_SHORT).show()
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
}
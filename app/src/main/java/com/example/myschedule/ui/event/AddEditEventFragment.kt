package com.example.myschedule.ui.event

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.myschedule.R
import com.example.myschedule.data.entity.CalendarEvent
import com.example.myschedule.databinding.FragmentAddEditEventBinding
import com.example.myschedule.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class AddEditEventFragment : Fragment() {

    companion object {
        fun newInstance() = AddEditEventFragment()

        private val DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")
    }

    private var _binding: FragmentAddEditEventBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private var startDate: LocalDate = LocalDate.now()
    private var startTime: LocalTime = LocalTime.now().withSecond(0).withNano(0)
    private var endDate: LocalDate = LocalDate.now()
    private var endTime: LocalTime = LocalTime.now().plusHours(1).withSecond(0).withNano(0)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateDateTimeViews()
        setupClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateDateTimeViews() {
        binding.tvStartDate.text = startDate.format(DATE_FMT)
        binding.tvStartTime.text = startTime.format(TIME_FMT)
        binding.tvEndDate.text = endDate.format(DATE_FMT)
        binding.tvEndTime.text = endTime.format(TIME_FMT)
    }

    private fun setupClickListeners() {
        // Toggle cả ngày
        binding.checkboxAllDay.setOnCheckedChangeListener { _, isChecked ->
            binding.tvStartTime.visibility = if (isChecked) View.GONE else View.VISIBLE
            binding.tvEndTime.visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        // Date pickers
        binding.tvStartDate.setOnClickListener { showDatePicker(isStart = true) }
        binding.tvEndDate.setOnClickListener { showDatePicker(isStart = false) }

        // Time pickers
        binding.tvStartTime.setOnClickListener { showTimePicker(isStart = true) }
        binding.tvEndTime.setOnClickListener { showTimePicker(isStart = false) }

        binding.checkboxDisableNotification.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutNotification.visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        // Lưu
        binding.btnSave.setOnClickListener { saveEvent() }
    }

    private fun showDatePicker(isStart: Boolean) {
        val date = if (isStart) startDate else endDate
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val picked = LocalDate.of(year, month + 1, day)
                if (isStart) {
                    startDate = picked
                    // Nếu endDate < startDate thì kéo endDate theo
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
            requireContext(),
            { _, hour, minute ->
                if (isStart) startTime = LocalTime.of(hour, minute)
                else endTime = LocalTime.of(hour, minute)
                updateDateTimeViews()
            },
            time.hour, time.minute, true
        ).show()
    }

    private fun saveEvent() {
        // 3.10 — Validate
        val title = binding.etTitle.text?.toString()?.trim()
        if (title.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Tên sự kiện không được để trống", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(requireContext(), "Thời gian kết thúc phải sau thời gian bắt đầu", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val zone = ZoneId.systemDefault()
        val startMillis = start.atZone(zone).toInstant().toEpochMilli()
        val endMillis = end.atZone(zone).toInstant().toEpochMilli()

        val event = CalendarEvent(
            sourceId = 1, // "Lịch của tôi"
            uid = UUID.randomUUID().toString(),
            title = title,
            startTime = startMillis,
            endTime = endMillis,
            location = binding.etLocation.text?.toString()?.trim()?.ifBlank { null },
            description = binding.etDescription.text?.toString()?.trim()?.ifBlank { null },
            reminderMinutes = getReminderMinutes()
        )

        viewModel.addManualEvent(event)
        Toast.makeText(requireContext(), "Đã lưu sự kiện", Toast.LENGTH_SHORT).show()

        // 3.12 — popBackStack
        parentFragmentManager.popBackStack()
    }

    private fun getReminderMinutes(): Long? {
        if (binding.checkboxDisableNotification.isChecked) return null

        val duration = binding.edNotificationDuration.text
            ?.toString()?.trim()?.toLongOrNull() ?: 0L

        return when (binding.rgReminderUnit.checkedRadioButtonId) {
            R.id.rbHour -> duration * 60
            R.id.rbDay -> duration * 60 * 24
            R.id.rbWeek -> duration * 60 * 24 * 7
            else -> duration // phút
        }
    }
}
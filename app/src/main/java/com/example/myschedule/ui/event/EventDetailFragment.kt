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
import com.example.myschedule.databinding.FragmentEventDetailBinding
import com.example.myschedule.viewmodel.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class EventDetailFragment : Fragment() {

    companion object {
        private const val ARG_EVENT_ID = "event_id"
        private val DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")

        fun newInstance(eventId: Int) = EventDetailFragment().apply {
            arguments = Bundle().apply { putInt(ARG_EVENT_ID, eventId) }
        }
    }

    private var _binding: FragmentEventDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private var currentEvent: CalendarEvent? = null
    private var isEditMode = false

    private var startDate: LocalDate = LocalDate.now()
    private var startTime: LocalTime = LocalTime.now()
    private var endDate: LocalDate = LocalDate.now()
    private var endTime: LocalTime = LocalTime.now().plusHours(1)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val eventId = arguments?.getInt(ARG_EVENT_ID) ?: run {
            parentFragmentManager.popBackStack()
            return
        }

        viewModel.getEventById(eventId).observe(viewLifecycleOwner) { event ->
            if (event == null) {
                parentFragmentManager.popBackStack(); return@observe
            }
            currentEvent = event
            bindEvent(event)
        }

        viewModel.getSourceNameForEvent(eventId).observe(viewLifecycleOwner) { name ->
            binding.tvSource.text = "Nguồn: $name"
        }

        setupClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

        // Xóa đoạn Spinner cũ, thay bằng:
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
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnEdit.setOnClickListener {
            if (!isEditMode) enterEditMode() else saveEdits()
        }

        binding.btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa sự kiện?")
                .setMessage("Sự kiện sẽ bị xóa vĩnh viễn.")
                .setNegativeButton("Hủy") { d, _ -> d.dismiss() }
                .setPositiveButton("Xóa") { _, _ ->
                    currentEvent?.let { viewModel.deleteEvent(it) }
                    parentFragmentManager.popBackStack()
                }
                .show()
        }

        // Date/Time pickers — chỉ active khi edit mode
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

        binding.etTitle.isEnabled = enabled
        binding.etTitle.alpha = alpha

        binding.etLocation.isEnabled = enabled
        binding.etLocation.alpha = alpha

        binding.etDescription.isEnabled = enabled
        binding.etDescription.alpha = alpha

        binding.tvStartDate.isEnabled = enabled
        binding.tvStartDate.alpha = alpha

        binding.tvStartTime.isEnabled = enabled
        binding.tvStartTime.alpha = alpha

        binding.tvEndDate.isEnabled = enabled
        binding.tvEndDate.alpha = alpha

        binding.tvEndTime.isEnabled = enabled
        binding.tvEndTime.alpha = alpha

        binding.checkboxDisableNotification.isEnabled = enabled
        binding.checkboxDisableNotification.alpha = alpha

        binding.edNotificationDuration.isEnabled = enabled
        binding.edNotificationDuration.alpha = alpha

        binding.rgReminderUnit.isEnabled = enabled
        binding.rbMinute.isEnabled = enabled
        binding.rbHour.isEnabled = enabled
        binding.rbDay.isEnabled = enabled
        binding.rbWeek.isEnabled = enabled
        binding.layoutNotification.alpha = alpha
    }

    private fun saveEdits() {
        val event = currentEvent ?: return
        val title = binding.etTitle.text?.toString()?.trim()
        if (title.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Tên sự kiện không được để trống", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val zone = ZoneId.systemDefault()
        val startMillis = startDate.atTime(startTime).atZone(zone).toInstant().toEpochMilli()
        val endMillis = endDate.atTime(endTime).atZone(zone).toInstant().toEpochMilli()

        if (endMillis <= startMillis) {
            Toast.makeText(
                requireContext(),
                "Thời gian kết thúc phải sau thời gian bắt đầu",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val updated = event.copy(
            title = title,
            startTime = startMillis,
            endTime = endMillis,
            location = binding.etLocation.text?.toString()?.trim()?.ifBlank { null },
            description = binding.etDescription.text?.toString()?.trim()?.ifBlank { null },
            reminderMinutes = getReminderMinutes()
        )
        viewModel.updateEvent(updated)
        exitEditMode()
        Toast.makeText(requireContext(), "Đã lưu", Toast.LENGTH_SHORT).show()
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

    private fun showDatePicker(isStart: Boolean) {
        val date = if (isStart) startDate else endDate
        DatePickerDialog(
            requireContext(),
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
            requireContext(),
            { _, hour, minute ->
                if (isStart) startTime = LocalTime.of(hour, minute)
                else endTime = LocalTime.of(hour, minute)
                updateDateTimeViews()
            },
            time.hour, time.minute, true
        ).show()
    }
}
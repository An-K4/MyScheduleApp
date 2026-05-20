package com.example.myschedule.ui.event

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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

        private val REMINDER_OPTIONS = listOf(
            "Tắt thông báo" to null,
            "5 phút trước" to 5L,
            "15 phút trước" to 15L,
            "30 phút trước" to 30L,
            "1 giờ trước" to 60L,
            "1 ngày trước" to 1440L
        )

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

        setupSpinner()

        viewModel.getEventById(eventId).observe(viewLifecycleOwner) { event ->
            if (event == null) {
                parentFragmentManager.popBackStack()
                return@observe
            }
            currentEvent = event
            bindEvent(event)

            // Parse tên nguồn
            viewModel.getSourceName(event.sourceId).observe(viewLifecycleOwner) { name ->
                binding.tvSource.text = "📁 Nguồn: $name"
            }
        }

        setupClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupSpinner() {
        val labels = REMINDER_OPTIONS.map { it.first }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            labels
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerReminder.adapter = adapter
        binding.spinnerReminder.isEnabled = false
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

        // Spinner thông báo — fallback 30 phút nếu null
        val reminderMinutes = event.reminderMinutes ?: 30L
        val idx = REMINDER_OPTIONS.indexOfFirst { it.second == reminderMinutes }
        binding.spinnerReminder.setSelection(if (idx >= 0) idx else 3)
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
    }

    private fun enterEditMode() {
        isEditMode = true
        binding.etTitle.isEnabled = true
        binding.etLocation.isEnabled = true
        binding.etDescription.isEnabled = true
        binding.spinnerReminder.isEnabled = true
        binding.btnEdit.setImageResource(R.drawable.ic_check)
        binding.etTitle.requestFocus()
    }

    private fun exitEditMode() {
        isEditMode = false
        binding.etTitle.isEnabled = false
        binding.etLocation.isEnabled = false
        binding.etDescription.isEnabled = false
        binding.spinnerReminder.isEnabled = false
        binding.btnEdit.setImageResource(R.drawable.ic_edit)
    }

    private fun saveEdits() {
        val event = currentEvent ?: return
        val title = binding.etTitle.text?.toString()?.trim()
        if (title.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Tên sự kiện không được để trống", Toast.LENGTH_SHORT).show()
            return
        }

        val zone = ZoneId.systemDefault()
        val startMillis = startDate.atTime(startTime).atZone(zone).toInstant().toEpochMilli()
        val endMillis = endDate.atTime(endTime).atZone(zone).toInstant().toEpochMilli()

        if (endMillis <= startMillis) {
            Toast.makeText(requireContext(), "Thời gian kết thúc phải sau thời gian bắt đầu", Toast.LENGTH_SHORT).show()
            return
        }

        val reminderMinutes = REMINDER_OPTIONS[binding.spinnerReminder.selectedItemPosition].second

        val updated = event.copy(
            title = title,
            startTime = startMillis,
            endTime = endMillis,
            location = binding.etLocation.text?.toString()?.trim()?.ifBlank { null },
            description = binding.etDescription.text?.toString()?.trim()?.ifBlank { null },
            reminderMinutes = reminderMinutes
        )
        viewModel.updateEvent(updated)
        exitEditMode()
        Toast.makeText(requireContext(), "Đã lưu", Toast.LENGTH_SHORT).show()
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
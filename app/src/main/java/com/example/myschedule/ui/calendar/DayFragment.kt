package com.example.myschedule.ui.calendar

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myschedule.data.entity.CalendarEvent
import com.example.myschedule.databinding.FragmentDayBinding
import com.example.myschedule.ui.event.EventDetailActivity
import com.example.myschedule.util.LunarCalendarUtil
import com.example.myschedule.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class DayFragment : Fragment() {

    private var _binding: FragmentDayBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var eventAdapter: EventAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter()
        eventAdapter.onItemClick = { event -> showEventDetail(event) }
        binding.rvEvents.apply {
            adapter = eventAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        binding.btnPrevDay.setOnClickListener {
            val current = viewModel.selectedDate.value ?: LocalDate.now()
            viewModel.selectDate(current.minusDays(1))
        }

        binding.btnNextDay.setOnClickListener {
            val current = viewModel.selectedDate.value ?: LocalDate.now()
            viewModel.selectDate(current.plusDays(1))
        }
    }

    private fun observeViewModel() {
        viewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            updateHeader(date)
        }

        viewModel.eventsForSelectedDate.observe(viewLifecycleOwner) { events ->
            updateEventList(events)
        }

        viewModel.sourceColors.observe(viewLifecycleOwner) { colors ->
            eventAdapter.updateSourceColors(colors)
        }
    }

    private fun updateHeader(date: LocalDate) {
        val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("vi"))
        val day = date.dayOfMonth
        val month = date.monthValue

        val lunar = LunarCalendarUtil.toLunarDateShort(date)

        // Format: "Thứ Ba, 19 tháng 5 • 15/4 âl"
        binding.tvDayHeader.text = "$dayOfWeek, $day tháng $month"
        binding.tvLunarHeader.text = "Ngày $lunar âm lịch"
    }

    private fun updateEventList(events: List<CalendarEvent>) {
        binding.tvNoEvent.visibility = View.GONE
        binding.rvEvents.visibility = View.GONE

        if (events.isEmpty()) {
            binding.tvNoEvent.visibility = View.VISIBLE
        } else {
            binding.rvEvents.visibility = View.VISIBLE
            eventAdapter.submitList(events)
        }
    }

    private fun showEventDetail(event: CalendarEvent) {
        val intent = Intent(requireContext(), EventDetailActivity::class.java).apply {
            putExtra(EventDetailActivity.Companion.EXTRA_EVENT_ID, event.id)
        }
        startActivity(intent)
    }
}
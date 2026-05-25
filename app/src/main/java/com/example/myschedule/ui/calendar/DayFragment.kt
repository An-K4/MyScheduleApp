package com.example.myschedule.ui.calendar

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
import com.example.myschedule.ui.MainActivity
import com.example.myschedule.util.LunarCalendarUtil
import com.example.myschedule.viewmodel.MainViewModel
import android.content.Intent
import java.time.LocalDate
import java.time.YearMonth
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
            // Đồng bộ currentMonth theo ngày đang xem
            val month = YearMonth.of(date.year, date.month)
            if (viewModel.currentMonth.value != month) {
                viewModel.setCurrentMonth(month)
            }
        }

        viewModel.currentMonth.observe(viewLifecycleOwner) { month ->
            updateToolbarTitle(month)
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
        binding.tvDayHeader.text = "$dayOfWeek, ${date.dayOfMonth} tháng ${date.monthValue}"
        binding.tvLunarHeader.text = "Ngày ${LunarCalendarUtil.toLunarDateShort(date)} âm lịch"
    }

    private fun updateToolbarTitle(month: YearMonth) {
        val monthName = month.month.getDisplayName(TextStyle.FULL, Locale("vi"))
            .replaceFirstChar { it.titlecase(Locale("vi")) }
        (activity as? MainActivity)?.updateMonthYearTitle(monthName)
    }

    private fun updateEventList(events: List<CalendarEvent>) {
        if (events.isEmpty()) {
            binding.tvNoEvent.visibility = View.VISIBLE
            binding.rvEvents.visibility = View.GONE
        } else {
            binding.tvNoEvent.visibility = View.GONE
            binding.rvEvents.visibility = View.VISIBLE
            eventAdapter.submitList(events)
        }
    }

    private fun showEventDetail(event: CalendarEvent) {
        val intent = Intent(requireContext(), EventDetailActivity::class.java).apply {
            putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.id)
        }
        startActivity(intent)
    }
}
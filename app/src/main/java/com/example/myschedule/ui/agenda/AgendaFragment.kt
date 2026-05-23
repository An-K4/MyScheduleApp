package com.example.myschedule.ui.agenda

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myschedule.data.entity.CalendarEvent
import com.example.myschedule.databinding.FragmentAgendaBinding
import com.example.myschedule.ui.event.EventDetailFragment
import com.example.myschedule.ui.main.MainActivity
import com.example.myschedule.viewmodel.MainViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class AgendaFragment : Fragment() {

    private var _binding: FragmentAgendaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var yearAdapter: YearAdapter
    private lateinit var agendaAdapter: AgendaAdapter

    private var currentYear: Int = LocalDate.now().year

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAgendaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupYearSelector()
        setupAgendaList()
        setupClickListeners()
        observeViewModel()
        loadAgendaForYear(currentYear)
        scrollYearToCenter(currentYear)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupYearSelector() {
        yearAdapter = YearAdapter { year ->
            currentYear = year
            loadAgendaForYear(year)
            yearAdapter.submitData(generateYearList(), currentYear)
        }

        binding.rvYears.apply {
            adapter = yearAdapter
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
        }

        yearAdapter.submitData(generateYearList(), currentYear)
    }

    private fun setupAgendaList() {
        agendaAdapter = AgendaAdapter { item ->
            if (item.type == AgendaItem.TYPE_EVENT) {
                item.event?.let { event ->
                    (activity as? MainActivity)?.switchTab(
                        EventDetailFragment.newInstance(event.id),
                        "EVENT_DETAIL"
                    )
                }
            }
        }

        binding.rvAgenda.apply {
            adapter = agendaAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        binding.btnPrevYear.setOnClickListener {
            currentYear--
            loadAgendaForYear(currentYear)
            yearAdapter.submitData(generateYearList(), currentYear)
            scrollYearToCenter(currentYear)
        }

        binding.btnNextYear.setOnClickListener {
            currentYear++
            loadAgendaForYear(currentYear)
            yearAdapter.submitData(generateYearList(), currentYear)
            scrollYearToCenter(currentYear)
        }
    }

    private fun observeViewModel() {
        // Observe sourceColors riêng để update màu khi source thay đổi
        viewModel.sourceColors.observe(viewLifecycleOwner) { colors ->
            // Chỉ update màu, không rebuild toàn bộ list
            agendaAdapter.updateColors(colors)
        }

        viewModel.currentMonth.observe(viewLifecycleOwner) { month ->
            if (month.year != currentYear) {
                currentYear = month.year
                loadAgendaForYear(currentYear)
                yearAdapter.submitData(generateYearList(), currentYear)
                scrollYearToCenter(currentYear)
            }
        }
    }

    private fun generateYearList(): List<Int> {
        // Tạo range năm: hiện tại -10 đến +10
        return (currentYear - 10..currentYear + 10).toList()
    }

    private fun loadAgendaForYear(year: Int) {
        val zone = ZoneId.systemDefault()
        val yearStart = YearMonth.of(year, 1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val yearEnd = YearMonth.of(year, 12).atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

        // Observe cả 2 LiveData cùng lúc, không lồng nhau
        viewModel.getEventsForYearRange(yearStart, yearEnd).observe(viewLifecycleOwner) { events ->
            updateAgendaUI(events, yearStart, yearEnd, zone)
        }
    }

    private fun updateAgendaUI(
        events: List<CalendarEvent>,
        yearStart: Long,
        yearEnd: Long,
        zone: ZoneId
    ) {
        if (events.isEmpty()) {
            binding.tvNoEvent.visibility = View.VISIBLE
            binding.rvAgenda.visibility = View.GONE
            return
        }

        binding.tvNoEvent.visibility = View.GONE
        binding.rvAgenda.visibility = View.VISIBLE

        val agendaItems = buildAgendaItems(events, yearStart, yearEnd, zone)
        val colors = viewModel.sourceColors.value ?: emptyMap()

        agendaAdapter.submitData(agendaItems, colors)

        // Auto scroll đến hôm nay (chỉ scroll 1 lần)
        if (agendaItems.isNotEmpty()) {
            val todayItem = AgendaItem(AgendaItem.TYPE_DATE_HEADER, LocalDate.now())
            val todayPosition = agendaItems.indexOf(todayItem)
            if (todayPosition >= 0) {
                binding.rvAgenda.post {
                    (binding.rvAgenda.layoutManager as? LinearLayoutManager)
                        ?.scrollToPositionWithOffset(todayPosition, 0)
                }
            }
        }
    }

    private fun buildAgendaItems(
        events: List<CalendarEvent>,
        yearStart: Long,
        yearEnd: Long,
        zone: ZoneId
    ): List<AgendaItem> {
        val yearStartDate = Instant.ofEpochMilli(yearStart).atZone(zone).toLocalDate()
        val yearEndDate = Instant.ofEpochMilli(yearEnd).atZone(zone).toLocalDate()
        val grouped = mutableMapOf<LocalDate, MutableList<AgendaItem>>()

        events.forEach { event ->
            val eventStart = Instant.ofEpochMilli(event.startTime).atZone(zone).toLocalDate()
            val eventEnd = Instant.ofEpochMilli(event.endTime).atZone(zone).toLocalDate()

            // Chỉ render ngày nằm trong [yearStartDate, yearEndDate]
            val rangeStart = maxOf(eventStart, yearStartDate)
            val rangeEnd = minOf(eventEnd, yearEndDate)

            var cursor = rangeStart
            while (!cursor.isAfter(rangeEnd)) {
                val displayStart = when (cursor) {
                    eventStart -> event.startTime
                    else -> cursor.atStartOfDay(zone).toInstant().toEpochMilli()
                }

                val displayEnd = when (cursor) {
                    eventEnd -> event.endTime
                    else -> cursor.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
                }

                grouped.getOrPut(cursor) { mutableListOf() }.add(
                    AgendaItem(
                        type = AgendaItem.TYPE_EVENT,
                        event = event,
                        displayStart = displayStart,
                        displayEnd = displayEnd
                    )
                )

                cursor = cursor.plusDays(1)
            }
        }

        // Build flat list: header + events
        return buildList {
            grouped.toSortedMap().forEach { (date, eventItems) ->
                add(AgendaItem(AgendaItem.TYPE_DATE_HEADER, date))
                addAll(eventItems.sortedBy { it.displayStart })
            }
        }
    }

    private fun scrollYearToCenter(year: Int) {
        val years = generateYearList()
        val position = years.indexOf(year)
        if (position >= 0) {
            binding.rvYears.post {
                val layoutManager = binding.rvYears.layoutManager as? LinearLayoutManager
                val firstChild = binding.rvYears.getChildAt(0)
                if (firstChild != null && layoutManager != null) {
                    val itemWidth = firstChild.width
                    val offset = binding.rvYears.width / 2 - itemWidth / 2
                    layoutManager.scrollToPositionWithOffset(position, offset)
                }
            }
        }
    }
}
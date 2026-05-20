package com.example.myschedule.ui.month

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myschedule.R
import com.example.myschedule.data.entity.CalendarEvent
import com.example.myschedule.databinding.CalendarDayLayoutBinding
import com.example.myschedule.databinding.CalendarHeaderLayoutBinding
import com.example.myschedule.databinding.FragmentMonthBinding
import com.example.myschedule.ui.main.EventAdapter
import com.example.myschedule.ui.main.MainActivity
import com.example.myschedule.viewmodel.MainViewModel
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class MonthFragment : Fragment() {

    companion object {
        private const val DOT_SIZE_DP = 5
        private const val DOT_MARGIN_DP = 2
        private const val MAX_DOTS = 4
    }

    private var _binding: FragmentMonthBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var eventAdapter: EventAdapter

    private var eventDateColorsCache: Map<LocalDate, List<Int>> = emptyMap()
    private var selectedDate: LocalDate = LocalDate.now()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMonthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupCalendar()
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

    private fun observeViewModel() {
        viewModel.eventDateColors.observe(viewLifecycleOwner) { dateColors ->
            eventDateColorsCache = dateColors
            binding.calendarView.notifyCalendarChanged()
        }

        viewModel.sourceColors.observe(viewLifecycleOwner) { colors ->
            eventAdapter.updateSourceColors(colors)
        }

        viewModel.eventsForSelectedDate.observe(viewLifecycleOwner) { events ->
            updateEventList(events)
        }

        viewModel.selectedDate.observe(viewLifecycleOwner) { newDate ->
            val oldDate = selectedDate
            selectedDate = newDate
            binding.calendarView.notifyDateChanged(newDate)
            if (oldDate != newDate) binding.calendarView.notifyDateChanged(oldDate)
        }
    }

    // ── Calendar ──────────────────────────────────────────────────────────────

    inner class DayViewContainer(view: View) : ViewContainer(view) {
        val dayBinding = CalendarDayLayoutBinding.bind(view)
        lateinit var day: CalendarDay
        init {
            view.setOnClickListener {
                if (day.position == DayPosition.MonthDate && day.date != selectedDate)
                    viewModel.selectDate(day.date)
            }
        }
    }

    class MonthViewContainer(view: View) : ViewContainer(view) {
        val titlesContainer = CalendarHeaderLayoutBinding.bind(view)
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    private fun setupCalendar() {
        val currentMonth = YearMonth.now()
        binding.calendarView.setup(
            currentMonth.minusMonths(100),
            currentMonth.plusMonths(100),
            daysOfWeek(firstDayOfWeek = DayOfWeek.MONDAY).first()
        )
        binding.calendarView.scrollToMonth(viewModel.currentMonth.value ?: currentMonth)

        binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data
                val textView = container.dayBinding.tvDayText
                val dotsContainer = container.dayBinding.dotsContainer
                val rootView = container.view

                textView.text = data.date.dayOfMonth.toString()

                if (data.position == DayPosition.MonthDate) {
                    textView.visibility = View.VISIBLE
                    renderEventDots(dotsContainer, data.date)

                    if (data.date == selectedDate) {
                        rootView.setBackgroundResource(R.drawable.selected_day_background)
                        textView.setTextColor(resources.getColor(R.color.white, null))
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
                    dotsContainer.removeAllViews()
                }
            }
        }

        binding.calendarView.monthScrollListener = { month ->
            viewModel.setCurrentMonth(month.yearMonth)
            val monthName = month.yearMonth.month
                .getDisplayName(TextStyle.FULL, Locale("vi"))
            val title = "${monthName.replaceFirstChar { it.titlecase(Locale("vi")) }} ${month.yearMonth.year}"
            (activity as? MainActivity)?.updateMonthYearTitle(title)
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

        if (viewModel.selectedDate.value == null) viewModel.selectDate(LocalDate.now())
    }

    private fun renderEventDots(container: android.widget.LinearLayout, date: LocalDate) {
        container.removeAllViews()
        val colors = eventDateColorsCache[date] ?: return
        val density = resources.displayMetrics.density
        val sizePx = (DOT_SIZE_DP * density).toInt()
        val marginPx = (DOT_MARGIN_DP * density).toInt()
        colors.take(MAX_DOTS).forEach { color ->
            val dot = View(requireContext()).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                }
                layoutParams = android.widget.LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    setMargins(marginPx, 0, marginPx, 0)
                }
            }
            container.addView(dot)
        }
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────

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
        // 2.5 — Tạm thời dùng Dialog, Phase 4 sẽ thay bằng EventDetailFragment
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(event.title)
            .setMessage(buildString {
                if (!event.location.isNullOrBlank()) appendLine("📍 ${event.location}\n")
                append(event.description ?: "Không có mô tả chi tiết.")
            })
            .setPositiveButton("Đóng") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
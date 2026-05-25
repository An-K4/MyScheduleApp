package com.example.myschedule.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myschedule.R
import com.example.myschedule.databinding.FragmentYearBinding
import com.example.myschedule.ui.MainActivity
import com.example.myschedule.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class YearFragment : Fragment() {

    private var _binding: FragmentYearBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: MiniMonthAdapter

    private var currentYear: Int = LocalDate.now().year

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentYearBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        updateHeader()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        adapter = MiniMonthAdapter { month ->
            viewModel.setCurrentMonth(month)
            (activity as? MainActivity)?.switchTab(MonthFragment(), "MONTH")
            (activity as? MainActivity)?.binding?.navigationView?.setCheckedItem(R.id.nav_month)
        }
        binding.rvMonths.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            this.adapter = this@YearFragment.adapter
        }
    }

    private fun setupClickListeners() {
        binding.btnPrevYear.setOnClickListener {
            currentYear--
            updateHeader()
            refreshData()
        }
        binding.btnNextYear.setOnClickListener {
            currentYear++
            updateHeader()
            refreshData()
        }
    }

    private fun observeViewModel() {
        viewModel.scrollToToday.observe(viewLifecycleOwner) { shouldScroll ->
            if (shouldScroll) {
                currentYear = LocalDate.now().year
                updateHeader()
                refreshData()
                viewModel.clearScrollToToday()
            }
        }

        viewModel.eventDates.observe(viewLifecycleOwner) {
            refreshData()
        }

        viewModel.currentMonth.observe(viewLifecycleOwner) { month ->
            // Cập nhật toolbar title
            updateToolbarTitle(month)
            // Cập nhật năm nếu cần
            if (month.year != currentYear) {
                currentYear = month.year
                updateHeader()
                refreshData()
            }
        }
    }

    private fun updateHeader() {
        binding.tvYearHeader.text = "Năm $currentYear"
    }

    private fun updateToolbarTitle(month: YearMonth) {
        val monthName = month.month.getDisplayName(TextStyle.FULL, Locale("vi"))
            .replaceFirstChar { it.titlecase(Locale("vi")) }
        (activity as? MainActivity)?.updateMonthYearTitle(monthName)
    }

    private fun refreshData() {
        val eventDates = viewModel.eventDates.value ?: emptySet()
        adapter.submitData(
            year = currentYear,
            currentMonth = viewModel.currentMonth.value ?: YearMonth.now(),
            eventDates = eventDates
        )
    }
}
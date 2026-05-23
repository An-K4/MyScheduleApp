package com.example.myschedule.ui.year

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myschedule.R
import com.example.myschedule.databinding.FragmentYearBinding
import com.example.myschedule.ui.main.MainActivity
import com.example.myschedule.ui.month.MonthFragment
import com.example.myschedule.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.YearMonth

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
            // 6.5 — Click tháng → chuyển MonthFragment + scroll tới tháng đó
            viewModel.setCurrentMonth(month)
            (activity as? MainActivity)?.switchTab(MonthFragment(), "MONTH")
            (activity as? MainActivity)?.binding?.navigationView?.setCheckedItem(R.id.nav_month)
        }

        binding.rvMonths.apply {
            layoutManager = GridLayoutManager(requireContext(), 3) // 3 cột
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
        viewModel.eventDates.observe(viewLifecycleOwner) { dates ->
            refreshData()
        }

        viewModel.currentMonth.observe(viewLifecycleOwner) { month ->
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

    private fun refreshData() {
        val eventDates = viewModel.eventDates.value ?: emptySet()
        adapter.submitData(
            year = currentYear,
            currentMonth = viewModel.currentMonth.value ?: YearMonth.now(),
            eventDates = eventDates
        )
    }
}
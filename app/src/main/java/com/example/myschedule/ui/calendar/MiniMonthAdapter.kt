package com.example.myschedule.ui.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.example.myschedule.databinding.ItemMiniMonthBinding
import java.time.LocalDate
import java.time.YearMonth

class MiniMonthAdapter(
    private val onMonthClick: (YearMonth) -> Unit
) : RecyclerView.Adapter<MiniMonthAdapter.MonthViewHolder>() {

    private var months: List<YearMonth> = emptyList()
    private var currentMonth: YearMonth = YearMonth.now()
    private var today: LocalDate = LocalDate.now()
    private var eventDates: Set<LocalDate> = emptySet()

    inner class MonthViewHolder(private val binding: ItemMiniMonthBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(month: YearMonth) {
            binding.tvMonthName.text = "Tháng ${month.monthValue}"

            if (month == currentMonth) {
                binding.cardMonth.strokeWidth = 4
                binding.cardMonth.strokeColor = "#FFA500".toColorInt()
            } else {
                binding.cardMonth.strokeWidth = 0
            }

            binding.cardMonth.setOnClickListener { onMonthClick(month) }

            binding.miniMonthView.bind(month, today, eventDates)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        MonthViewHolder(
            ItemMiniMonthBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) =
        holder.bind(months[position])

    override fun getItemCount() = months.size

    fun submitData(year: Int, currentMonth: YearMonth, eventDates: Set<LocalDate>) {
        this.months = (1..12).map { YearMonth.of(year, it) }
        this.currentMonth = currentMonth
        this.eventDates = eventDates
        notifyDataSetChanged()
    }
}
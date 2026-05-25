package com.example.myschedule.ui.calendar

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myschedule.databinding.ItemMiniMonthBinding
import com.google.android.material.color.MaterialColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import androidx.core.graphics.toColorInt
import com.google.android.material.R

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
            val monthName = month.month.getDisplayName(TextStyle.FULL, Locale("vi"))
            binding.tvMonthName.text = "Tháng ${month.monthValue}"

            // Highlight tháng hiện tại
            if (month == currentMonth) {
                binding.cardMonth.strokeWidth = 4
                binding.cardMonth.strokeColor = 0xFFFF9800.toInt() // Orange
            } else {
                binding.cardMonth.strokeWidth = 0
            }

            binding.cardMonth.setOnClickListener {
                onMonthClick(month)
            }

            renderMiniCalendar(binding.gridDays, month)
        }

        private fun renderMiniCalendar(grid: GridLayout, month: YearMonth) {
            grid.removeAllViews()

            val firstDay = month.atDay(1)
            val firstDayOfWeek = firstDay.dayOfWeek.value // 1 = Monday
            val daysInMonth = month.lengthOfMonth()

            val cellSize = (grid.width / 7).coerceAtLeast(40)

            // Thêm cell trống trước ngày 1
            for (i in 1 until firstDayOfWeek) {
                grid.addView(createEmptyCell(cellSize))
            }

            // Thêm các ngày trong tháng
            for (day in 1..daysInMonth) {
                val date = month.atDay(day)
                grid.addView(createDayCell(date, cellSize))
            }
        }

        private fun createEmptyCell(size: Int): View {
            return View(itemView.context).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                }
            }
        }

        private fun createDayCell(date: LocalDate, size: Int): TextView {
            return TextView(itemView.context).apply {
                text = date.dayOfMonth.toString()
                gravity = Gravity.CENTER
                textSize = 10f

                // Màu chữ
                setTextColor(
                    if (date.dayOfWeek == DayOfWeek.SUNDAY)
                        "#E53935".toColorInt() // Đỏ cho CN
                    else
                        MaterialColors.getColor(
                            itemView.context,
                            R.attr.colorOnBackground,
                            Color.BLACK
                        )
                )

                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(1, 1, 1, 1)
                }

                // Highlight ngày hôm nay
                if (date == today) {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor("#FFA500".toColorInt()) // Orange
                    }
                    setTextColor(Color.WHITE)
                }
                // Chấm màu nếu có sự kiện
                else if (eventDates.contains(date)) {
                    compoundDrawablePadding = 0
                    val dot = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor("#03DAC5".toColorInt()) // Teal
                        setSize(6, 6)
                    }
                    setCompoundDrawablesWithIntrinsicBounds(null, null, null, dot)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        MonthViewHolder(
            ItemMiniMonthBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) =
        holder.bind(months[position])

    override fun getItemCount() = months.size

    fun submitData(
        year: Int,
        currentMonth: YearMonth,
        eventDates: Set<LocalDate>
    ) {
        this.months = (1..12).map { YearMonth.of(year, it) }
        this.currentMonth = currentMonth
        this.eventDates = eventDates
        notifyDataSetChanged()
    }
}
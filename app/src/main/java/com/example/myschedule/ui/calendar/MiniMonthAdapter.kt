package com.example.myschedule.ui.calendar

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.example.myschedule.databinding.ItemMiniMonthBinding
import com.google.android.material.color.MaterialColors
import java.time.DayOfWeek
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

            binding.gridDays.post {
                val gridWidth = binding.gridDays.width
                if (gridWidth > 0) {
                    renderMiniCalendar(binding.gridDays, month, gridWidth)
                }
            }
        }

        private fun renderMiniCalendar(grid: GridLayout, month: YearMonth, gridWidth: Int) {
            grid.removeAllViews()

            val cellSize = gridWidth / 7
            val scaledDensity = grid.resources.displayMetrics.scaledDensity
            val textSizeSp = (cellSize * 0.55f) / scaledDensity

            val firstDayOfWeek = month.atDay(1).dayOfWeek.value
            val daysInMonth = month.lengthOfMonth()

            repeat(firstDayOfWeek - 1) {
                grid.addView(createEmptyCell(cellSize))
            }

            for (day in 1..daysInMonth) {
                grid.addView(createDayCell(month.atDay(day), cellSize, textSizeSp))
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

        private fun createDayCell(date: LocalDate, size: Int, textSizeSp: Float): TextView {
            val isToday = date == today
            val hasEvent = eventDates.contains(date)

            return TextView(itemView.context).apply {
                text = date.dayOfMonth.toString()
                gravity = android.view.Gravity.CENTER
                textSize = textSizeSp
                includeFontPadding = false

                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(1, 1, 1, 1)
                }

                // Nền vàng cho ngày hôm nay — độc lập với chấm sự kiện
                if (isToday) {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor("#FFA500".toColorInt())
                    }
                    setTextColor(Color.WHITE)
                } else {
                    setTextColor(getTextColor(date))
                }

                // Chấm sự kiện — vẽ độc lập, không bị block bởi isToday
                if (hasEvent) {
                    val dotSize = (size * 0.12f).toInt().coerceAtLeast(4)
                    val dot = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor("#03DAC5".toColorInt())
                        setSize(dotSize, dotSize)
                    }
                    setCompoundDrawablesWithIntrinsicBounds(null, null, null, dot)
                    compoundDrawablePadding = 0
                }
            }
        }

        private fun getTextColor(date: LocalDate): Int {
            return if (date.dayOfWeek == DayOfWeek.SUNDAY) {
                "#E53935".toColorInt()
            } else {
                MaterialColors.getColor(
                    itemView.context,
                    com.google.android.material.R.attr.colorOnBackground,
                    Color.BLACK
                )
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

    fun submitData(year: Int, currentMonth: YearMonth, eventDates: Set<LocalDate>) {
        this.months = (1..12).map { YearMonth.of(year, it) }
        this.currentMonth = currentMonth
        this.eventDates = eventDates
        notifyDataSetChanged()
    }
}
package com.example.myschedule.ui.agenda

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myschedule.R
import com.example.myschedule.databinding.ItemYearBinding

class YearAdapter(
    private val onYearClick: (Int) -> Unit
) : RecyclerView.Adapter<YearAdapter.YearViewHolder>() {

    private var years: List<Int> = emptyList()
    private var selectedYear: Int = 0

    inner class YearViewHolder(private val binding: ItemYearBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(year: Int) {
            binding.tvYear.text = year.toString()

            // Highlight năm được chọn
            if (year == selectedYear) {
                binding.cardYear.strokeColor = ContextCompat.getColor(
                    itemView.context,
                    R.color.selected_day_background
                )
                binding.cardYear.strokeWidth = 4
            } else {
                binding.cardYear.strokeColor = android.graphics.Color.TRANSPARENT
                binding.cardYear.strokeWidth = 2
            }

            binding.root.setOnClickListener {
                onYearClick(year)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        YearViewHolder(
            ItemYearBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: YearViewHolder, position: Int) =
        holder.bind(years[position])

    override fun getItemCount() = years.size

    fun submitData(yearList: List<Int>, selected: Int) {
        years = yearList
        selectedYear = selected
        notifyDataSetChanged()
    }
}
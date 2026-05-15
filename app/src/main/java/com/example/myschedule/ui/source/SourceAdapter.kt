package com.example.myschedule.ui.source

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myschedule.data.entity.CalendarSource
import com.example.myschedule.databinding.SourceItemLayoutBinding

class SourceAdapter(
    private val onToggle: (CalendarSource, Boolean) -> Unit,
    private val onDelete: (CalendarSource) -> Unit
) : ListAdapter<CalendarSource, SourceAdapter.SourceViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CalendarSource>() {
            override fun areItemsTheSame(old: CalendarSource, new: CalendarSource) =
                old.id == new.id
            override fun areContentsTheSame(old: CalendarSource, new: CalendarSource) =
                old == new
        }
    }

    inner class SourceViewHolder(private val binding: SourceItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(source: CalendarSource) {
            // Màu nguồn lịch làm màu nền CardView
            binding.cardSource.setCardBackgroundColor(source.color)

            // Chữ trắng để đọc được trên mọi màu nền
            binding.tvSourceName.setTextColor(android.graphics.Color.WHITE)
            binding.tvSourceUri.setTextColor(android.graphics.Color.WHITE)
            binding.btnDeleteSource.setColorFilter(android.graphics.Color.WHITE)

            binding.tvSourceName.text = source.name
            binding.tvSourceUri.text = source.uri
                .substringAfterLast('/')
                .ifBlank { source.uri }

            binding.checkboxEnabled.setOnCheckedChangeListener(null)
            binding.checkboxEnabled.isChecked = source.isEnabled
            binding.checkboxEnabled.setOnCheckedChangeListener { _, isChecked ->
                onToggle(source, isChecked)
            }

            binding.btnDeleteSource.setOnClickListener {
                onDelete(source)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        SourceViewHolder(
            SourceItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: SourceViewHolder, position: Int) =
        holder.bind(getItem(position))
}
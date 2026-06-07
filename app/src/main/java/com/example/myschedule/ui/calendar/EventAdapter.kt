package com.example.myschedule.ui.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myschedule.R
import com.example.myschedule.data.entity.CalendarEvent
import com.example.myschedule.databinding.EventItemLayoutBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class EventAdapter : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private var events: List<CalendarEvent> = emptyList()

    // Map sourceId → color (ARGB Int), cập nhật từ ViewModel
    private var sourceColors: Map<Int, Int> = emptyMap()

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    var onItemClick: ((CalendarEvent) -> Unit)? = null

    inner class EventViewHolder(private val binding: EventItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(events[bindingAdapterPosition])
                }
            }
        }

        fun bind(event: CalendarEvent) {
            binding.tvEventTitle.text = event.title

            val zone = ZoneId.systemDefault()
            val startText = Instant.ofEpochMilli(event.startTime)
                .atZone(zone).toLocalTime().format(timeFormatter)
            val endText = Instant.ofEpochMilli(event.endTime)
                .atZone(zone).toLocalTime().format(timeFormatter)

            val timeStr = "$startText - $endText"
            binding.tvEventTime.text = if (!event.location.isNullOrBlank()) {
                "$timeStr       ${event.location}"
            } else {
                timeStr
            }

            // Thanh màu dọc lấy màu từ source, fallback về màu mặc định nếu không tìm thấy
            val color = sourceColors[event.sourceId]
            if (color != null) {
                binding.vEventColor.setBackgroundColor(color)
            } else {
                binding.vEventColor.setBackgroundResource(
                    R.color.selected_day_background
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        EventViewHolder(
            EventItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) =
        holder.bind(events[position])

    override fun getItemCount() = events.size

    fun submitList(newEvents: List<CalendarEvent>) {
        events = newEvents
        notifyDataSetChanged()
    }

    // Gọi từ Activity khi danh sách source thay đổi
    fun updateSourceColors(colors: Map<Int, Int>) {
        sourceColors = colors
        notifyDataSetChanged()
    }
}
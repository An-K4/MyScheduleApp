package com.example.myschedule.ui.agenda

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myschedule.databinding.ItemAgendaEventBinding
import com.example.myschedule.databinding.ItemAgendaHeaderBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class AgendaAdapter(
    private val onItemClick: (AgendaItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<AgendaItem> = emptyList()
    private var sourceColors: Map<Int, Int> = emptyMap()

    companion object {
        private val DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")
    }

    inner class HeaderViewHolder(private val binding: ItemAgendaHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AgendaItem) {
            val date = item.date ?: return
            val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("vi"))
            binding.tvDateHeader.text = "$dayOfWeek, ${date.format(DATE_FMT)}"
        }
    }

    inner class EventViewHolder(private val binding: ItemAgendaEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AgendaItem) {
            val event = item.event ?: return
            val zone = ZoneId.systemDefault()

            val startText = Instant.ofEpochMilli(item.displayStart)
                .atZone(zone).toLocalTime().format(TIME_FMT)
            val endText = Instant.ofEpochMilli(item.displayEnd)
                .atZone(zone).toLocalTime().format(TIME_FMT)

            binding.tvEventTime.text = "$startText - $endText"
            binding.tvEventTitle.text = event.title
            binding.tvEventLocation.text = event.location ?: "Không có địa điểm"

            // Màu thanh dọc theo nguồn
            val color = sourceColors[event.sourceId]
            if (color != null) {
                binding.vEventColor.setBackgroundColor(color)
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun getItemViewType(position: Int) = items[position].type

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == AgendaItem.TYPE_DATE_HEADER) {
            HeaderViewHolder(
                ItemAgendaHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        } else {
            EventViewHolder(
                ItemAgendaEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.bind(items[position])
            is EventViewHolder -> holder.bind(items[position])
        }
    }

    override fun getItemCount() = items.size

    fun submitData(newItems: List<AgendaItem>, colors: Map<Int, Int>) {
        items = newItems
        sourceColors = colors
        notifyDataSetChanged()
    }

    fun updateColors(colors: Map<Int, Int>) {
        sourceColors = colors
        notifyDataSetChanged()
    }
}
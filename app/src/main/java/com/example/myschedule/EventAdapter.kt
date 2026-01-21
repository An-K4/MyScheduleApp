package com.example.myschedule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myschedule.databinding.EventItemLayoutBinding
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.DtEnd
import net.fortuna.ical4j.model.property.DtStart
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import net.fortuna.ical4j.model.property.Location

class EventAdapter : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private var events: List<VEvent> = emptyList()
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // Đây là một biến kiểu hàm, MainActivity sẽ gán hành động cho nó.
    // Nhận vào một VEvent (sự kiện được click) và không trả về gì (Unit).
    var onItemClick: ((VEvent) -> Unit)? = null


    inner class EventViewHolder(private val binding: EventItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Gán sự kiện click cho toàn bộ view của item
            binding.root.setOnClickListener {
                // Kiểm tra xem vị trí có hợp lệ không
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    // Gọi hàm onItemClick, truyền vào sự kiện ở vị trí đã được click
                    onItemClick?.invoke(events[adapterPosition])
                }
            }
        }

        fun bind(event: VEvent) {
            binding.tvEventTitle.text = event.summary?.value ?: "Sự kiện không có tên"

            val location = event.getProperty<Location>("LOCATION")?.value

            val dtStart = event.getProperty<DtStart>("DTSTART")
            val dtEnd = event.getProperty<DtEnd>("DTEND")
            val startTime = dtStart?.date?.toLocalTimeText() ?: ""
            val endTime = dtEnd?.date?.toLocalTimeText() ?: ""

            val time = if (startTime.isNotEmpty() || endTime.isNotEmpty()) {
                "$startTime - $endTime"
            } else {
                "Cả ngày"
            }

            binding.tvEventTime.text = if (!location.isNullOrBlank()) {
                // Nếu có địa điểm diễn ra sự kiện thì nối chuỗi theo định dạng "Thời gian     Địa chỉ"
                "$time       $location"
            } else {
                time
            }
        }

        private fun Date.toLocalTimeText(): String {
            return this.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
                .format(timeFormatter)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding =
            EventItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size

    fun submitList(newEvents: List<VEvent>) {
        events = newEvents
        notifyDataSetChanged()
    }

}
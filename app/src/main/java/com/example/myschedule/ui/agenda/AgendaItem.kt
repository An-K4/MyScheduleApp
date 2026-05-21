package com.example.myschedule.ui.agenda

import com.example.myschedule.data.entity.CalendarEvent
import java.time.LocalDate

data class AgendaItem(
    val type: Int,
    val date: LocalDate? = null,        // Dùng cho TYPE_DATE_HEADER
    val event: CalendarEvent? = null,   // Dùng cho TYPE_EVENT
    val displayStart: Long = 0,         // Giờ bắt đầu hiển thị (đã điều chỉnh cho multi-day)
    val displayEnd: Long = 0            // Giờ kết thúc hiển thị
) {
    companion object {
        const val TYPE_DATE_HEADER = 0
        const val TYPE_EVENT = 1
    }

    // Override equals để indexOf() hoạt động (auto scroll đến hôm nay)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AgendaItem) return false
        return type == other.type && date == other.date
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + (date?.hashCode() ?: 0)
        return result
    }
}
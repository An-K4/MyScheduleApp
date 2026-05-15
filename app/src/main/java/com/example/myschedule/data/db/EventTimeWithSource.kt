package com.example.myschedule.data.db

import androidx.room.ColumnInfo

// Projection nhỏ gọn: chỉ lấy startTime + sourceId để vẽ dấu chấm trên lịch
data class EventTimeWithSource(
    @ColumnInfo(name = "startTime") val startTime: Long,
    @ColumnInfo(name = "sourceId") val sourceId: Int
)
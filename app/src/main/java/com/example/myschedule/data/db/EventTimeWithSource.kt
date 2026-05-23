package com.example.myschedule.data.db

import androidx.room.ColumnInfo

data class EventTimeWithSource(
    @ColumnInfo(name = "startTime") val startTime: Long,
    @ColumnInfo(name = "endTime") val endTime: Long,
    @ColumnInfo(name = "sourceId") val sourceId: Int
)
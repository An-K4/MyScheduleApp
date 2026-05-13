package com.example.myschedule.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_sources")
data class CalendarSource(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val uri: String,
    val color: Int,
    val isEnabled: Boolean = true
)
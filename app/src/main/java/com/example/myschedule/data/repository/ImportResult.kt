package com.example.myschedule.data.repository

import com.example.myschedule.data.entity.CalendarSource

sealed class ImportResult {
    data class Success(val source: CalendarSource, val eventCount: Int) : ImportResult()
    data class Duplicate(val existingSource: CalendarSource) : ImportResult()
    data class Error(val message: String) : ImportResult()
}
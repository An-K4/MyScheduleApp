package com.example.myschedule.data.db

import androidx.room.*
import com.example.myschedule.data.entity.CalendarEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<CalendarEvent>)

    @Query("DELETE FROM calendar_events WHERE sourceId = :sourceId")
    suspend fun deleteBySourceId(sourceId: Int)

    @Query("""
        SELECT e.* FROM calendar_events e
        INNER JOIN calendar_sources s ON e.sourceId = s.id
        WHERE s.isEnabled = 1
        AND e.startTime < :dayEnd
        AND e.endTime > :dayStart
        ORDER BY e.startTime ASC
    """)
    fun getEventsForDay(dayStart: Long, dayEnd: Long): Flow<List<CalendarEvent>>

    // 6.3 — Trả startTime + sourceId để ViewModel map sang màu sắc
    @Query("""
        SELECT e.startTime, e.sourceId FROM calendar_events e
        INNER JOIN calendar_sources s ON e.sourceId = s.id
        WHERE s.isEnabled = 1
    """)
    fun getEnabledEventStartTimes(): Flow<List<EventTimeWithSource>>

    @Query("SELECT * FROM calendar_events WHERE sourceId = :sourceId")
    suspend fun getEventsBySourceId(sourceId: Int): List<CalendarEvent>
}
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

    // Lấy events trong 1 ngày cụ thể, chỉ từ các source đang bật
    @Query("""
        SELECT e.* FROM calendar_events e
        INNER JOIN calendar_sources s ON e.sourceId = s.id
        WHERE s.isEnabled = 1
        AND e.startTime >= :dayStart
        AND e.startTime < :dayEnd
        ORDER BY e.startTime ASC
    """)
    fun getEventsForDay(dayStart: Long, dayEnd: Long): Flow<List<CalendarEvent>>

    // Lấy tất cả startTime của events từ sources đang bật (để vẽ dấu chấm trên lịch)
    @Query("""
        SELECT e.startTime FROM calendar_events e
        INNER JOIN calendar_sources s ON e.sourceId = s.id
        WHERE s.isEnabled = 1
    """)
    fun getEnabledEventStartTimes(): Flow<List<Long>>

    // Lấy events theo sourceId (để hủy notification khi xóa source)
    @Query("SELECT * FROM calendar_events WHERE sourceId = :sourceId")
    suspend fun getEventsBySourceId(sourceId: Int): List<CalendarEvent>
}
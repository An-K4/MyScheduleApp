package com.example.myschedule.data.repository

import android.content.Context
import android.net.Uri
import com.example.myschedule.data.db.AppDatabase
import com.example.myschedule.data.entity.CalendarEvent
import com.example.myschedule.data.entity.CalendarSource
import kotlinx.coroutines.flow.Flow
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.DtEnd
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.model.property.Uid
import java.time.LocalDate
import java.time.ZoneId

class CalendarRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val sourceDao = db.calendarSourceDao()
    private val eventDao = db.calendarEventDao()

    // Bảng màu tự động gán cho từng source
    companion object {
        val SOURCE_COLORS = listOf(
            0xFF4285F4.toInt(), // Blue
            0xFF34A853.toInt(), // Green
            0xFFEA4335.toInt(), // Red
            0xFFFBBC05.toInt(), // Yellow
            0xFF9C27B0.toInt(), // Purple
            0xFFFF5722.toInt(), // Deep Orange
            0xFF00BCD4.toInt(), // Cyan
            0xFF795548.toInt(), // Brown
        )
    }

    // ── Sources ──────────────────────────────────────────

    fun getAllSources(): Flow<List<CalendarSource>> = sourceDao.getAllSources()

    suspend fun updateSourceEnabled(sourceId: Int, isEnabled: Boolean) {
        sourceDao.updateEnabled(sourceId, isEnabled)
    }

    suspend fun deleteSource(source: CalendarSource): List<CalendarEvent> {
        // Lấy events trước khi xóa (để hủy notification ở tầng trên)
        val events = eventDao.getEventsBySourceId(source.id)
        sourceDao.delete(source)  // CASCADE sẽ tự xóa events trong DB
        return events
    }

    // ── Import ICS ───────────────────────────────────────

    suspend fun importIcsFile(uri: Uri, fileName: String): Pair<CalendarSource, List<CalendarEvent>> {
        val sourceCount = sourceDao.getCount()
        val color = SOURCE_COLORS[sourceCount % SOURCE_COLORS.size]

        // Tạo và lưu source mới
        val source = CalendarSource(
            name = fileName,
            uri = uri.toString(),
            color = color
        )
        val sourceId = sourceDao.insert(source).toInt()
        val savedSource = source.copy(id = sourceId)

        // Parse file ICS thành events
        val events = parseIcsToEvents(uri, sourceId)
        eventDao.insertAll(events)

        return Pair(savedSource, events)
    }

    private fun parseIcsToEvents(uri: Uri, sourceId: Int): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val calendar = CalendarBuilder().build(stream)
                for (component in calendar.getComponents<VEvent>("VEVENT")) {
                    val dtStart = component.getProperty<DtStart>("DTSTART") ?: continue
                    val dtEnd = component.getProperty<DtEnd>("DTEND")
                    val uid = component.getProperty<Uid>("UID")?.value ?: component.toString()

                    events.add(
                        CalendarEvent(
                            sourceId = sourceId,
                            uid = uid,
                            title = component.summary?.value ?: "Sự kiện",
                            startTime = dtStart.date.time,
                            endTime = dtEnd?.date?.time ?: dtStart.date.time,
                            location = component.getProperty<net.fortuna.ical4j.model.property.Location>("LOCATION")?.value,
                            description = component.getProperty<net.fortuna.ical4j.model.property.Description>("DESCRIPTION")?.value
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return events
    }

    // ── Events ───────────────────────────────────────────

    fun getEventsForDay(date: LocalDate): Flow<List<CalendarEvent>> {
        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toEpochSecond() * 1000
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toEpochSecond() * 1000
        return eventDao.getEventsForDay(dayStart, dayEnd)
    }

    fun getEnabledEventStartTimes(): Flow<List<Long>> = eventDao.getEnabledEventStartTimes()
}
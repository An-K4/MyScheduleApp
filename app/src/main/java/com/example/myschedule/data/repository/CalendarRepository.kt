package com.example.myschedule.data.repository

import android.content.Context
import android.net.Uri
import com.example.myschedule.data.db.AppDatabase
import com.example.myschedule.data.db.EventTimeWithSource
import com.example.myschedule.data.entity.CalendarEvent
import com.example.myschedule.data.entity.CalendarSource
import com.example.myschedule.receiver.NotificationScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    companion object {
        val SOURCE_COLORS = listOf(
            0xFF4285F4.toInt(),
            0xFF34A853.toInt(),
            0xFFEA4335.toInt(),
            0xFFFBBC05.toInt(),
            0xFF9C27B0.toInt(),
            0xFFFF5722.toInt(),
            0xFF00BCD4.toInt(),
            0xFF795548.toInt(),
        )
    }

    // ── Sources ──────────────────────────────────────────

    fun getAllSources(): Flow<List<CalendarSource>> = sourceDao.getAllSources()

    suspend fun updateSourceEnabled(sourceId: Int, isEnabled: Boolean) {
        sourceDao.updateEnabled(sourceId, isEnabled)
    }

    suspend fun deleteSource(source: CalendarSource): List<CalendarEvent> {
        val events = eventDao.getEventsBySourceId(source.id)
        // Hủy toàn bộ thông báo liên quan trước khi xóa
        NotificationScheduler.cancelAll(context, events)
        sourceDao.delete(source)
        return events
    }

    // ── Import ICS ───────────────────────────────────────

    suspend fun importIcsFile(uri: Uri, fileName: String): ImportResult {
        // Check duplicate theo URI
        val existing = sourceDao.getByUri(uri.toString())
        if (existing != null) {
            return ImportResult.Duplicate(existing)
        }

        return try {
            val sourceCount = sourceDao.getCount()
            val color = SOURCE_COLORS[sourceCount % SOURCE_COLORS.size]

            val source = CalendarSource(name = fileName, uri = uri.toString(), color = color)
            val sourceId = sourceDao.insert(source).toInt()
            val savedSource = source.copy(id = sourceId)

            val events = parseIcsToEvents(uri, sourceId)
            eventDao.insertAll(events)

            // Lên lịch thông báo cho tất cả sự kiện mới import
            NotificationScheduler.scheduleAll(context, events)

            ImportResult.Success(savedSource, events.size)
        } catch (e: Exception) {
            ImportResult.Error(e.message ?: "Lỗi không xác định")
        }
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
                            location = component.getProperty<net.fortuna.ical4j.model.property.Location>(
                                "LOCATION"
                            )?.value,
                            description = component.getProperty<net.fortuna.ical4j.model.property.Description>(
                                "DESCRIPTION"
                            )?.value
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
        val dayEnd = date.atTime(23, 59, 59).atZone(zone).toEpochSecond() * 1000

        return eventDao.getEventsForDay(dayStart, dayEnd)
            .map { events ->
                events.map { event ->
                    event.copy(
                        startTime = maxOf(event.startTime, dayStart),
                        endTime = minOf(event.endTime, dayEnd)
                    )
                }
            }
    }

    fun getEnabledEventStartTimes(): Flow<List<EventTimeWithSource>> = eventDao.getEnabledEventStartTimes()

    suspend fun ensureDefaultSource() {
        val existing = sourceDao.getById(1)
        if (existing == null) {
            sourceDao.insert(
                CalendarSource(
                    id = 1,
                    name = "Lịch của tôi",
                    uri = "local",
                    color = SOURCE_COLORS[0],
                    isEnabled = true
                )
            )
        }
    }

    suspend fun addEvent(event: CalendarEvent) {
        eventDao.insertEvent(event)
        if (event.reminderMinutes != null) {
            NotificationScheduler.scheduleOne(context, event)
        }
    }

    suspend fun updateEvent(event: CalendarEvent) {
        eventDao.updateEvent(event)
        NotificationScheduler.cancelOne(context, event)
        if (event.reminderMinutes != null) {
            NotificationScheduler.scheduleOne(context, event)
        }
    }

    suspend fun deleteEvent(event: CalendarEvent) {
        NotificationScheduler.cancelOne(context, event)
        eventDao.deleteEvent(event)
    }

    suspend fun getEventById(eventId: Int): CalendarEvent? =
        eventDao.getEventById(eventId)

    suspend fun getSourceById(id: Int): CalendarSource? = sourceDao.getById(id)
}
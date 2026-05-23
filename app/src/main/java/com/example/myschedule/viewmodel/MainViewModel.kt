package com.example.myschedule.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.myschedule.data.repository.CalendarRepository
import com.example.myschedule.data.repository.ImportResult
import com.example.myschedule.data.entity.CalendarEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CalendarRepository(application)

    private val _currentMonth = MutableLiveData<YearMonth>(YearMonth.now())
    val currentMonth: LiveData<YearMonth> = _currentMonth

    fun setCurrentMonth(month: YearMonth) { _currentMonth.value = month }

    private val _selectedDate = MutableLiveData<LocalDate>(LocalDate.now())
    val selectedDate: LiveData<LocalDate> = _selectedDate

    // Import result để Activity hiển thị Toast phù hợp
    private val _importResult = MutableLiveData<ImportResult?>()
    val importResult: LiveData<ImportResult?> = _importResult

    val eventDateColors: LiveData<Map<LocalDate, List<Int>>> =
        combine(
            repository.getEnabledEventTimes(),
            repository.getAllSources()
        ) { eventTimes, sources ->
            val colorMap = sources.associate { it.id to it.color }
            val zone = ZoneId.systemDefault()
            val result = mutableMapOf<LocalDate, MutableList<Int>>()

            eventTimes.forEach { item ->
                val startDate = Instant.ofEpochMilli(item.startTime).atZone(zone).toLocalDate()
                val endDate = Instant.ofEpochMilli(item.endTime).atZone(zone).toLocalDate()
                val color = colorMap[item.sourceId] ?: return@forEach

                var cursor = startDate
                while (!cursor.isAfter(endDate)) {
                    result.getOrPut(cursor) { mutableListOf() }.let {
                        if (!it.contains(color)) it.add(color)
                    }
                    cursor = cursor.plusDays(1)
                }
            }
            result
        }.asLiveData()


    val eventDates: LiveData<Set<LocalDate>> =
        repository.getEnabledEventTimes()
            .map { items ->
                val zone = ZoneId.systemDefault()
                val dates = mutableSetOf<LocalDate>()
                items.forEach { item ->
                    val startDate = Instant.ofEpochMilli(item.startTime).atZone(zone).toLocalDate()
                    val endDate = Instant.ofEpochMilli(item.endTime).atZone(zone).toLocalDate()
                    var cursor = startDate
                    while (!cursor.isAfter(endDate)) {
                        dates.add(cursor)
                        cursor = cursor.plusDays(1)
                    }
                }
                dates
            }.asLiveData()

    val sourceColors: LiveData<Map<Int, Int>> =
        repository.getAllSources()
            .map { sources -> sources.associate { it.id to it.color } }
            .asLiveData()

    @OptIn(ExperimentalCoroutinesApi::class)
    val eventsForSelectedDate: LiveData<List<CalendarEvent>> =
        _selectedDate.toFlow()
            .flatMapLatest { date -> repository.getEventsForDay(date) }
            .asLiveData()

    init {
        viewModelScope.launch {
            repository.ensureDefaultSource()
        }
    }

    fun selectDate(date: LocalDate) { _selectedDate.value = date }

    fun importIcsFile(uri: Uri, fileName: String) {
        viewModelScope.launch {
            val result = repository.importIcsFile(uri, fileName)
            _importResult.value = result
        }
    }

    // Gọi sau khi Activity đã xử lý event để tránh re-trigger khi rotate
    fun clearImportResult() { _importResult.value = null }

    private fun <T> LiveData<T>.toFlow() =
        kotlinx.coroutines.flow.flow {
            val channel = kotlinx.coroutines.channels.Channel<T>()
            val observer = androidx.lifecycle.Observer<T> { value ->
                viewModelScope.launch { channel.send(value) }
            }
            observeForever(observer)
            try {
                for (value in channel) emit(value)
            } finally {
                removeObserver(observer)
            }
        }

    fun addManualEvent(event: CalendarEvent) {
        viewModelScope.launch { repository.addEvent(event) }
    }

    fun updateEvent(event: CalendarEvent) {
        viewModelScope.launch { repository.updateEvent(event) }
    }

    fun deleteEvent(event: CalendarEvent) {
        viewModelScope.launch { repository.deleteEvent(event) }
    }

    fun getEventById(eventId: Int): LiveData<CalendarEvent?> {
        val result = MutableLiveData<CalendarEvent?>()
        viewModelScope.launch {
            result.value = repository.getEventById(eventId)
        }
        return result
    }

    fun getSourceName(sourceId: Int): LiveData<String> {
        val result = MutableLiveData<String>()
        viewModelScope.launch {
            val source = repository.getSourceById(sourceId)
            result.value = source?.name ?: "Không rõ nguồn"
        }
        return result
    }

    fun getEventsForYearRange(yearStart: Long, yearEnd: Long): LiveData<List<CalendarEvent>> {
        return repository.getEventsForYearRange(yearStart, yearEnd).asLiveData()
    }
}
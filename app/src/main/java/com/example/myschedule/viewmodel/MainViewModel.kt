package com.example.myschedule.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.myschedule.data.repository.CalendarRepository
import com.example.myschedule.data.entity.CalendarEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CalendarRepository(application)

    private val _currentMonth = MutableLiveData(YearMonth.now())
    val currentMonth: LiveData<YearMonth> = _currentMonth

    fun setCurrentMonth(month: YearMonth) {
        _currentMonth.value = month
    }

    private val _selectedDate = MutableLiveData(LocalDate.now())
    val selectedDate: LiveData<LocalDate> = _selectedDate

    private val _scrollToToday = MutableLiveData(false)
    val scrollToToday: LiveData<Boolean> = _scrollToToday

    fun goToToday() {
        selectDate(LocalDate.now())
        setCurrentMonth(YearMonth.now())
        _scrollToToday.value = true
    }

    fun clearScrollToToday() {
        _scrollToToday.value = false
    }

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
        _selectedDate.switchMap { date -> repository.getEventsForDay(date).asLiveData() }

    private val _agendaYearRange = MutableLiveData<Pair<Long, Long>>()

    fun setAgendaYearRange(yearStart: Long, yearEnd: Long) {
        _agendaYearRange.value = Pair(yearStart, yearEnd)
    }

    val agendaEvents: LiveData<List<CalendarEvent>> = _agendaYearRange.switchMap { (start, end) ->
        repository.getEventsForYearRange(start, end).asLiveData()
    }

    init {
        viewModelScope.launch {
            repository.ensureDefaultSource()
        }
    }

    fun selectDate(date: LocalDate) { _selectedDate.value = date }
}
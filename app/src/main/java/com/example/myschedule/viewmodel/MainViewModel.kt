package com.example.myschedule.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.myschedule.data.entity.CalendarEvent
import com.example.myschedule.data.repository.CalendarRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CalendarRepository(application)

    // Ngày đang được chọn
    private val _selectedDate = MutableLiveData<LocalDate>(LocalDate.now())
    val selectedDate: LiveData<LocalDate> = _selectedDate

    // Tập hợp các ngày có sự kiện (để vẽ dấu chấm trên lịch)
    val eventDates: LiveData<Set<LocalDate>> =
        repository.getEnabledEventStartTimes()
            .toLocalDateSet()
            .asLiveData()

    // Danh sách sự kiện của ngày đang chọn — tự động cập nhật khi đổi ngày
    @OptIn(ExperimentalCoroutinesApi::class)
    val eventsForSelectedDate: LiveData<List<CalendarEvent>> =
        _selectedDate.toFlow()
            .flatMapLatest { date -> repository.getEventsForDay(date) }
            .asLiveData()

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun importIcsFile(uri: android.net.Uri, fileName: String) {
        viewModelScope.launch {
            repository.importIcsFile(uri, fileName)
        }
    }

    // Extension: chuyển Flow<List<Long>> thành Flow<Set<LocalDate>>
    private fun Flow<List<Long>>.toLocalDateSet(): Flow<Set<LocalDate>> =
        map { times ->
            val zone = ZoneId.systemDefault()
            times.map { epoch ->
                java.time.Instant.ofEpochMilli(epoch).atZone(zone).toLocalDate()
            }.toSet()
        }

    // Extension: chuyển LiveData<T> thành Flow<T>
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
}
package com.example.myschedule.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myschedule.data.entity.CalendarEvent
import com.example.myschedule.data.repository.CalendarRepository
import kotlinx.coroutines.launch

class EventDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CalendarRepository(application)

    private val _event = MutableLiveData<CalendarEvent?>()
    val event: LiveData<CalendarEvent?> = _event

    private val _sourceName = MutableLiveData<String>()
    val sourceName: LiveData<String> = _sourceName

    private val _finishSignal = MutableLiveData(false)
    val finishSignal: LiveData<Boolean> = _finishSignal

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadEvent(eventId: Int) {
        viewModelScope.launch {
            val event = repository.getEventById(eventId)
            _event.value = event

            if (event != null) {
                val source = repository.getSourceById(event.sourceId)
                _sourceName.value = source?.name ?: "Không rõ nguồn"
            }
        }
    }

    fun updateEvent(
        title: String,
        startMillis: Long,
        endMillis: Long,
        location: String?,
        description: String?,
        reminderMinutes: Long?
    ) {
        val current = _event.value ?: return
        viewModelScope.launch {
            try {
                val updated = current.copy(
                    title = title,
                    startTime = startMillis,
                    endTime = endMillis,
                    location = location,
                    description = description,
                    reminderMinutes = reminderMinutes
                )
                repository.updateEvent(updated)
                _event.value = updated
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi khi lưu: ${e.message}"
            }
        }
    }

    fun deleteEvent() {
        val current = _event.value ?: return
        viewModelScope.launch {
            try {
                repository.deleteEvent(current)
                _finishSignal.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi khi xóa: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
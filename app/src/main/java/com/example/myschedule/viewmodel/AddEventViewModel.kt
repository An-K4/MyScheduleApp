package com.example.myschedule.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myschedule.data.entity.CalendarEvent
import com.example.myschedule.data.repository.CalendarRepository
import kotlinx.coroutines.launch

class AddEventViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CalendarRepository(application)

    private val _saveSuccess = MutableLiveData(false)
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun addEvent(event: CalendarEvent) {
        viewModelScope.launch {
            try {
                repository.ensureDefaultSource()
                repository.addEvent(event)
                _saveSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi khi lưu sự kiện: ${e.message}"
            }
        }
    }

    fun clearError() { _errorMessage.value = null }
}
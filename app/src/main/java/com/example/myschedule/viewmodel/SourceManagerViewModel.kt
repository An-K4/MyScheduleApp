package com.example.myschedule.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.myschedule.data.entity.CalendarSource
import com.example.myschedule.data.repository.CalendarRepository
import com.example.myschedule.data.repository.ImportResult
import kotlinx.coroutines.launch

class SourceManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CalendarRepository(application)

    val allSources = repository.getAllSources().asLiveData()

    private val _importResult = MutableLiveData<ImportResult?>()
    val importResult: LiveData<ImportResult?> = _importResult

    fun importIcsFile(uri: Uri, fileName: String) {
        viewModelScope.launch {
            val result = repository.importIcsFile(uri, fileName)
            _importResult.value = result
        }
    }

    fun clearImportResult() { _importResult.value = null }

    fun toggleSource(sourceId: Int, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.updateSourceEnabled(sourceId, isEnabled)
        }
    }

    fun deleteSource(source: CalendarSource) {
        viewModelScope.launch {
            // Repository đã gọi NotificationScheduler.cancelAll() bên trong
            repository.deleteSource(source)
        }
    }
}
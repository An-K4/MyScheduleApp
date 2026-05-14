package com.example.myschedule.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.myschedule.data.entity.CalendarSource
import com.example.myschedule.data.repository.CalendarRepository
import com.example.myschedule.receiver.NotificationReceiver
import kotlinx.coroutines.launch

class SourceManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CalendarRepository(application)

    val allSources = repository.getAllSources().asLiveData()

    // 5.5 — Import file ICS mới từ SourceManagerActivity
    fun importIcsFile(uri: Uri, fileName: String) {
        viewModelScope.launch {
            repository.importIcsFile(uri, fileName)
        }
    }

    // 5.6 — Toggle bật/tắt nguồn
    fun toggleSource(sourceId: Int, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.updateSourceEnabled(sourceId, isEnabled)
        }
    }

    // 5.7 — Xóa nguồn + hủy notification liên quan
    fun deleteSource(source: CalendarSource) {
        viewModelScope.launch {
            val eventsToCancel = repository.deleteSource(source)

            val alarmManager =
                getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager

            eventsToCancel.forEach { event ->
                val intent = Intent(getApplication(), NotificationReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    getApplication(),
                    event.uid.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
            }
        }
    }
}
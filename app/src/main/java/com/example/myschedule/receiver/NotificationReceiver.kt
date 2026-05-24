package com.example.myschedule.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.myschedule.R

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val NOTIFICATION_ID_KEY = "notification_id"
        const val EVENT_TITLE_KEY = "event_title"
        const val REMINDER_MINUTES_KEY = "reminder_minutes"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val eventTitle = intent.getStringExtra(EVENT_TITLE_KEY) ?: "Sự kiện sắp diễn ra"
        val notificationId = intent.getIntExtra(NOTIFICATION_ID_KEY, 0)
        val reminderMinutes = intent.getLongExtra(REMINDER_MINUTES_KEY, 30L)

        val channelId = "event_notifications"
        val channelName = "Thông báo sự kiện"

        val contentText = when {
            reminderMinutes < 60 -> "Sự kiện sẽ bắt đầu trong $reminderMinutes phút nữa."
            reminderMinutes < 1440 -> "Sự kiện sẽ bắt đầu trong ${reminderMinutes / 60} giờ nữa."
            else -> "Sự kiện sẽ bắt đầu trong ${reminderMinutes / 1440} ngày nữa."
        }

        // Tạo Notification Channel (bắt buộc cho Android 8.0+)
        // Hệ thống sẽ tự bỏ qua nếu channel đã tồn tại.
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        // Xây dựng thông báo
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_calendar)
            .setContentTitle(eventTitle)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        // Hiển thị thông báo
        notificationManager.notify(notificationId, notification)
    }
}
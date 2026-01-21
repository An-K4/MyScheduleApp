package com.example.myschedule

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val NOTIFICATION_ID_KEY = "notification_id"
        const val EVENT_TITLE_KEY = "event_title"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val eventTitle = intent.getStringExtra(EVENT_TITLE_KEY) ?: "Sự kiện sắp diễn ra"
        val notificationId = intent.getIntExtra(NOTIFICATION_ID_KEY, 0)

        val channelId = "event_notifications"
        val channelName = "Thông báo sự kiện"

        // Tạo Notification Channel (bắt buộc cho Android 8.0+)
        // Hệ thống sẽ tự bỏ qua nếu channel đã tồn tại.
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        // Xây dựng thông báo
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_calendar)
            .setContentTitle(eventTitle)
            .setContentText("Sự kiện sẽ bắt đầu trong 30 phút nữa.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        // Hiển thị thông báo
        notificationManager.notify(notificationId, notification)
    }
}
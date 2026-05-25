package com.example.myschedule.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.myschedule.R
import com.example.myschedule.ui.event.EventDetailActivity

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_EVENT_TITLE = "event_title"
        const val EXTRA_REMINDER_MINUTES = "reminder_minutes"
        const val EXTRA_NOTIFICATION_ID = "notification_id"

        private const val CHANNEL_ID = "event_notifications"
        private const val CHANNEL_NAME = "Thông báo sự kiện"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getIntExtra(EXTRA_EVENT_ID, -1)
        val eventTitle = intent.getStringExtra(EXTRA_EVENT_TITLE) ?: "Sự kiện sắp diễn ra"
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val reminderMinutes = intent.getLongExtra(EXTRA_REMINDER_MINUTES, 30L)

        val contentText = when {
            reminderMinutes < 60 -> "Sự kiện sẽ bắt đầu trong $reminderMinutes phút nữa."
            reminderMinutes < 1440 -> "Sự kiện sẽ bắt đầu trong ${reminderMinutes / 60} giờ nữa."
            else -> "Sự kiện sẽ bắt đầu trong ${reminderMinutes / 1440} ngày nữa."
        }

        // ── PendingIntent mở EventDetailActivity khi tap ──────────────────────
        val tapIntent = Intent(context, EventDetailActivity::class.java).apply {
            putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ── Tạo channel (bỏ qua nếu đã tồn tại) ─────────────────────────────
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        // ── Build và hiển thị notification ───────────────────────────────────
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_calendar)
            .setContentTitle(eventTitle)
            .setContentText(contentText)
            .setContentIntent(tapPendingIntent)   // ← tap → mở EventDetailActivity
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)                  // ← tự dismiss sau khi tap
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
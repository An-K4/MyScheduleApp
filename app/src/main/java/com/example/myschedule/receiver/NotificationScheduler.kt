package com.example.myschedule.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.myschedule.data.entity.CalendarEvent

object NotificationScheduler {

    private const val NOTIFY_BEFORE_MS = 30 * 60 * 1000L  // 30 phút

    fun scheduleAll(context: Context, events: List<CalendarEvent>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()

        events.forEach { event ->
            val triggerAt = event.startTime - NOTIFY_BEFORE_MS
            // Bỏ qua sự kiện đã qua hoặc sắp xảy ra trong vòng < 30 phút
            if (triggerAt <= now) return@forEach

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra(NotificationReceiver.EVENT_TITLE_KEY, event.title)
                putExtra(NotificationReceiver.NOTIFICATION_ID_KEY, event.uid.hashCode())
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                event.uid.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                // setExactAndAllowWhileIdle để thông báo bắn đúng giờ kể cả khi Doze
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                // Android 12+ cần permission SCHEDULE_EXACT_ALARM
                // Fallback sang set() thông thường nếu không có quyền
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
        }
    }

    fun cancelAll(context: Context, events: List<CalendarEvent>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        events.forEach { event ->
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                event.uid.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    fun scheduleOne(context: Context, event: CalendarEvent) {
        if (event.reminderMinutes == null) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = event.startTime - event.reminderMinutes * 60 * 1000L
        if (triggerAt <= System.currentTimeMillis()) return

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EVENT_TITLE_KEY, event.title)
            putExtra(NotificationReceiver.NOTIFICATION_ID_KEY, event.uid.hashCode())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.uid.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent
            )
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancelOne(context: Context, event: CalendarEvent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.uid.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
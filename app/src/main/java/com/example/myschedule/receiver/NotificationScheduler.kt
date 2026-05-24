package com.example.myschedule.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.myschedule.data.entity.CalendarEvent

object NotificationScheduler {

    fun scheduleAll(context: Context, events: List<CalendarEvent>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()

        events.forEach { event ->
            // ── MỚI: skip nếu user tắt thông báo (null) ──
            val reminderMs = event.reminderMinutes?.let { it * 60 * 1000L } ?: return@forEach

            val triggerAt = event.startTime - reminderMs
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
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } catch (e: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
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
            putExtra(NotificationReceiver.REMINDER_MINUTES_KEY, event.reminderMinutes)
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
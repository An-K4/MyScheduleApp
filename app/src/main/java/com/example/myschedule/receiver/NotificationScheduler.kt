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
            val reminderMs = event.reminderMinutes?.let { it * 60 * 1000L } ?: return@forEach
            val triggerAt = event.startTime - reminderMs
            if (triggerAt <= now) return@forEach

            val pendingIntent = buildPendingIntent(context, event)

            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent
                )
            } catch (e: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }
    }

    fun cancelAll(context: Context, events: List<CalendarEvent>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        events.forEach { event ->
            alarmManager.cancel(buildPendingIntent(context, event))
        }
    }

    fun scheduleOne(context: Context, event: CalendarEvent) {
        if (event.reminderMinutes == null) return
        val triggerAt = event.startTime - event.reminderMinutes * 60 * 1000L
        if (triggerAt <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, event)

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
        alarmManager.cancel(buildPendingIntent(context, event))
    }

    // ── Helper dùng chung ─────────────────────────────────────────────────────
    private fun buildPendingIntent(context: Context, event: CalendarEvent): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_EVENT_ID, event.id)
            putExtra(NotificationReceiver.EXTRA_EVENT_TITLE, event.title)
            putExtra(NotificationReceiver.EXTRA_REMINDER_MINUTES, event.reminderMinutes ?: 30L)
            putExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, event.uid.hashCode())
        }
        return PendingIntent.getBroadcast(
            context,
            event.uid.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
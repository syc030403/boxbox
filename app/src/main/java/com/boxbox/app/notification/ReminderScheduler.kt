package com.boxbox.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ReminderScheduler {

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        return alarmManager.canScheduleExactAlarms()
    }

    private fun pendingIntentFor(context: Context, entry: ReminderEntry): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_KEY, entry.key)
            putExtra(ReminderReceiver.EXTRA_RACE_NAME, entry.raceName)
            putExtra(ReminderReceiver.EXTRA_SESSION_TYPE, entry.sessionType)
            putExtra(ReminderReceiver.EXTRA_OFFSET_MINUTES, entry.offsetMinutes)
        }
        return PendingIntent.getBroadcast(
            context, entry.key.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun schedule(context: Context, entry: ReminderEntry) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = pendingIntentFor(context, entry)
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, entry.triggerAtMillis, pendingIntent
            )
        } catch (_: SecurityException) {
            // 정확한 알람 권한이 없음 -> 대략적인 시간에라도 울리도록 폴백
            alarmManager.set(AlarmManager.RTC_WAKEUP, entry.triggerAtMillis, pendingIntent)
        }
        ReminderStore.put(context, entry)
    }

    fun cancel(context: Context, key: String) {
        val all = ReminderStore.getAll(context)
        val entry = all[key] ?: return
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(pendingIntentFor(context, entry))
        ReminderStore.remove(context, key)
    }

    /** 기기 재부팅 등으로 날아간 알람을, 아직 유효한(미래 시각인) 것만 다시 건다. */
    fun rescheduleAll(context: Context) {
        val now = System.currentTimeMillis()
        ReminderStore.getAll(context).values.forEach { entry ->
            if (entry.triggerAtMillis > now) {
                schedule(context, entry)
            } else {
                ReminderStore.remove(context, entry.key)
            }
        }
    }
}

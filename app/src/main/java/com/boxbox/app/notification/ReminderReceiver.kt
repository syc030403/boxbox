package com.boxbox.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.boxbox.app.R

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_KEY = "extra_key"
        const val EXTRA_RACE_NAME = "extra_race_name"
        const val EXTRA_SESSION_TYPE = "extra_session_type"
        const val EXTRA_OFFSET_MINUTES = "extra_offset_minutes"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra(EXTRA_KEY) ?: return
        val raceName = intent.getStringExtra(EXTRA_RACE_NAME) ?: return
        val sessionType = intent.getStringExtra(EXTRA_SESSION_TYPE) ?: return
        val offsetMinutes = intent.getIntExtra(EXTRA_OFFSET_MINUTES, 0)

        val text = context.getString(
            R.string.reminder_notification_text,
            OffsetFormat.label(context, offsetMinutes),
            SessionLabels.label(context, sessionType)
        )
        NotificationHelper.show(context, notificationId = key.hashCode(), title = raceName, text = text)

        ReminderStore.remove(context, key)
    }
}

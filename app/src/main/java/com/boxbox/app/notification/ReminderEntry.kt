package com.boxbox.app.notification

data class ReminderEntry(
    val key: String,
    val season: Int,
    val round: String,
    val raceName: String,
    val sessionType: String,
    val offsetMinutes: Int,
    val triggerAtMillis: Long
) {
    companion object {
        fun makeKey(season: Int, round: String, sessionType: String, offsetMinutes: Int) =
            "s${season}_r${round}_t${sessionType}_o$offsetMinutes"
    }
}

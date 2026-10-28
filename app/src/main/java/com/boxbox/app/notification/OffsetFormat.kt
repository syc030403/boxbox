package com.boxbox.app.notification

import android.content.Context
import com.boxbox.app.R

object OffsetFormat {
    /** 분 단위 오프셋 선택지. "N분 전" 알림을 걸 수 있는 프리셋 목록. */
    val PRESET_MINUTES = listOf(1440, 180, 60, 30, 10)

    fun label(context: Context, offsetMinutes: Int): String = when {
        offsetMinutes % 1440 == 0 -> context.getString(R.string.offset_days, offsetMinutes / 1440)
        offsetMinutes % 60 == 0 -> context.getString(R.string.offset_hours, offsetMinutes / 60)
        else -> context.getString(R.string.offset_minutes, offsetMinutes)
    }
}

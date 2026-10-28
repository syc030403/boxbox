package com.boxbox.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {

    private val kstFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd (E) HH:mm 'KST'", Locale.KOREA)
    private val kstZone = ZoneId.of("Asia/Seoul")
    private val localTimestampFormatter = DateTimeFormatter.ofPattern("MM.dd HH:mm", Locale.KOREA)

    fun formatLocalTimestamp(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(localTimestampFormatter)

    fun toKstZonedDateTime(date: String, time: String?): ZonedDateTime {
        val localDate = LocalDate.parse(date)
        val localTime = time?.let { LocalTime.parse(it.removeSuffix("Z")) } ?: LocalTime.MIDNIGHT
        return LocalDateTime.of(localDate, localTime).atOffset(ZoneOffset.UTC).atZoneSameInstant(kstZone)
    }

    fun formatKst(date: String, time: String?): String =
        toKstZonedDateTime(date, time).format(kstFormatter)

    fun isPast(date: String, time: String?): Boolean =
        toKstZonedDateTime(date, time).isBefore(ZonedDateTime.now(kstZone))

    fun toEpochMillis(date: String, time: String?): Long =
        toKstZonedDateTime(date, time).toInstant().toEpochMilli()
}

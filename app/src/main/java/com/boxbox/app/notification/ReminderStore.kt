package com.boxbox.app.notification

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * 사용자가 설정한 레이스 알림을 기기에 영구 저장한다 (TTL 없는 캐시).
 * 재부팅 후 알람이 날아가므로 BootReceiver가 이 목록을 다시 읽어 재예약한다.
 */
object ReminderStore {
    private val gson = Gson()
    private val listType = object : TypeToken<MutableMap<String, ReminderEntry>>() {}.type

    private fun file(context: Context) = File(context.filesDir, "boxbox_reminders.json")

    @Synchronized
    fun getAll(context: Context): Map<String, ReminderEntry> {
        val f = file(context)
        if (!f.exists()) return emptyMap()
        return try {
            gson.fromJson<MutableMap<String, ReminderEntry>>(f.readText(), listType) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    @Synchronized
    fun put(context: Context, entry: ReminderEntry) {
        val all = getAll(context).toMutableMap()
        all[entry.key] = entry
        file(context).writeText(gson.toJson(all))
    }

    @Synchronized
    fun remove(context: Context, key: String) {
        val all = getAll(context).toMutableMap()
        all.remove(key)
        file(context).writeText(gson.toJson(all))
    }

    fun enabledOffsetsFor(context: Context, season: Int, round: String, sessionType: String): Set<Int> =
        getAll(context).values
            .filter { it.season == season && it.round == round && it.sessionType == sessionType }
            .map { it.offsetMinutes }
            .toSet()

    fun hasAnyFor(context: Context, season: Int, round: String, sessionType: String): Boolean =
        getAll(context).values.any { it.season == season && it.round == round && it.sessionType == sessionType }
}

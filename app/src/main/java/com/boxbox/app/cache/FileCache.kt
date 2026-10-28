package com.boxbox.app.cache

import android.content.Context
import com.google.gson.Gson
import java.io.File

object FileCache {
    private val gson = Gson()

    private fun dataFile(context: Context, key: String) = File(context.cacheDir, "boxbox_$key.json")
    private fun metaFile(context: Context, key: String) = File(context.cacheDir, "boxbox_$key.meta")

    fun <T> save(context: Context, key: String, data: T) {
        try {
            dataFile(context, key).writeText(gson.toJson(data))
            metaFile(context, key).writeText(System.currentTimeMillis().toString())
        } catch (_: Exception) {
            // best-effort cache; a write failure just means no offline fallback for this key
        }
    }

    fun <T> load(context: Context, key: String, clazz: Class<T>): T? = try {
        val file = dataFile(context, key)
        if (!file.exists()) null else gson.fromJson(file.readText(), clazz)
    } catch (_: Exception) {
        null
    }

    fun savedAtMillis(context: Context, key: String): Long? = try {
        val file = metaFile(context, key)
        if (!file.exists()) null else file.readText().trim().toLong()
    } catch (_: Exception) {
        null
    }

    fun ageMillis(context: Context, key: String): Long? =
        savedAtMillis(context, key)?.let { System.currentTimeMillis() - it }
}

package com.boxbox.app.notification

import android.content.Context
import com.boxbox.app.R

object SessionLabels {
    private val labelRes = mapOf(
        "fp1" to R.string.session_fp1,
        "fp2" to R.string.session_fp2,
        "fp3" to R.string.session_fp3,
        "sq" to R.string.session_sprint_qualifying,
        "sprint" to R.string.session_sprint,
        "quali" to R.string.session_qualifying,
        "race" to R.string.session_race
    )

    fun label(context: Context, sessionType: String): String =
        labelRes[sessionType]?.let { context.getString(it) } ?: sessionType
}

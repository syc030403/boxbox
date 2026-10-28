package com.boxbox.app.util

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.TextView

data class TeamStyle(val colorHex: String, val code: String)

object TeamInfo {
    private val styles = mapOf(
        "mclaren" to TeamStyle("#FF8000", "MCL"),
        "ferrari" to TeamStyle("#E80020", "FER"),
        "red_bull" to TeamStyle("#3671C6", "RBR"),
        "mercedes" to TeamStyle("#27F4D2", "MER"),
        "aston_martin" to TeamStyle("#229971", "AMR"),
        "alpine" to TeamStyle("#00A1E8", "ALP"),
        "haas" to TeamStyle("#B6BABD", "HAA"),
        "rb" to TeamStyle("#6692FF", "RB"),
        "williams" to TeamStyle("#64C4FF", "WIL"),
        "sauber" to TeamStyle("#52E252", "SAU"),
        "audi" to TeamStyle("#BB0A30", "AUD"),
        "cadillac" to TeamStyle("#A6192E", "CAD")
    )

    private val fallback = TeamStyle("#808088", "?")

    fun styleFor(constructorId: String?): TeamStyle =
        styles[constructorId] ?: fallback

    fun applyChip(chip: TextView, constructorId: String?) {
        val style = styleFor(constructorId)
        val bgColor = Color.parseColor(style.colorHex)
        chip.text = style.code
        (chip.background.mutate() as GradientDrawable).setColor(bgColor)
        chip.setTextColor(if (isLight(bgColor)) Color.BLACK else Color.WHITE)
    }

    fun applyAccentBar(bar: View, constructorId: String?) {
        bar.setBackgroundColor(Color.parseColor(styleFor(constructorId).colorHex))
    }

    private fun isLight(color: Int): Boolean {
        val luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color))
        return luminance > 150
    }
}

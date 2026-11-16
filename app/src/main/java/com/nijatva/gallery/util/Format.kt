package com.nijatva.gallery.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * The few numbers a gallery shows, written the way a person reads them.
 */
object Format {

    fun bytes(value: Long): String {
        if (value <= 0L) return "0 B"
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            value < kb -> "$value B"
            value < mb -> trimmed(value / kb) + " KB"
            value < gb -> trimmed(value / mb) + " MB"
            else -> trimmed(value / gb) + " GB"
        }
    }

    private fun trimmed(value: Double): String {
        val text = String.format(Locale.US, "%.1f", value)
        return if (text.endsWith(".0")) text.dropLast(2) else text
    }

    fun items(count: Int): String = if (count == 1) "1 item" else "$count items"

    /** "702 items · 2.35 GB", the line under every folder card. */
    fun itemsAndSize(count: Int, bytes: Long): String = items(count) + " · " + bytes(bytes)

    fun duration(milliseconds: Long): String {
        val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    fun date(epochSeconds: Long): String {
        if (epochSeconds <= 0L) return "No date"
        val date = Date(epochSeconds * 1000L)
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { time = date }
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        return when {
            sameDay(now, then) -> "Today, $time"
            sameDay(yesterday(now), then) -> "Yesterday, $time"
            now.get(Calendar.YEAR) == then.get(Calendar.YEAR) ->
                SimpleDateFormat("d MMM", Locale.getDefault()).format(date)
            else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(date)
        }
    }

    fun dimensions(width: Int, height: Int): String =
        if (width > 0 && height > 0) "$width × $height" else "Not reported"

    private fun sameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    private fun yesterday(now: Calendar): Calendar =
        (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
}

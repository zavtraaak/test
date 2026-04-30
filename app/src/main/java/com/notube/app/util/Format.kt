package com.notube.app.util

import java.util.Locale

/** Format a count of views/subscribers in compact YouTube style: 12K, 3.4M, 1.2B. */
fun formatCount(value: Long): String {
    if (value < 1_000) return value.toString()
    val (divisor, suffix) = when {
        value >= 1_000_000_000 -> 1_000_000_000.0 to "B"
        value >= 1_000_000 -> 1_000_000.0 to "M"
        else -> 1_000.0 to "K"
    }
    val v = value / divisor
    val str = if (v >= 100) "%.0f".format(Locale.US, v)
    else if (v >= 10) "%.1f".format(Locale.US, v).trimEnd('0').trimEnd('.')
    else "%.1f".format(Locale.US, v).trimEnd('0').trimEnd('.')
    return "$str$suffix"
}

/** Format a duration in seconds as HH:MM:SS or MM:SS. */
fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return ""
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

package com.v2ex.idea.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

fun formatEpochSeconds(epochSeconds: Long?): String {
    if (epochSeconds == null || epochSeconds <= 0L) return "--"
    return Instant.ofEpochSecond(epochSeconds)
        .atZone(ZoneId.systemDefault())
        .format(FORMATTER)
}

package com.ampairs.communication.service.campaign

import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Quiet-hours window evaluated in the workspace business timezone. Handles windows that span
 * midnight (start > end, e.g. 21:00–07:00). Promotional sends are deferred until the window ends.
 */
@Component
class QuietHours {

    /** True if [instant] falls inside the quiet window. Blank start/end → no quiet hours. */
    fun isWithin(instant: Instant, start: String?, end: String?, timezone: String): Boolean {
        val s = parse(start) ?: return false
        val e = parse(end) ?: return false
        if (s == e) return false
        val now = instant.atZone(zone(timezone)).toLocalTime()
        return if (s < e) now >= s && now < e            // same-day window
        else now >= s || now < e                          // spans midnight
    }

    /** The next instant at which the quiet window ends (when to resume), or [instant] if not quiet. */
    fun nextEnd(instant: Instant, start: String?, end: String?, timezone: String): Instant {
        if (!isWithin(instant, start, end, timezone)) return instant
        val e = parse(end)!!
        val z = zone(timezone)
        val zoned = instant.atZone(z)
        val todayEnd = zoned.toLocalDate().atTime(e).atZone(z)
        // If the end time today is still ahead, resume today; otherwise tomorrow (midnight-spanning).
        return if (todayEnd.toInstant().isAfter(instant)) todayEnd.toInstant()
        else todayEnd.plusDays(1).toInstant()
    }

    private fun parse(value: String?): LocalTime? =
        value?.takeIf { it.isNotBlank() }?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

    private fun zone(timezone: String): ZoneId = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneOffset.UTC)
}

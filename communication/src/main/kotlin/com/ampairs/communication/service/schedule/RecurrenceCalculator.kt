package com.ampairs.communication.service.schedule

import com.ampairs.communication.domain.enums.Frequency
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Computes the next occurrence of a recurrence rule as a UTC [Instant], evaluating all wall-clock
 * math in the workspace business timezone (FR-018). Honors interval, day selectors, start/end dates,
 * and clamps an out-of-range monthly day to the month's last day (FR-020).
 */
@Component
class RecurrenceCalculator {

    /**
     * @param after the next occurrence must be strictly after this instant.
     * @return the next occurrence as a UTC Instant, or null if the rule has no further occurrence
     *   (e.g. past the end date).
     */
    fun next(
        frequency: Frequency,
        interval: Int,
        dayOfWeek: Int?,
        dayOfMonth: Int?,
        timeOfDay: String,
        timezone: String,
        after: Instant,
        startDate: String?,
        endDate: String?,
    ): Instant? {
        val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneOffset.UTC)
        val time = runCatching { LocalTime.parse(timeOfDay) }.getOrDefault(LocalTime.of(9, 0))
        val step = if (interval < 1) 1 else interval
        val start = startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val end = endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val afterZ = after.atZone(zone)
        val anchor = maxOf(start ?: afterZ.toLocalDate(), afterZ.toLocalDate().minusYears(1))

        val candidate: ZonedDateTime = when (frequency) {
            Frequency.DAILY -> firstAfter(generateSequence(start ?: anchor) { it.plusDays(step.toLong()) }, time, zone, after)
            Frequency.WEEKLY -> {
                val target = dayOfWeek ?: (start ?: afterZ.toLocalDate()).dayOfWeek.value
                var firstOcc = start ?: afterZ.toLocalDate()
                var guard = 0
                while (firstOcc.dayOfWeek.value != target && guard++ < 7) firstOcc = firstOcc.plusDays(1)
                firstAfter(generateSequence(firstOcc) { it.plusWeeks(step.toLong()) }, time, zone, after)
            }
            Frequency.MONTHLY -> {
                val dom = dayOfMonth ?: (start ?: afterZ.toLocalDate()).dayOfMonth
                val months = generateSequence(YearMonth.from(start ?: afterZ.toLocalDate())) { it.plusMonths(step.toLong()) }
                    .map { ym -> ym.atDay(minOf(dom, ym.lengthOfMonth())) } // clamp to month end (FR-020)
                firstAfter(months, time, zone, after)
            }
        } ?: return null

        if (end != null && candidate.toLocalDate().isAfter(end)) return null
        return candidate.toInstant()
    }

    /** First date in the sequence whose at-`time` instant is strictly after `after`. Capped. */
    private fun firstAfter(dates: Sequence<LocalDate>, time: LocalTime, zone: ZoneId, after: Instant): ZonedDateTime? {
        return dates.take(MAX_ITER)
            .map { it.atTime(time).atZone(zone) }
            .firstOrNull { it.toInstant().isAfter(after) }
    }

    /** Stable occurrence key — the wall-clock minute of the occurrence in its zone, e.g. 2026-07-01T09:00. */
    fun occurrenceKey(instant: Instant, timezone: String): String {
        val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneOffset.UTC)
        val ldt = instant.atZone(zone).toLocalDateTime()
        return "%04d-%02d-%02dT%02d:%02d".format(ldt.year, ldt.monthValue, ldt.dayOfMonth, ldt.hour, ldt.minute)
    }

    companion object {
        private const val MAX_ITER = 800
    }
}

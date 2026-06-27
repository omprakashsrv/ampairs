package com.ampairs.communication.service.schedule

import com.ampairs.communication.domain.enums.Frequency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class RecurrenceCalculatorTest {

    private val calc = RecurrenceCalculator()

    @Test
    fun `monthly day 1 at 9am fires at business-local time, not server time`() {
        // After 2026-06-15T00:00Z, next monthly day-1 09:00 in Asia/Kolkata (UTC+5:30).
        val after = Instant.parse("2026-06-15T00:00:00Z")
        val next = calc.next(Frequency.MONTHLY, 1, null, 1, "09:00", "Asia/Kolkata", after, null, null)!!
        // 09:00 IST on 2026-07-01 == 03:30 UTC
        assertEquals(Instant.parse("2026-07-01T03:30:00Z"), next)
    }

    @Test
    fun `monthly day 31 clamps to last day of a short month`() {
        val after = Instant.parse("2026-02-01T00:00:00Z")
        val next = calc.next(Frequency.MONTHLY, 1, null, 31, "09:00", "UTC", after, null, null)!!
        // February 2026 has 28 days → fires on the 28th
        assertEquals(28, next.atZone(ZoneId.of("UTC")).dayOfMonth)
        assertEquals(2, next.atZone(ZoneId.of("UTC")).monthValue)
    }

    @Test
    fun `weekly with interval 2 lands on the requested weekday`() {
        val after = Instant.parse("2026-06-01T00:00:00Z") // Monday
        // dayOfWeek=1 (Monday), every 2 weeks
        val next = calc.next(Frequency.WEEKLY, 2, 1, null, "08:00", "UTC", after, null, null)!!
        assertEquals(java.time.DayOfWeek.MONDAY, next.atZone(ZoneId.of("UTC")).dayOfWeek)
        assertTrue(next.isAfter(after))
    }

    @Test
    fun `daily fires the next day at the configured time`() {
        val after = Instant.parse("2026-06-15T12:00:00Z")
        val next = calc.next(Frequency.DAILY, 1, null, null, "06:00", "UTC", after, null, null)!!
        assertEquals(Instant.parse("2026-06-16T06:00:00Z"), next)
    }

    @Test
    fun `returns null past the end date`() {
        val after = Instant.parse("2026-12-15T00:00:00Z")
        val next = calc.next(Frequency.MONTHLY, 1, null, 1, "09:00", "UTC", after, null, "2026-12-31")
        assertNull(next) // next would be 2027-01-01, beyond end date
    }

    @Test
    fun `occurrence key is the business-local wall-clock minute`() {
        val instant = Instant.parse("2026-07-01T03:30:00Z")
        assertEquals("2026-07-01T09:00", calc.occurrenceKey(instant, "Asia/Kolkata"))
    }
}

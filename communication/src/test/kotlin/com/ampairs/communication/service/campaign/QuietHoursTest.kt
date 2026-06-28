package com.ampairs.communication.service.campaign

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class QuietHoursTest {

    private val quietHours = QuietHours()

    // 09:30 IST == 04:00 UTC on 2026-06-15
    private val morningIst = Instant.parse("2026-06-15T04:00:00Z")
    // 23:00 IST == 17:30 UTC
    private val nightIst = Instant.parse("2026-06-15T17:30:00Z")

    @Test
    fun `same-day window`() {
        // Quiet 09:00-17:00 IST → 09:30 is within
        assertTrue(quietHours.isWithin(morningIst, "09:00", "17:00", "Asia/Kolkata"))
        assertFalse(quietHours.isWithin(nightIst, "09:00", "17:00", "Asia/Kolkata"))
    }

    @Test
    fun `midnight-spanning window`() {
        // Quiet 21:00-07:00 IST → 23:00 is within, 09:30 is not
        assertTrue(quietHours.isWithin(nightIst, "21:00", "07:00", "Asia/Kolkata"))
        assertFalse(quietHours.isWithin(morningIst, "21:00", "07:00", "Asia/Kolkata"))
    }

    @Test
    fun `blank config means no quiet hours`() {
        assertFalse(quietHours.isWithin(nightIst, null, null, "Asia/Kolkata"))
        assertFalse(quietHours.isWithin(nightIst, "", "", "Asia/Kolkata"))
    }

    @Test
    fun `nextEnd returns the resume instant when within quiet hours`() {
        val resume = quietHours.nextEnd(morningIst, "09:00", "17:00", "Asia/Kolkata")
        // 17:00 IST == 11:30 UTC same day
        assertTrue(resume.isAfter(morningIst))
    }
}

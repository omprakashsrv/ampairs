package com.ampairs.sfa.domain

import com.ampairs.sfa.domain.enums.VisitOutcome
import com.ampairs.sfa.domain.model.Attendance
import com.ampairs.sfa.domain.model.Leave
import com.ampairs.sfa.domain.model.Visit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class ReportingCalculatorsTest {

    private fun attendance(inAt: Instant?, outAt: Instant?, active: Boolean = true) = Attendance().apply {
        repMemberUid = "REP-1"; checkInAt = inAt; checkOutAt = outAt; this.active = active
    }

    private fun visit(outcome: VisitOutcome, customer: String, adHoc: Boolean = false, active: Boolean = true) = Visit().apply {
        repMemberUid = "REP-1"; this.outcome = outcome; customerUid = customer; this.adHoc = adHoc; this.active = active
    }

    @Test
    fun `attendance summary counts present days, hours, open days and leave`() {
        val base = Instant.parse("2026-06-01T09:00:00Z")
        val summary = AttendanceSummaryCalculator.summarize(
            "REP-1",
            attendance = listOf(
                attendance(base, base.plusSeconds(8 * 3600)),          // 8h closed
                attendance(base, base.plusSeconds(4 * 3600)),          // 4h closed
                attendance(base, null),                                // open (no checkout)
                attendance(null, null, active = false),                // inactive ignored
            ),
            leaves = listOf(Leave().apply { active = true }, Leave().apply { active = false }),
        )
        assertEquals(3, summary.daysPresent)
        assertEquals(12.0, summary.totalWorkingHours)
        assertEquals(1, summary.openDays)
        assertEquals(1, summary.leaveDays)
    }

    @Test
    fun `visit productivity computes productive percent, coverage and ad-hoc`() {
        val summary = VisitProductivityCalculator.summarize(
            "REP-1",
            listOf(
                visit(VisitOutcome.PRODUCTIVE, "CUS-1"),
                visit(VisitOutcome.PRODUCTIVE, "CUS-1"),       // revisit — same outlet
                visit(VisitOutcome.NO_ORDER, "CUS-2", adHoc = true),
                visit(VisitOutcome.OUTLET_CLOSED, "CUS-3"),
                visit(VisitOutcome.PRODUCTIVE, "CUS-4", active = false), // inactive ignored
            ),
        )
        assertEquals(4, summary.totalVisits)
        assertEquals(2, summary.productiveVisits)
        assertEquals(50.0, summary.productivePercent)
        assertEquals(3, summary.uniqueOutlets) // CUS-1 deduped
        assertEquals(1, summary.adHocVisits)
    }

    @Test
    fun `zero visits yields zero percent`() {
        val s = VisitProductivityCalculator.summarize("REP-1", emptyList())
        assertEquals(0.0, s.productivePercent)
        assertEquals(0, s.totalVisits)
    }
}

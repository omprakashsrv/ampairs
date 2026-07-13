package com.ampairs.sfa.domain.service

import com.ampairs.sfa.domain.model.Attendance
import com.ampairs.sfa.domain.model.Visit
import com.ampairs.sfa.repository.AttendanceRepository
import com.ampairs.sfa.repository.LeaveRepository
import com.ampairs.sfa.repository.VisitRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant

/** ReportingService is a thin delegator to the pure calculators — assert it wires the repos through. */
class ReportingServiceTest {

    private val attendanceRepo: AttendanceRepository = mock()
    private val leaveRepo: LeaveRepository = mock()
    private val visitRepo: VisitRepository = mock()
    private val service = ReportingService(attendanceRepo, leaveRepo, visitRepo)

    private val from = Instant.parse("2026-06-01T00:00:00Z")
    private val to = Instant.parse("2026-06-30T00:00:00Z")

    @Test
    fun `attendance summary reads attendance + leaves for the rep`() {
        whenever(attendanceRepo.findByRepMemberUidAndCheckInAtBetween("REP-1", from, to)).thenReturn(emptyList<Attendance>())
        whenever(leaveRepo.findByRepMemberUidAndLeaveDateBetween("REP-1", from, to)).thenReturn(emptyList())
        val summary = service.attendanceSummary("REP-1", from, to)
        assertEquals("REP-1", summary.repMemberUid)
        assertEquals(0, summary.daysPresent)
    }

    @Test
    fun `visit productivity reads visits for the rep`() {
        whenever(visitRepo.findByRepMemberUidAndVisitedAtBetween("REP-1", from, to)).thenReturn(emptyList<Visit>())
        val prod = service.visitProductivity("REP-1", from, to)
        assertEquals("REP-1", prod.repMemberUid)
        assertEquals(0, prod.totalVisits)
    }
}

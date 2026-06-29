package com.ampairs.sfa.domain.service

import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.sfa.domain.enums.AttendanceStatus
import com.ampairs.sfa.domain.model.Attendance
import com.ampairs.sfa.repository.AttendanceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant

class AttendanceServiceTest {

    private val repo: AttendanceRepository = mock()
    private val publisher: EntityChangePublisher = mock()
    private val service = AttendanceService(repo, publisher)

    @Test
    fun `a new open attendance auto-closes a prior open one for the same rep`() {
        whenever(repo.save(any<Attendance>())).thenAnswer { it.arguments[0] as Attendance }
        whenever(repo.findByUid(any())).thenReturn(null)
        val prior = Attendance().apply { uid = "ATT-prior"; repMemberUid = "REP-1"; status = AttendanceStatus.OPEN; checkInAt = Instant.parse("2026-06-01T09:00:00Z") }
        whenever(repo.findByRepMemberUidAndStatusAndActiveTrue("REP-1", AttendanceStatus.OPEN)).thenReturn(listOf(prior))

        val incoming = Attendance().apply { uid = "ATT-new"; repMemberUid = "REP-1"; checkInAt = Instant.parse("2026-06-02T09:00:00Z") }
        val saved = service.bulkUpsertAttendance(listOf(incoming))

        assertEquals(AttendanceStatus.AUTO_CLOSED, prior.status)
        assertEquals(AttendanceStatus.OPEN, saved.first().status)
    }

    @Test
    fun `attendance with a checkout is CLOSED`() {
        whenever(repo.save(any<Attendance>())).thenAnswer { it.arguments[0] as Attendance }
        whenever(repo.findByUid(any())).thenReturn(null)
        val incoming = Attendance().apply {
            uid = "ATT-1"; repMemberUid = "REP-1"
            checkInAt = Instant.parse("2026-06-01T09:00:00Z"); checkOutAt = Instant.parse("2026-06-01T18:00:00Z")
        }
        val saved = service.bulkUpsertAttendance(listOf(incoming))
        assertEquals(AttendanceStatus.CLOSED, saved.first().status)
    }
}

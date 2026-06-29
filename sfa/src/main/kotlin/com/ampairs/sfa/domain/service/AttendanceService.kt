package com.ampairs.sfa.domain.service

import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.sfa.domain.enums.AttendanceStatus
import com.ampairs.sfa.domain.model.Attendance
import com.ampairs.sfa.repository.AttendanceRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Attendance check-in/check-out over `/sync`. Status is derived: OPEN until a check-out time
 * arrives, then CLOSED. (Single-open enforcement + scheduled auto-close land in Phase 8b.)
 */
@Service
@Transactional
class AttendanceService(
    private val attendanceRepository: AttendanceRepository,
    private val entityChangePublisher: EntityChangePublisher,
) {

    @Transactional(readOnly = true)
    fun getAttendanceAfterSync(lastSync: String?, pageable: Pageable): Page<Attendance> =
        syncFeed(lastSync, pageable, { attendanceRepository.findAll(it) }, { i, p -> attendanceRepository.findByUpdatedAtAfter(i, p) })

    fun bulkUpsertAttendance(incoming: List<Attendance>): List<Attendance> = incoming.map { row ->
        row.status = deriveStatus(row)
        val existing = row.uid.takeIf { it.isNotBlank() }?.let { attendanceRepository.findByUid(it) }
        if (existing != null) {
            existing.repMemberUid = row.repMemberUid
            existing.checkInAt = row.checkInAt
            existing.checkInLatitude = row.checkInLatitude
            existing.checkInLongitude = row.checkInLongitude
            existing.checkOutAt = row.checkOutAt
            existing.checkOutLatitude = row.checkOutLatitude
            existing.checkOutLongitude = row.checkOutLongitude
            existing.status = row.status
            existing.active = row.active
            attendanceRepository.save(existing).also { entityChangePublisher.updated("attendance", it.uid) }
        } else {
            attendanceRepository.save(row).also { entityChangePublisher.created("attendance", it.uid) }
        }
    }

    private fun deriveStatus(a: Attendance): AttendanceStatus =
        if (a.checkOutAt != null) AttendanceStatus.CLOSED else AttendanceStatus.OPEN
}

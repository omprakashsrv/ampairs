package com.ampairs.sfa.domain.service

import com.ampairs.sfa.domain.AttendanceSummaryCalculator
import com.ampairs.sfa.domain.VisitProductivityCalculator
import com.ampairs.sfa.domain.dto.AttendanceSummaryResponse
import com.ampairs.sfa.domain.dto.VisitProductivityResponse
import com.ampairs.sfa.repository.AttendanceRepository
import com.ampairs.sfa.repository.LeaveRepository
import com.ampairs.sfa.repository.VisitRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Read-only field-ops reporting (mirrors the `payment` AgingService read-model pattern): derives
 * attendance summaries and visit productivity from the captured rows — no stored aggregates.
 */
@Service
@Transactional(readOnly = true)
class ReportingService(
    private val attendanceRepository: AttendanceRepository,
    private val leaveRepository: LeaveRepository,
    private val visitRepository: VisitRepository,
) {

    fun attendanceSummary(repMemberUid: String, from: Instant, to: Instant): AttendanceSummaryResponse {
        val attendance = attendanceRepository.findByRepMemberUidAndCheckInAtBetween(repMemberUid, from, to)
        val leaves = leaveRepository.findByRepMemberUidAndLeaveDateBetween(repMemberUid, from, to)
        return AttendanceSummaryCalculator.summarize(repMemberUid, attendance, leaves)
    }

    fun visitProductivity(repMemberUid: String, from: Instant, to: Instant): VisitProductivityResponse {
        val visits = visitRepository.findByRepMemberUidAndVisitedAtBetween(repMemberUid, from, to)
        return VisitProductivityCalculator.summarize(repMemberUid, visits)
    }
}

package com.ampairs.sfa.domain

import com.ampairs.sfa.domain.dto.AttendanceSummaryResponse
import com.ampairs.sfa.domain.dto.VisitProductivityResponse
import com.ampairs.sfa.domain.enums.VisitOutcome
import com.ampairs.sfa.domain.model.Attendance
import com.ampairs.sfa.domain.model.Leave
import com.ampairs.sfa.domain.model.Visit
import java.time.Duration

/** Pure attendance-summary computation (FR-AS1–7). Excused leave is reported separately, never "absent". */
object AttendanceSummaryCalculator {

    fun summarize(
        repMemberUid: String,
        attendance: List<Attendance>,
        leaves: List<Leave>,
    ): AttendanceSummaryResponse {
        val active = attendance.filter { it.active }
        val daysPresent = active.count { it.checkInAt != null }
        val openDays = active.count { it.checkInAt != null && it.checkOutAt == null }
        val totalWorkingHours = active.sumOf { a ->
            val inAt = a.checkInAt
            val outAt = a.checkOutAt
            if (inAt != null && outAt != null && outAt.isAfter(inAt)) {
                Duration.between(inAt, outAt).toMinutes() / 60.0
            } else {
                0.0
            }
        }
        val leaveDays = leaves.count { it.active }
        return AttendanceSummaryResponse(
            repMemberUid = repMemberUid,
            daysPresent = daysPresent,
            totalWorkingHours = totalWorkingHours,
            openDays = openDays,
            leaveDays = leaveDays,
        )
    }
}

/** Pure visit-productivity computation (FR-VP1–7): productive-call % + unique-outlet coverage. */
object VisitProductivityCalculator {

    fun summarize(repMemberUid: String, visits: List<Visit>): VisitProductivityResponse {
        val active = visits.filter { it.active }
        val total = active.size
        val productive = active.count { it.outcome == VisitOutcome.PRODUCTIVE }
        val productivePercent = if (total > 0) productive * 100.0 / total else 0.0
        val uniqueOutlets = active.map { it.customerUid }.toSet().size
        val adHoc = active.count { it.adHoc }
        return VisitProductivityResponse(
            repMemberUid = repMemberUid,
            totalVisits = total,
            productiveVisits = productive,
            productivePercent = productivePercent,
            uniqueOutlets = uniqueOutlets,
            adHocVisits = adHoc,
        )
    }
}

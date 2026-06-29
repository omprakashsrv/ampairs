package com.ampairs.sfa.domain

import com.ampairs.sfa.domain.dto.AdherenceSummary
import com.ampairs.sfa.domain.enums.PlannedVisitStatus
import com.ampairs.sfa.domain.model.PlannedVisit
import com.ampairs.sfa.domain.model.Visit

/**
 * Pure beat-adherence computation (FR-017): planned vs actual, with ad-hoc visits counted
 * separately from planned-visit adherence. Kept dependency-free so it is trivially unit-testable.
 */
object AdherenceCalculator {

    fun summarize(
        repMemberUid: String,
        planned: List<PlannedVisit>,
        visits: List<Visit>,
    ): AdherenceSummary {
        val activePlanned = planned.filter { it.active }
        val plannedCount = activePlanned.size
        val visitedCount = activePlanned.count { it.status == PlannedVisitStatus.VISITED }
        val missedCount = activePlanned.count { it.status == PlannedVisitStatus.MISSED }
        val pendingCount = activePlanned.count { it.status == PlannedVisitStatus.PENDING }
        val adHocCount = visits.count { it.active && it.adHoc }
        val adherencePercent = if (plannedCount > 0) visitedCount * 100.0 / plannedCount else 0.0
        return AdherenceSummary(
            repMemberUid = repMemberUid,
            plannedCount = plannedCount,
            visitedCount = visitedCount,
            missedCount = missedCount,
            pendingCount = pendingCount,
            adHocCount = adHocCount,
            adherencePercent = adherencePercent,
        )
    }
}

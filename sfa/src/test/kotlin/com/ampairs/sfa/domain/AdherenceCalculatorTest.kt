package com.ampairs.sfa.domain

import com.ampairs.sfa.domain.enums.PlannedVisitStatus
import com.ampairs.sfa.domain.enums.VisitOutcome
import com.ampairs.sfa.domain.model.PlannedVisit
import com.ampairs.sfa.domain.model.Visit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AdherenceCalculatorTest {

    private fun planned(status: PlannedVisitStatus, active: Boolean = true) = PlannedVisit().apply {
        this.status = status
        this.active = active
        repMemberUid = "REP-1"
    }

    private fun visit(adHoc: Boolean, active: Boolean = true) = Visit().apply {
        this.adHoc = adHoc
        this.active = active
        outcome = VisitOutcome.PRODUCTIVE
        repMemberUid = "REP-1"
    }

    @Test
    fun `adherence percent is visited over planned`() {
        val planned = listOf(
            planned(PlannedVisitStatus.VISITED),
            planned(PlannedVisitStatus.VISITED),
            planned(PlannedVisitStatus.MISSED),
            planned(PlannedVisitStatus.PENDING),
        )
        val summary = AdherenceCalculator.summarize("REP-1", planned, emptyList())
        assertEquals(4, summary.plannedCount)
        assertEquals(2, summary.visitedCount)
        assertEquals(1, summary.missedCount)
        assertEquals(1, summary.pendingCount)
        assertEquals(50.0, summary.adherencePercent)
    }

    @Test
    fun `ad-hoc visits are counted separately and not in planned adherence`() {
        val planned = listOf(planned(PlannedVisitStatus.VISITED))
        val visits = listOf(visit(adHoc = true), visit(adHoc = true), visit(adHoc = false))
        val summary = AdherenceCalculator.summarize("REP-1", planned, visits)
        assertEquals(2, summary.adHocCount)
        assertEquals(1, summary.plannedCount)
        assertEquals(100.0, summary.adherencePercent)
    }

    @Test
    fun `inactive planned visits are excluded`() {
        val planned = listOf(
            planned(PlannedVisitStatus.VISITED),
            planned(PlannedVisitStatus.VISITED, active = false),
        )
        val summary = AdherenceCalculator.summarize("REP-1", planned, emptyList())
        assertEquals(1, summary.plannedCount)
    }

    @Test
    fun `zero planned yields zero percent not divide-by-zero`() {
        val summary = AdherenceCalculator.summarize("REP-1", emptyList(), emptyList())
        assertEquals(0.0, summary.adherencePercent)
        assertEquals(0, summary.plannedCount)
    }
}

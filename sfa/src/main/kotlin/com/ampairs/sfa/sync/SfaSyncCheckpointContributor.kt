package com.ampairs.sfa.sync

import com.ampairs.core.sync.SyncCheckpointContributor
import com.ampairs.sfa.repository.AttendanceRepository
import com.ampairs.sfa.repository.BeatOutletRepository
import com.ampairs.sfa.repository.BeatRepository
import com.ampairs.sfa.repository.FieldOrderRepository
import com.ampairs.sfa.repository.JourneyPlanRepository
import com.ampairs.sfa.repository.LeaveRepository
import com.ampairs.sfa.repository.PlannedVisitRepository
import com.ampairs.sfa.repository.VisitRepository
import com.ampairs.sfa.repository.VisitSurveyResponseRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes the SFA module's per-entity sync checkpoints (max `updatedAt` for the current
 * workspace) so the mobile client pulls only the entities that have lagged. @TenantId-scoped.
 */
@Component
class SfaSyncCheckpointContributor(
    private val beatRepository: BeatRepository,
    private val beatOutletRepository: BeatOutletRepository,
    private val journeyPlanRepository: JourneyPlanRepository,
    private val plannedVisitRepository: PlannedVisitRepository,
    private val visitRepository: VisitRepository,
    private val attendanceRepository: AttendanceRepository,
    private val fieldOrderRepository: FieldOrderRepository,
    private val leaveRepository: LeaveRepository,
    private val visitSurveyResponseRepository: VisitSurveyResponseRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> = mapOf(
        "beat" to beatRepository.findMaxUpdatedAt(),
        "beat_outlet" to beatOutletRepository.findMaxUpdatedAt(),
        "journey_plan" to journeyPlanRepository.findMaxUpdatedAt(),
        "planned_visit" to plannedVisitRepository.findMaxUpdatedAt(),
        "visit" to visitRepository.findMaxUpdatedAt(),
        "attendance" to attendanceRepository.findMaxUpdatedAt(),
        "field_order" to fieldOrderRepository.findMaxUpdatedAt(),
        "leave" to leaveRepository.findMaxUpdatedAt(),
        "visit_survey_response" to visitSurveyResponseRepository.findMaxUpdatedAt(),
    )
}

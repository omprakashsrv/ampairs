package com.ampairs.sfa.domain.service

import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.sfa.domain.AdherenceCalculator
import com.ampairs.sfa.domain.dto.AdherenceSummary
import com.ampairs.sfa.domain.model.JourneyPlan
import com.ampairs.sfa.domain.model.PlannedVisit
import com.ampairs.sfa.repository.JourneyPlanRepository
import com.ampairs.sfa.repository.PlannedVisitRepository
import com.ampairs.sfa.repository.VisitRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Journey plans (PJP) + planned visits over `/sync`, plus beat-adherence reporting (FR-017/SC-010).
 */
@Service
@Transactional
class JourneyPlanService(
    private val journeyPlanRepository: JourneyPlanRepository,
    private val plannedVisitRepository: PlannedVisitRepository,
    private val visitRepository: VisitRepository,
    private val entityChangePublisher: EntityChangePublisher,
) {

    @Transactional(readOnly = true)
    fun getJourneyPlansAfterSync(lastSync: String?, pageable: Pageable): Page<JourneyPlan> =
        syncFeed(lastSync, pageable, { journeyPlanRepository.findAll(it) }, { i, p -> journeyPlanRepository.findByUpdatedAtAfter(i, p) })

    fun bulkUpsertJourneyPlans(incoming: List<JourneyPlan>): List<JourneyPlan> = incoming.map { row ->
        val existing = row.uid.takeIf { it.isNotBlank() }?.let { journeyPlanRepository.findByUid(it) }
        if (existing != null) {
            existing.repMemberUid = row.repMemberUid
            existing.beatUid = row.beatUid
            existing.weekday = row.weekday
            existing.active = row.active
            journeyPlanRepository.save(existing).also { entityChangePublisher.updated("journey_plan", it.uid) }
        } else {
            journeyPlanRepository.save(row).also { entityChangePublisher.created("journey_plan", it.uid) }
        }
    }

    @Transactional(readOnly = true)
    fun getPlannedVisitsAfterSync(lastSync: String?, pageable: Pageable): Page<PlannedVisit> =
        syncFeed(lastSync, pageable, { plannedVisitRepository.findAll(it) }, { i, p -> plannedVisitRepository.findByUpdatedAtAfter(i, p) })

    fun bulkUpsertPlannedVisits(incoming: List<PlannedVisit>): List<PlannedVisit> = incoming.map { row ->
        val existing = row.uid.takeIf { it.isNotBlank() }?.let { plannedVisitRepository.findByUid(it) }
        if (existing != null) {
            existing.journeyPlanUid = row.journeyPlanUid
            existing.beatUid = row.beatUid
            existing.customerUid = row.customerUid
            existing.repMemberUid = row.repMemberUid
            existing.plannedDate = row.plannedDate
            existing.status = row.status
            existing.visitSequence = row.visitSequence
            existing.active = row.active
            plannedVisitRepository.save(existing).also { entityChangePublisher.updated("planned_visit", it.uid) }
        } else {
            plannedVisitRepository.save(row).also { entityChangePublisher.created("planned_visit", it.uid) }
        }
    }

    /**
     * Beat adherence for a rep over [from, to]: planned vs visited, with ad-hoc visits counted
     * separately (FR-017). Computation is delegated to the pure [AdherenceCalculator].
     */
    @Transactional(readOnly = true)
    fun adherence(repMemberUid: String, from: Instant, to: Instant): AdherenceSummary {
        val planned = plannedVisitRepository.findByRepMemberUidAndPlannedDateBetween(repMemberUid, from, to)
        val visits = visitRepository.findByRepMemberUidAndVisitedAtBetween(repMemberUid, from, to)
        return AdherenceCalculator.summarize(repMemberUid, planned, visits)
    }
}

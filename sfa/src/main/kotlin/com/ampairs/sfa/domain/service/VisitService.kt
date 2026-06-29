package com.ampairs.sfa.domain.service

import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.sfa.config.Constants
import com.ampairs.sfa.domain.GeoFenceCalculator
import com.ampairs.sfa.domain.enums.PlannedVisitStatus
import com.ampairs.sfa.domain.model.Visit
import com.ampairs.sfa.exception.SfaValidationException
import com.ampairs.sfa.repository.PlannedVisitRepository
import com.ampairs.sfa.repository.VisitRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Visit capture over `/sync`. Enforces the ad-hoc rule, computes the informational geo-fence flag
 * (never blocks), and reconciles a fulfilled [com.ampairs.sfa.domain.model.PlannedVisit] to VISITED.
 */
@Service
@Transactional
class VisitService(
    private val visitRepository: VisitRepository,
    private val plannedVisitRepository: PlannedVisitRepository,
    private val entityChangePublisher: EntityChangePublisher,
) {

    @Transactional(readOnly = true)
    fun getVisitsAfterSync(lastSync: String?, pageable: Pageable): Page<Visit> =
        syncFeed(lastSync, pageable, { visitRepository.findAll(it) }, { i, p -> visitRepository.findByUpdatedAtAfter(i, p) })

    fun bulkUpsertVisits(incoming: List<Visit>): List<Visit> = incoming.map { row ->
        validateAdHoc(row)
        // Informational geo-fence flag (distance null ⇒ NO_LOCATION). Never blocks.
        row.geoFenceStatus = GeoFenceCalculator.classify(row.distanceMeters, Constants.DEFAULT_GEO_FENCE_RADIUS_METERS)

        val existing = row.uid.takeIf { it.isNotBlank() }?.let { visitRepository.findByUid(it) }
        val saved = if (existing != null) {
            existing.customerUid = row.customerUid
            existing.repMemberUid = row.repMemberUid
            existing.plannedVisitUid = row.plannedVisitUid
            existing.outcome = row.outcome
            existing.latitude = row.latitude
            existing.longitude = row.longitude
            existing.distanceMeters = row.distanceMeters
            existing.geoFenceStatus = row.geoFenceStatus
            existing.adHoc = row.adHoc
            existing.notes = row.notes
            existing.orderUid = row.orderUid
            existing.visitedAt = row.visitedAt
            existing.active = row.active
            visitRepository.save(existing).also { entityChangePublisher.updated("visit", it.uid) }
        } else {
            visitRepository.save(row).also { entityChangePublisher.created("visit", it.uid) }
        }
        reconcilePlannedVisit(saved)
        saved
    }

    private fun validateAdHoc(visit: Visit) {
        val hasPlanned = !visit.plannedVisitUid.isNullOrBlank()
        if (!visit.adHoc && !hasPlanned) {
            throw SfaValidationException("A planned_visit_uid is required when ad_hoc = false")
        }
        if (visit.adHoc && hasPlanned) {
            throw SfaValidationException("An ad-hoc visit must not reference a planned_visit_uid")
        }
    }

    /** A planned visit fulfilled by an active, non-ad-hoc visit is marked VISITED. */
    private fun reconcilePlannedVisit(visit: Visit) {
        if (visit.adHoc || !visit.active) return
        val plannedUid = visit.plannedVisitUid ?: return
        val planned = plannedVisitRepository.findByUid(plannedUid) ?: return
        if (planned.status != PlannedVisitStatus.VISITED) {
            planned.status = PlannedVisitStatus.VISITED
            plannedVisitRepository.save(planned)
            entityChangePublisher.updated("planned_visit", planned.uid)
        }
    }
}

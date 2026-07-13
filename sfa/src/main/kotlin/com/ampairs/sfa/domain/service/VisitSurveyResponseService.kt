package com.ampairs.sfa.domain.service

import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.sfa.domain.model.VisitSurveyResponse
import com.ampairs.sfa.repository.VisitSurveyResponseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Offline-captured store-visit survey responses over `/sync` (UID-keyed, soft-delete-inclusive). */
@Service
@Transactional
class VisitSurveyResponseService(
    private val repository: VisitSurveyResponseRepository,
    private val entityChangePublisher: EntityChangePublisher,
) {

    @Transactional(readOnly = true)
    fun getAfterSync(lastSync: String?, pageable: Pageable): Page<VisitSurveyResponse> =
        syncFeed(lastSync, pageable, { repository.findAll(it) }, { i, p -> repository.findByUpdatedAtAfter(i, p) })

    fun bulkUpsert(incoming: List<VisitSurveyResponse>): List<VisitSurveyResponse> = incoming.map { row ->
        val existing = row.uid.takeIf { it.isNotBlank() }?.let { repository.findByUid(it) }
        if (existing != null) {
            existing.visitUid = row.visitUid
            existing.repMemberUid = row.repMemberUid
            existing.responses = row.responses
            existing.active = row.active
            repository.save(existing).also { entityChangePublisher.updated("visit_survey_response", it.uid) }
        } else {
            repository.save(row).also { entityChangePublisher.created("visit_survey_response", it.uid) }
        }
    }
}

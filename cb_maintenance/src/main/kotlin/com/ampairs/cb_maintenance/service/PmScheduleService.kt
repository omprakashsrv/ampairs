package com.ampairs.cb_maintenance.service

import com.ampairs.cb_maintenance.domain.dto.PmScheduleRequest
import com.ampairs.cb_maintenance.domain.dto.PmScheduleResponse
import com.ampairs.cb_maintenance.domain.dto.applyRequest
import com.ampairs.cb_maintenance.domain.dto.asPmScheduleResponse
import com.ampairs.cb_maintenance.domain.model.PmSchedule
import com.ampairs.cb_maintenance.repository.PmScheduleRepository
import com.ampairs.core.sync.EntityChangePublisher
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

@Service
class PmScheduleService(
    private val repository: PmScheduleRepository,
    private val entityChangePublisher: EntityChangePublisher,
) {
    private val logger = LoggerFactory.getLogger(PmScheduleService::class.java)

    @Transactional(readOnly = true)
    fun findByUid(uid: String): PmScheduleResponse? = repository.findByUid(uid)?.asPmScheduleResponse()

    @Transactional
    fun create(request: PmScheduleRequest): PmScheduleResponse {
        val saved = repository.save(PmSchedule().applyRequest(request))
        entityChangePublisher.created("cb_pm_schedule", saved.uid)
        return saved.asPmScheduleResponse()
    }

    @Transactional
    fun delete(uid: String) {
        val schedule = repository.findByUid(uid)
            ?: throw MaintenanceNotFoundException("PM schedule not found for uid: $uid")
        schedule.active = false
        repository.save(schedule)
        entityChangePublisher.deleted("cb_pm_schedule", schedule.uid)
    }

    @Transactional(readOnly = true)
    fun getAfterSync(lastSync: String?, pageable: Pageable): Page<PmScheduleResponse> {
        val page: Page<PmSchedule> = if (lastSync.isNullOrBlank()) {
            repository.findAllForSync(pageable)
        } else {
            try {
                repository.findByUpdatedAtAfter(
                    Instant.parse(URLDecoder.decode(lastSync, StandardCharsets.UTF_8)), pageable,
                )
            } catch (e: Exception) {
                logger.warn("Invalid last_sync '{}', full feed", lastSync, e)
                repository.findAllForSync(pageable)
            }
        }
        return page.map { it.asPmScheduleResponse() }
    }

    @Transactional
    fun bulkUpsert(requests: List<PmScheduleRequest>): List<PmScheduleResponse> =
        requests.map { request ->
            val existing = request.uid?.takeIf { it.isNotBlank() }?.let { repository.findByUid(it) }
            val entity = (existing ?: PmSchedule()).applyRequest(request)
            repository.save(entity)
                .also { entityChangePublisher.updated("cb_pm_schedule", it.uid) }
                .asPmScheduleResponse()
        }
}

package com.ampairs.communication.service.trigger

import com.ampairs.communication.domain.dto.BindingRequest
import com.ampairs.communication.domain.dto.BindingResponse
import com.ampairs.communication.domain.dto.applyRequest
import com.ampairs.communication.domain.dto.asResponse
import com.ampairs.communication.domain.model.EventTemplateBinding
import com.ampairs.communication.repository.EventTemplateBindingRepository
import com.ampairs.core.sync.EntityChangePublisher
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

/** CRUD + `/sync` for event→template bindings; also the lookup used by the transactional listener. */
@Service
class BindingService(
    private val repository: EventTemplateBindingRepository,
    private val entityChangePublisher: EntityChangePublisher,
) {
    private val logger = LoggerFactory.getLogger(BindingService::class.java)

    @Transactional(readOnly = true)
    fun getAfterSync(lastSync: String?, pageable: Pageable): Page<BindingResponse> {
        val page: Page<EventTemplateBinding> = if (lastSync.isNullOrBlank()) {
            repository.findAllForSync(pageable)
        } else {
            try {
                repository.findByUpdatedAtAfter(Instant.parse(URLDecoder.decode(lastSync, StandardCharsets.UTF_8)), pageable)
            } catch (e: Exception) {
                logger.warn("Invalid last_sync '{}', full feed", lastSync, e)
                repository.findAllForSync(pageable)
            }
        }
        return page.map { it.asResponse() }
    }

    @Transactional
    fun bulkUpsert(requests: List<BindingRequest>): List<BindingResponse> = requests.map { req ->
        val entity = (repository.findByUid(req.uid) ?: EventTemplateBinding()).applyRequest(req)
        repository.save(entity)
            .also { entityChangePublisher.updated("event_template_binding", it.uid) }
            .asResponse()
    }

    @Transactional(readOnly = true)
    fun findForEvent(eventType: String): EventTemplateBinding? =
        repository.findByEventTypeAndEnabledTrueAndActiveTrue(eventType.uppercase())
}

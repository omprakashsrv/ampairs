package com.ampairs.communication.service.template

import com.ampairs.communication.domain.dto.TemplateAggregateRequest
import com.ampairs.communication.domain.dto.TemplateAggregateResponse
import com.ampairs.communication.domain.dto.applyHeader
import com.ampairs.communication.domain.dto.applyRequest
import com.ampairs.communication.domain.dto.asResponse
import com.ampairs.communication.domain.model.MessageTemplate
import com.ampairs.communication.domain.model.TemplateVariant
import com.ampairs.communication.repository.MessageTemplateRepository
import com.ampairs.communication.repository.TemplateVariantRepository
import com.ampairs.communication.service.TemplateVersionConflictException
import com.ampairs.core.sync.EntityChangePublisher
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

/**
 * Aggregate template service for the `/sync` contract: upsert template header + variants together,
 * delete variants by absence, enforce `base_version` optimistic concurrency. Also resolves a
 * template + its variants by code for the send/preview paths.
 */
@Service
class TemplateService(
    private val templateRepository: MessageTemplateRepository,
    private val variantRepository: TemplateVariantRepository,
    private val entityChangePublisher: EntityChangePublisher,
) {
    private val logger = LoggerFactory.getLogger(TemplateService::class.java)

    @Transactional(readOnly = true)
    fun getTemplatesAfterSync(lastSync: String?, pageable: Pageable): Page<TemplateAggregateResponse> {
        val page: Page<MessageTemplate> = if (lastSync.isNullOrBlank()) {
            templateRepository.findAllForSync(pageable)
        } else {
            try {
                val decoded = URLDecoder.decode(lastSync, StandardCharsets.UTF_8)
                templateRepository.findByUpdatedAtAfter(Instant.parse(decoded), pageable)
            } catch (e: Exception) {
                logger.warn("Invalid last_sync '{}', falling back to full feed", lastSync, e)
                templateRepository.findAllForSync(pageable)
            }
        }
        val templateUids = page.content.map { it.uid }
        val variantsByTemplate = if (templateUids.isEmpty()) emptyMap()
        else variantRepository.findByTemplateUidIn(templateUids).groupBy { it.templateUid }
        return page.map { it.asResponse(variantsByTemplate[it.uid].orEmpty()) }
    }

    @Transactional
    fun bulkUpsert(requests: List<TemplateAggregateRequest>): List<TemplateAggregateResponse> =
        requests.map { upsertAggregate(it) }

    private fun upsertAggregate(request: TemplateAggregateRequest): TemplateAggregateResponse {
        val existing = templateRepository.findByUid(request.uid)
        if (existing != null && existing.baseVersion > request.baseVersion) {
            throw TemplateVersionConflictException(
                existing.code,
                "Stale template '${existing.code}' (server base_version ${existing.baseVersion} > ${request.baseVersion})"
            )
        }
        val header = (existing ?: MessageTemplate()).applyHeader(request)
        val saved = templateRepository.save(header)

        // Reconcile variants: upsert provided, delete-by-absence the rest.
        val incomingUids = request.variants.map { it.uid }.toSet()
        val current = variantRepository.findByTemplateUid(saved.uid)
        current.filter { it.uid !in incomingUids && it.active }.forEach {
            it.active = false
            variantRepository.save(it)
        }
        request.variants.forEach { v ->
            val variant = (variantRepository.findByUid(v.uid) ?: TemplateVariant()).applyRequest(v, saved.uid)
            variantRepository.save(variant)
        }

        if (existing == null) entityChangePublisher.created("message_template", saved.uid)
        else entityChangePublisher.updated("message_template", saved.uid)

        return saved.asResponse(variantRepository.findByTemplateUid(saved.uid))
    }

    @Transactional(readOnly = true)
    fun findByCode(code: String): Pair<MessageTemplate, List<TemplateVariant>>? {
        val template = templateRepository.findByCode(code) ?: return null
        return template to variantRepository.findByTemplateUid(template.uid).filter { it.active }
    }

    @Transactional(readOnly = true)
    fun findByUid(uid: String): Pair<MessageTemplate, List<TemplateVariant>>? {
        val template = templateRepository.findByUid(uid) ?: return null
        return template to variantRepository.findByTemplateUid(template.uid).filter { it.active }
    }
}

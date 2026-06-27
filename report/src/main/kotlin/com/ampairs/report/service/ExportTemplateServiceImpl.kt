package com.ampairs.report.service

import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.report.domain.dto.ExportFilterDto
import com.ampairs.report.domain.dto.ExportTemplateRequest
import com.ampairs.report.domain.dto.ExportTemplateResponse
import com.ampairs.report.domain.model.ExportTemplate
import com.ampairs.report.repository.ExportTemplateRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

@Service
class ExportTemplateServiceImpl(
    private val repository: ExportTemplateRepository,
    private val objectMapper: ObjectMapper,
    private val entityChangePublisher: EntityChangePublisher,
) : ExportTemplateService {

    private val logger = LoggerFactory.getLogger(ExportTemplateServiceImpl::class.java)

    private val columnListType = object : TypeReference<List<String>>() {}
    private val filterListType = object : TypeReference<List<ExportFilterDto>>() {}

    @Transactional(readOnly = true)
    override fun findByUid(uid: String): ExportTemplateResponse? {
        if (uid.isBlank()) return null
        return repository.findByUid(uid)?.toResponse()
    }

    @Transactional(readOnly = true)
    override fun getTemplatesAfterSync(lastSync: String?, pageable: Pageable): Page<ExportTemplateResponse> {
        val page: Page<ExportTemplate> = if (lastSync.isNullOrBlank()) {
            repository.findAllForSync(pageable)
        } else {
            try {
                val decoded = URLDecoder.decode(lastSync, StandardCharsets.UTF_8)
                repository.findByUpdatedAtAfter(Instant.parse(decoded), pageable)
            } catch (e: Exception) {
                logger.warn("Invalid last_sync '{}', falling back to full sync feed", lastSync, e)
                repository.findAllForSync(pageable)
            }
        }
        return page.map { it.toResponse() }
    }

    @Transactional
    override fun bulkUpsert(requests: List<ExportTemplateRequest>): List<ExportTemplateResponse> {
        return requests.map { request ->
            val existing = request.uid?.takeIf { it.isNotBlank() }?.let { repository.findByUid(it) }
            if (existing != null) {
                existing.applyRequest(request)
                repository.save(existing)
                    .also { entityChangePublisher.updated("export_template", it.uid) }
                    .toResponse()
            } else {
                val template = ExportTemplate().applyRequest(request)
                repository.save(template)
                    .also { entityChangePublisher.created("export_template", it.uid) }
                    .toResponse()
            }
        }
    }

    private fun ExportTemplate.applyRequest(req: ExportTemplateRequest): ExportTemplate = apply {
        req.uid?.takeIf { it.isNotBlank() }?.let { uid = it }
        moduleKey = req.moduleKey.trim()
        name = req.name.trim()
        selectedColumns = objectMapper.writeValueAsString(req.selectedColumns)
        filters = objectMapper.writeValueAsString(req.filters)
        sortBy = req.sortBy?.trim()
        sortDir = req.sortDir
        defaultFormat = req.defaultFormat
        defaultLocation = req.defaultLocation
        includeInactive = req.includeInactive
        active = req.active
        // Keep the client ref_id (e.g. Tally GUID); never wipe a stored one with a blank.
        req.refId?.takeIf { it.isNotBlank() }?.let { refId = it }
    }

    private fun ExportTemplate.toResponse(): ExportTemplateResponse = ExportTemplateResponse(
        uid = uid,
        refId = refId,
        moduleKey = moduleKey,
        name = name,
        selectedColumns = readJson(selectedColumns, columnListType),
        filters = readJson(filters, filterListType),
        sortBy = sortBy,
        sortDir = sortDir,
        defaultFormat = defaultFormat,
        defaultLocation = defaultLocation,
        includeInactive = includeInactive,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun <T> readJson(raw: String?, type: TypeReference<List<T>>): List<T> {
        val json = raw?.takeIf { it.isNotBlank() } ?: return emptyList()
        return runCatching { objectMapper.readValue(json, type) }
            .onFailure { logger.warn("Failed to parse stored JSON column: {}", json, it) }
            .getOrDefault(emptyList())
    }
}

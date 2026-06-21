package com.ampairs.workspace.printing.service

import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.workspace.printing.domain.dto.PrintTemplateRequest
import com.ampairs.workspace.printing.domain.dto.PrintTemplateResponse
import com.ampairs.workspace.printing.domain.dto.applyRequest
import com.ampairs.workspace.printing.domain.dto.asResponse
import com.ampairs.workspace.printing.domain.model.PrintTemplate
import com.ampairs.workspace.printing.repository.PrintTemplateRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

@Service
class PrintTemplateServiceImpl(
    private val repository: PrintTemplateRepository,
    private val entityChangePublisher: EntityChangePublisher,
) : PrintTemplateService {

    private val logger = LoggerFactory.getLogger(PrintTemplateServiceImpl::class.java)

    @Transactional(readOnly = true)
    override fun getTemplatesAfterSync(lastSync: String?, pageable: Pageable): Page<PrintTemplateResponse> {
        val page: Page<PrintTemplate> = if (lastSync.isNullOrBlank()) {
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
        return page.map { it.asResponse() }
    }

    @Transactional
    override fun bulkUpsert(requests: List<PrintTemplateRequest>): List<PrintTemplateResponse> {
        return requests.map { request ->
            val existing = request.id.takeIf { it.isNotBlank() }?.let { repository.findByUid(it) }
            if (existing != null) {
                existing.applyRequest(request)
                repository.save(existing)
                    .also { entityChangePublisher.updated("print_template", it.uid) }
                    .asResponse()
            } else {
                val template = PrintTemplate().applyRequest(request)
                repository.save(template)
                    .also { entityChangePublisher.created("print_template", it.uid) }
                    .asResponse()
            }
        }
    }

    @Transactional(readOnly = true)
    override fun findByUid(uid: String): PrintTemplateResponse? {
        if (uid.isBlank()) return null
        return repository.findByUid(uid)?.asResponse()
    }
}

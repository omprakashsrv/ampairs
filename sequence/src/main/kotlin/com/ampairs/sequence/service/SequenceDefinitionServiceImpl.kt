package com.ampairs.sequence.service

import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.sequence.config.Constants
import com.ampairs.sequence.domain.dto.SequenceDefinitionRequest
import com.ampairs.sequence.domain.dto.SequenceDefinitionResponse
import com.ampairs.sequence.domain.dto.SequenceNumberResponse
import com.ampairs.sequence.domain.dto.SequencePreviewResponse
import com.ampairs.sequence.domain.dto.asSequenceDefinitionResponse
import com.ampairs.sequence.domain.model.SequenceDefinition
import com.ampairs.sequence.domain.model.SequenceScope
import com.ampairs.sequence.exception.DuplicateSequenceDefinitionException
import com.ampairs.sequence.exception.InvalidSequenceRequestException
import com.ampairs.sequence.exception.SequenceDefinitionNotFoundException
import com.ampairs.sequence.repository.SequenceDefinitionRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

@Service
class SequenceDefinitionServiceImpl(
    private val definitionRepository: SequenceDefinitionRepository,
    private val counterService: SequenceCounterService,
    private val entityChangePublisher: EntityChangePublisher,
) : SequenceDefinitionService {

    private val logger = LoggerFactory.getLogger(SequenceDefinitionServiceImpl::class.java)

    @Transactional(readOnly = true)
    override fun findAll(): List<SequenceDefinitionResponse> {
        return definitionRepository.findAllByOrderByActiveDescEntityTypeAsc()
            .map { it.asSequenceDefinitionResponse() }
    }

    @Transactional(readOnly = true)
    override fun findByUid(uid: String): SequenceDefinitionResponse? {
        if (uid.isBlank()) return null
        return definitionRepository.findByUid(uid)?.asSequenceDefinitionResponse()
    }

    @Transactional(readOnly = true)
    override fun getDefinitionsAfterSync(lastSync: String?, pageable: Pageable): Page<SequenceDefinitionResponse> {
        val page: Page<SequenceDefinition> = if (lastSync.isNullOrBlank()) {
            definitionRepository.findAllForSync(pageable)
        } else {
            try {
                val decoded = URLDecoder.decode(lastSync, StandardCharsets.UTF_8)
                definitionRepository.findByUpdatedAtAfter(Instant.parse(decoded), pageable)
            } catch (e: Exception) {
                logger.warn("Invalid last_sync '{}', falling back to full sync feed", lastSync, e)
                definitionRepository.findAllForSync(pageable)
            }
        }
        return page.map { it.asSequenceDefinitionResponse() }
    }

    @Transactional
    override fun bulkUpsert(requests: List<SequenceDefinitionRequest>): List<SequenceDefinitionResponse> {
        return requests.map { request ->
            val existing = request.uid?.takeIf { it.isNotBlank() }?.let { definitionRepository.findByUid(it) }
            if (existing != null) {
                applyRequest(existing, request, strictCounter = false)
                definitionRepository.save(existing)
                    .also { entityChangePublisher.updated(Constants.ENTITY_TYPE_SEQUENCE, it.uid) }
                    .asSequenceDefinitionResponse()
            } else {
                val definition = applyRequest(SequenceDefinition(), request, strictCounter = false)
                definitionRepository.save(definition)
                    .also { entityChangePublisher.created(Constants.ENTITY_TYPE_SEQUENCE, it.uid) }
                    .asSequenceDefinitionResponse()
            }
        }
    }

    @Transactional
    override fun create(request: SequenceDefinitionRequest): SequenceDefinitionResponse {
        val definition = applyRequest(SequenceDefinition(), request.copy(active = true), strictCounter = true)
        val saved = definitionRepository.save(definition)
        entityChangePublisher.created(Constants.ENTITY_TYPE_SEQUENCE, saved.uid)
        return saved.asSequenceDefinitionResponse()
    }

    @Transactional
    override fun update(uid: String, request: SequenceDefinitionRequest): SequenceDefinitionResponse {
        val existing = definitionRepository.findByUid(uid)
            ?: throw SequenceDefinitionNotFoundException("Sequence definition not found for uid: $uid")
        applyRequest(existing, request, strictCounter = true)
        val saved = definitionRepository.save(existing)
        entityChangePublisher.updated(Constants.ENTITY_TYPE_SEQUENCE, saved.uid)
        return saved.asSequenceDefinitionResponse()
    }

    @Transactional(readOnly = true)
    override fun preview(uid: String): SequencePreviewResponse {
        val definition = definitionRepository.findByUid(uid)
            ?: throw SequenceDefinitionNotFoundException("Sequence definition not found for uid: $uid")
        val nextValue = SequenceFormatter.nextValue(
            definition.currentValue, definition.startValue, definition.incrementStep
        )
        return SequencePreviewResponse(
            definitionUid = definition.uid,
            nextValue = nextValue,
            formatted = SequenceFormatter.format(
                definition.prefix, definition.suffix, definition.paddingLength, nextValue
            ),
        )
    }

    @Transactional
    override fun next(entityType: String, userId: String?): SequenceNumberResponse {
        val resolved = counterService.resolveOrProvision(entityType, userId)
        val issued = counterService.advance(resolved.uid, 1)
        val definition = issued.definition
        entityChangePublisher.updated(Constants.ENTITY_TYPE_SEQUENCE, definition.uid)
        return SequenceNumberResponse(
            definitionUid = definition.uid,
            entityType = definition.entityType,
            value = issued.first,
            formatted = SequenceFormatter.format(
                definition.prefix, definition.suffix, definition.paddingLength, issued.first
            ),
        )
    }

    /**
     * Applies a request onto an entity with shared validation.
     *
     * Counter handling: a requested current_value may only fast-forward the counter.
     * With [strictCounter] (direct create/update) a regression is rejected (FR-012);
     * without it (sync bulk upsert) the server value silently wins (research R3).
     */
    private fun applyRequest(
        definition: SequenceDefinition,
        request: SequenceDefinitionRequest,
        strictCounter: Boolean,
    ): SequenceDefinition {
        if (request.scope == SequenceScope.USER && request.userId.isNullOrBlank()) {
            throw InvalidSequenceRequestException("user_id is required for USER-scoped sequence definitions")
        }
        if (request.paddingLength > Constants.MAX_PADDING_LENGTH) {
            throw InvalidSequenceRequestException("padding_length must be <= ${Constants.MAX_PADDING_LENGTH}")
        }
        if (request.active) {
            assertNoDuplicateActive(request, definition.uid)
        }

        // Preserve the client-generated UID on offline-first creates (UID-keyed sync contract)
        request.uid?.takeIf { it.isNotBlank() }?.let { definition.uid = it }
        definition.entityType = request.entityType
        definition.scope = request.scope
        definition.userId = request.userId?.takeIf { it.isNotBlank() }
        definition.prefix = request.prefix?.takeIf { it.isNotBlank() }
        definition.suffix = request.suffix?.takeIf { it.isNotBlank() }
        definition.paddingLength = request.paddingLength
        definition.startValue = request.startValue
        definition.incrementStep = request.incrementStep
        definition.active = request.active
        definition.refId = request.refId

        val requestedCounter = request.currentValue
        if (requestedCounter != null) {
            if (requestedCounter >= definition.currentValue) {
                definition.currentValue = requestedCounter
            } else if (strictCounter) {
                throw InvalidSequenceRequestException(
                    "current_value cannot be lowered (server: ${definition.currentValue}, requested: $requestedCounter)"
                )
            }
            // sync upsert: server counter is authoritative — regression silently ignored
        }
        return definition
    }

    private fun assertNoDuplicateActive(request: SequenceDefinitionRequest, selfUid: String) {
        val duplicate = if (request.scope == SequenceScope.USER) {
            definitionRepository.existsByEntityTypeAndScopeAndUserIdAndActiveTrueAndUidNot(
                request.entityType, SequenceScope.USER, request.userId!!, selfUid
            )
        } else {
            definitionRepository.existsByEntityTypeAndScopeAndActiveTrueAndUidNot(
                request.entityType, SequenceScope.WORKSPACE, selfUid
            )
        }
        if (duplicate) {
            throw DuplicateSequenceDefinitionException(
                "An active ${request.scope} sequence definition already exists for entity_type '${request.entityType}'"
            )
        }
    }
}

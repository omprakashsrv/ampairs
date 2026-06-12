package com.ampairs.sequence.service

import com.ampairs.sequence.domain.model.SequenceDefinition
import com.ampairs.sequence.domain.model.SequenceScope
import com.ampairs.sequence.exception.InactiveSequenceDefinitionException
import com.ampairs.sequence.exception.SequenceDefinitionNotFoundException
import com.ampairs.sequence.repository.SequenceDefinitionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Internal counter engine shared by direct generation and block allocation.
 *
 * Resolution order (FR-003): active USER-scope definition for the caller →
 * active WORKSPACE-scope definition → auto-provisioned workspace default.
 *
 * Advancement always happens under a pessimistic row lock on the definition
 * (research R1) so concurrent callers each receive disjoint values/ranges.
 */
@Service
class SequenceCounterService(
    private val definitionRepository: SequenceDefinitionRepository,
) {

    private val logger = LoggerFactory.getLogger(SequenceCounterService::class.java)

    @Transactional
    fun resolveOrProvision(entityType: String, userId: String?): SequenceDefinition {
        val userScoped = userId?.takeIf { it.isNotBlank() }?.let {
            definitionRepository.findFirstByEntityTypeAndScopeAndUserIdAndActiveTrue(
                entityType, SequenceScope.USER, it
            )
        }
        if (userScoped != null) return userScoped

        val workspaceScoped = definitionRepository.findFirstByEntityTypeAndScopeAndActiveTrue(
            entityType, SequenceScope.WORKSPACE
        )
        if (workspaceScoped != null) return workspaceScoped

        logger.info("Auto-provisioning default sequence definition for entity_type={}", entityType)
        return definitionRepository.save(SequenceDefinition().apply {
            this.entityType = entityType
            scope = SequenceScope.WORKSPACE
            prefix = SequenceFormatter.defaultPrefix(entityType)
            paddingLength = 0
            startValue = 1
            incrementStep = 1
            currentValue = 0
            active = true
        })
    }

    /**
     * Atomically advances the counter by [count] steps under a row lock and returns the
     * issued value range (first..last, stepping by incrementStep). Refuses inactive definitions.
     */
    @Transactional
    fun advance(definitionUid: String, count: Int): IssuedRange {
        require(count >= 1) { "count must be >= 1" }
        val definition = definitionRepository.findByUidForUpdate(definitionUid)
            ?: throw SequenceDefinitionNotFoundException("Sequence definition not found for uid: $definitionUid")
        if (!definition.active) {
            throw InactiveSequenceDefinitionException(
                "Sequence definition $definitionUid for '${definition.entityType}' is inactive"
            )
        }
        val first = SequenceFormatter.nextValue(definition.currentValue, definition.startValue, definition.incrementStep)
        val last = first + (count - 1).toLong() * definition.incrementStep
        definition.currentValue = last
        definitionRepository.save(definition)
        return IssuedRange(definition, first, last)
    }
}

data class IssuedRange(
    val definition: SequenceDefinition,
    val first: Long,
    val last: Long,
)

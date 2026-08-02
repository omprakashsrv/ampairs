package com.ampairs.connector.service

import com.ampairs.connector.domain.dto.FieldMappingDto
import com.ampairs.connector.domain.dto.FieldMappingRuleDto
import com.ampairs.connector.domain.model.ConnectorFieldMapping
import com.ampairs.connector.config.ConnectorJson
import com.ampairs.connector.exception.ConnectorErrors
import com.ampairs.connector.repository.ConnectorFieldMappingRepository
import com.fasterxml.jackson.core.type.TypeReference
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Per-installation, per-entity field mappings. The set of mapped `ampairsField`s is the allowlist the
 * sparse-upsert (US3) intersects with each row's present columns.
 *
 * NOTE: full target-schema validation (FR-013 — ampairsField must exist on the entity and be
 * type-compatible) is reduced here to a non-blank check; deep schema validation is a follow-up that
 * needs the per-entity field catalogue. Invalid (blank-target, non-unmapped) rules are rejected.
 */
@Service
class ConnectorMappingService(
    private val mappingRepository: ConnectorFieldMappingRepository,
    private val installationService: ConnectorInstallationService,
) {
    private val objectMapper = ConnectorJson.mapper
    private val ruleListType = object : TypeReference<List<FieldMappingRuleDto>>() {}

    fun list(installationUid: String): List<FieldMappingDto> {
        installationService.getInstallation(installationUid)
        return mappingRepository.findByInstallationUidAndActiveTrue(installationUid).map { it.toDto() }
    }

    @Transactional
    fun upsert(installationUid: String, entityType: String, rules: List<FieldMappingRuleDto>): FieldMappingDto {
        installationService.getInstallation(installationUid)
        rules.filterNot { it.unmapped }.forEach {
            if (it.ampairsField.isBlank()) {
                throw ConnectorErrors.invalidMapping("Mapping rule for external field '${it.externalField}' has a blank Ampairs target")
            }
        }
        val mapping = mappingRepository.findByInstallationUidAndEntityType(installationUid, entityType)
            ?: ConnectorFieldMapping().apply {
                this.installationUid = installationUid
                this.entityType = entityType
                this.version = 0
            }
        mapping.rules = objectMapper.writeValueAsString(rules)
        mapping.version += 1
        mapping.active = true
        val saved = mappingRepository.save(mapping)
        installationService.markEnabledIfReady(installationUid)
        return saved.toDto()
    }

    /** The writable allowlist for a connector/entity: mapped (non-unmapped) ampairsField names. */
    fun allowlist(installationUid: String, entityType: String): Set<String> {
        val mapping = mappingRepository.findByInstallationUidAndEntityType(installationUid, entityType)
            ?.takeIf { it.active }   // a soft-deleted mapping must not yield a live allowlist (matches list())
            ?: return emptySet()
        return mapping.parseRules().filterNot { it.unmapped }.map { it.ampairsField }.toSet()
    }

    private fun ConnectorFieldMapping.parseRules(): List<FieldMappingRuleDto> =
        rules?.takeIf { it.isNotBlank() }?.let { objectMapper.readValue(it, ruleListType) } ?: emptyList()

    private fun ConnectorFieldMapping.toDto(): FieldMappingDto = FieldMappingDto(
        installationUid = installationUid,
        entityType = entityType,
        version = version,
        rules = parseRules(),
    )
}

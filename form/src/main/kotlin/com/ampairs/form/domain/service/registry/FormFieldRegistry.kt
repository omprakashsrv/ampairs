package com.ampairs.form.domain.service.registry

import com.ampairs.form.domain.model.EntityType
import org.springframework.stereotype.Component

/** The default section every entity has — the fallback home so no field is ever orphaned. */
const val GENERAL_SECTION = "General"

/**
 * Aggregates all [StandardFieldProvider]s (one per domain) into the single authoritative source of
 * built-in fields/sections per entity type. Used to seed defaults, merge new fields, and validate
 * STANDARD field keys. A domain with no provider yields just the default `General` section.
 */
@Component
class FormFieldRegistry(providers: List<StandardFieldProvider>) {

    private val byType: Map<EntityType, StandardFieldProvider> =
        providers.associateBy { it.entityType() }

    fun sectionsFor(entityType: EntityType): List<StandardSectionSpec> {
        val declared = byType[entityType]?.sections().orEmpty()
        return if (declared.any { it.name == GENERAL_SECTION }) declared
        else declared + StandardSectionSpec(name = GENERAL_SECTION, order = declared.size)
    }

    fun standardFieldsFor(entityType: EntityType): List<StandardFieldSpec> =
        byType[entityType]?.standardFields().orEmpty()

    fun isKnownStandardField(entityType: EntityType, fieldKey: String): Boolean =
        standardFieldsFor(entityType).any { it.key == fieldKey }

    fun essentialKeys(entityType: EntityType): Set<String> =
        standardFieldsFor(entityType).filter { it.essential }.map { it.key }.toSet()
}

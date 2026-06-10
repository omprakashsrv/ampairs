package com.ampairs.form.domain.service.registry

import com.ampairs.form.domain.model.EntityType
import com.ampairs.form.domain.model.FieldDataType
import com.ampairs.form.domain.model.OptionSource
import com.ampairs.form.domain.model.validation.ValidationRule

/**
 * SPI implemented by each domain module to declare its built-in (STANDARD) fields and sections —
 * the single source of truth (FR-020/021). The form module's [FormFieldRegistry] aggregates all
 * providers to seed defaults, merge new fields non-destructively, and validate STANDARD field keys.
 *
 * Cross-module by design: a domain module depends on `form` to implement this, and contributes a
 * `@Component` — no direct repository coupling.
 */
interface StandardFieldProvider {

    fun entityType(): EntityType

    /** Sections this entity offers, in order. Must include the default "General" home if used. */
    fun sections(): List<StandardSectionSpec>

    fun standardFields(): List<StandardFieldSpec>
}

/** A built-in section the domain offers. */
data class StandardSectionSpec(
    val name: String,
    val order: Int = 0,
    val visible: Boolean = true,
)

/** A built-in field the domain offers. [section] is the owning section name (must exist in [StandardFieldProvider.sections]). */
data class StandardFieldSpec(
    val key: String,
    val label: String,
    val dataType: FieldDataType,
    val section: String,
    val order: Int = 0,
    /** Structurally essential fields cannot be hidden or removed (FR-015). */
    val essential: Boolean = false,
    val mandatory: Boolean = false,
    val defaultValue: String? = null,
    val optionSource: OptionSource? = null,
    val enumValues: List<String>? = null,
    val dynamicSourceKey: String? = null,
    val validationRules: List<ValidationRule> = emptyList(),
    val placeholder: String? = null,
    val helpText: String? = null,
)

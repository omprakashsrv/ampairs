package com.ampairs.form.domain.service

import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.form.domain.dto.FormFieldDto
import com.ampairs.form.domain.dto.FormSchemaRequest
import com.ampairs.form.domain.dto.FormSchemaResponse
import com.ampairs.form.domain.dto.FormSectionDto
import com.ampairs.form.domain.model.EntityType
import com.ampairs.form.domain.model.FieldDataType
import com.ampairs.form.domain.model.FieldSource
import com.ampairs.form.domain.model.FormField
import com.ampairs.form.domain.model.FormSchema
import com.ampairs.form.domain.model.FormSection
import com.ampairs.form.domain.model.OptionSource
import com.ampairs.form.domain.model.validation.ValidationRule
import com.ampairs.form.domain.repository.FormFieldRepository
import com.ampairs.form.domain.repository.FormSchemaRepository
import com.ampairs.form.domain.repository.FormSectionRepository
import com.ampairs.form.domain.service.registry.FormFieldRegistry
import com.ampairs.form.domain.service.registry.GENERAL_SECTION
import com.ampairs.form.domain.service.registry.StandardFieldSpec
import com.ampairs.form.domain.service.validation.ValidationEngine
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

/**
 * Owns the FormSchema aggregate: seed-on-read defaults from the registry, the aggregate `/sync`
 * feed, and atomic replace-aggregate push with optimistic concurrency + invariant validation.
 * `@TenantId` filtering scopes every query to the current workspace.
 */
@Service
class FormConfigService(
    private val schemaRepository: FormSchemaRepository,
    private val sectionRepository: FormSectionRepository,
    private val fieldRepository: FormFieldRepository,
    private val registry: FormFieldRegistry,
    private val validationEngine: ValidationEngine,
) {

    // ---- Read (UI) -------------------------------------------------------------------------------

    /** Read aggregate for the UI; seeds defaults + merges new registry fields on first access. */
    @Transactional
    fun getConfigSchema(entityTypeValue: String): FormSchemaResponse {
        val entityType = EntityType.fromValue(entityTypeValue)
        val schema = seedAndMerge(entityType)
        return assembleResponse(schema)
    }

    // ---- Sync feed -------------------------------------------------------------------------------

    /**
     * Incremental pull: aggregates updated at/after [lastSync] (full feed when null/blank).
     *
     * On a **full** pull (no `last_sync` — the client bootstrap) we seed registry defaults for every
     * provider-backed entity type in the current workspace first, so a fresh workspace gets the
     * standard fields instead of an empty feed. Seeding is tenant-scoped (`@TenantId`) and idempotent
     * (`seedAndMerge` only adds what's missing), so it is a no-op on subsequent full syncs.
     */
    @Transactional
    fun getAfterSync(lastSync: String?, page: Int, size: Int): PageResponse<FormSchemaResponse> {
        val parsed = parseLastSync(lastSync)
        if (parsed == null) {
            EntityType.entries
                .filter { registry.standardFieldsFor(it).isNotEmpty() }
                .forEach { seedAndMerge(it) }
        }
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "updatedAt", "uid"))
        val result: Page<FormSchema> =
            if (parsed == null) schemaRepository.findAllForSync(pageable)
            else schemaRepository.findUpdatedAfter(parsed, pageable)
        return PageResponse.from(result) { assembleResponse(it) }
    }

    /** Push: replace each aggregate (upsert present members, delete absent) with optimistic version. */
    @Transactional
    fun replaceAggregates(requests: List<FormSchemaRequest>): List<FormSchemaResponse> =
        requests.map { replaceAggregate(it) }

    private fun replaceAggregate(request: FormSchemaRequest): FormSchemaResponse {
        val entityType = EntityType.fromValue(request.entityType)
        val schema = schemaRepository.findByEntityType(entityType.value)
            ?: FormSchema().apply { this.entityType = entityType.value; uid = entityType.value; version = 0 }

        if (request.baseVersion != null && schema.id != 0L && request.baseVersion < schema.version) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "form schema for '${entityType.value}' changed (base_version=${request.baseVersion}, current=${schema.version}); re-pull and re-apply",
            )
        }

        validateAggregate(entityType, request)

        // --- sections: upsert present, delete absent ---
        val existingSections = sectionRepository.findByEntityTypeOrderByDisplayOrderAsc(entityType.value)
        val keptSectionUids = mutableSetOf<String>()
        request.sections.forEach { dto ->
            val entity = dto.uid?.let { uid -> existingSections.firstOrNull { it.uid == uid } }
                ?: FormSection().apply { dto.uid?.let { uid = it } }
            entity.entityType = entityType.value
            entity.name = dto.name
            entity.displayOrder = dto.displayOrder ?: 0
            entity.visible = dto.visible ?: true
            val saved = sectionRepository.save(entity)
            keptSectionUids += saved.uid
        }
        existingSections.filter { it.uid !in keptSectionUids }
            .map { it.uid }
            .takeIf { it.isNotEmpty() }
            ?.let { sectionRepository.deleteByUidIn(it) }

        // --- fields: upsert present, delete absent ---
        val existingFields = fieldRepository.findByEntityTypeOrderByDisplayOrderAsc(entityType.value)
        val keptFieldUids = mutableSetOf<String>()
        request.fields.forEach { dto ->
            val entity = dto.uid?.let { uid -> existingFields.firstOrNull { it.uid == uid } }
                ?: FormField().apply { dto.uid?.let { uid = it } }
            applyFieldDto(entity, entityType, dto)
            val saved = fieldRepository.save(entity)
            keptFieldUids += saved.uid
        }
        existingFields.filter { it.uid !in keptFieldUids }
            .map { it.uid }
            .takeIf { it.isNotEmpty() }
            ?.let { fieldRepository.deleteByUidIn(it) }

        schema.version += 1
        val savedSchema = schemaRepository.save(schema)
        return assembleResponse(savedSchema)
    }

    // ---- Seeding / merge -------------------------------------------------------------------------

    private fun seedAndMerge(entityType: EntityType): FormSchema {
        val schema = schemaRepository.findByEntityType(entityType.value)
            ?: schemaRepository.save(
                FormSchema().apply { this.entityType = entityType.value; uid = entityType.value; version = 0 }
            )

        // Sections: add any registry section missing by name.
        val existingSections = sectionRepository.findByEntityTypeOrderByDisplayOrderAsc(entityType.value)
        val sectionByName = existingSections.associateBy { it.name }.toMutableMap()
        registry.sectionsFor(entityType).forEach { spec ->
            if (spec.name !in sectionByName) {
                val saved = sectionRepository.save(
                    FormSection().apply {
                        this.entityType = entityType.value
                        name = spec.name
                        displayOrder = spec.order
                        visible = spec.visible
                    }
                )
                sectionByName[saved.name] = saved
            }
        }
        val generalUid = (sectionByName[GENERAL_SECTION] ?: sectionByName.values.first()).uid

        // Fields: add any registry standard field missing by key (non-destructive).
        val existingKeys = fieldRepository.findByEntityTypeOrderByDisplayOrderAsc(entityType.value)
            .filter { it.source == FieldSource.STANDARD.value }
            .map { it.fieldKey }
            .toSet()
        registry.standardFieldsFor(entityType)
            .filter { it.key !in existingKeys }
            .forEach { spec ->
                fieldRepository.save(buildStandardField(entityType, spec, sectionByName[spec.section]?.uid ?: generalUid))
            }
        return schema
    }

    private fun buildStandardField(entityType: EntityType, spec: StandardFieldSpec, sectionUid: String) =
        FormField().apply {
            this.entityType = entityType.value
            source = FieldSource.STANDARD.value
            fieldKey = spec.key
            displayName = spec.label
            dataType = spec.dataType.value
            this.sectionUid = sectionUid
            displayOrder = spec.order
            mandatory = spec.mandatory
            defaultValue = spec.defaultValue
            optionSource = spec.optionSource?.value
            enumValues = spec.enumValues
            dynamicSourceKey = spec.dynamicSourceKey
            validationRules = spec.validationRules.takeIf { it.isNotEmpty() }
            placeholder = spec.placeholder
            helpText = spec.helpText
        }

    // ---- Validation ------------------------------------------------------------------------------

    private fun validateAggregate(entityType: EntityType, request: FormSchemaRequest) {
        require(request.sections.isNotEmpty()) { "A form schema must have at least one section" }
        val sectionUids = request.sections.mapNotNull { it.uid }.toSet()
        val essentialKeys = registry.essentialKeys(entityType)
        val pushedStandardKeys = request.fields.filter { it.source == FieldSource.STANDARD.value }.map { it.fieldKey }.toSet()

        // Essential standard fields must be present and visible.
        essentialKeys.forEach { key ->
            val field = request.fields.firstOrNull { it.fieldKey == key && it.source == FieldSource.STANDARD.value }
                ?: throw badRequest("Essential standard field '$key' for '${entityType.value}' cannot be removed")
            if (field.visible == false) throw badRequest("Essential standard field '$key' cannot be hidden")
        }

        request.fields.forEach { dto ->
            // Every field belongs to an existing section in this aggregate.
            if (dto.uid == null && dto.sectionUid !in sectionUids && request.sections.none { it.uid == dto.sectionUid }) {
                throw badRequest("Field '${dto.fieldKey}' references unknown section '${dto.sectionUid}'")
            }
            val source = FieldSource.fromValue(dto.source ?: FieldSource.CUSTOM.value)
            val dataType = FieldDataType.fromValue(dto.dataType ?: FieldDataType.TEXT.value)
            if (source == FieldSource.STANDARD && !registry.isKnownStandardField(entityType, dto.fieldKey)) {
                throw badRequest("Unknown standard field '${dto.fieldKey}' for '${entityType.value}'")
            }
            if (dataType.isChoice) {
                val os = dto.optionSource?.let { OptionSource.fromValue(it) }
                    ?: throw badRequest("Choice field '${dto.fieldKey}' needs an option_source")
                when (os) {
                    OptionSource.STATIC -> if (dto.enumValues.isNullOrEmpty())
                        throw badRequest("Static choice field '${dto.fieldKey}' needs enum_values")
                    OptionSource.DYNAMIC -> if (dto.dynamicSourceKey.isNullOrBlank())
                        throw badRequest("Dynamic choice field '${dto.fieldKey}' needs a dynamic_source_key")
                }
            }
            if (dataType == FieldDataType.CUSTOM && dto.widgetKey.isNullOrBlank()) {
                throw badRequest("Custom field '${dto.fieldKey}' needs a widget_key")
            }
            dto.validationRules?.let { validationEngine.assertConsistent(dto.fieldKey, it) }
        }
        // standard-keys that the registry knows are not silently fabricated beyond the catalog is covered above.
        pushedStandardKeys // referenced to keep intent explicit
    }

    private fun applyFieldDto(entity: FormField, entityType: EntityType, dto: FormFieldDto) {
        entity.entityType = entityType.value
        entity.source = dto.source ?: FieldSource.CUSTOM.value
        entity.fieldKey = dto.fieldKey
        entity.displayName = dto.displayName
        entity.dataType = dto.dataType ?: FieldDataType.TEXT.value
        entity.widgetKey = dto.widgetKey
        entity.sectionUid = dto.sectionUid
        entity.visible = dto.visible ?: true
        entity.mandatory = dto.mandatory ?: false
        entity.enabled = dto.enabled ?: true
        entity.displayOrder = dto.displayOrder ?: 0
        entity.defaultValue = dto.defaultValue
        entity.optionSource = dto.optionSource
        entity.enumValues = dto.enumValues
        entity.dynamicSourceKey = dto.dynamicSourceKey
        entity.validationRules = dto.validationRules
        entity.placeholder = dto.placeholder
        entity.helpText = dto.helpText
    }

    // ---- Assembly --------------------------------------------------------------------------------

    private fun assembleResponse(schema: FormSchema): FormSchemaResponse {
        val sections = sectionRepository.findByEntityTypeOrderByDisplayOrderAsc(schema.entityType)
        val fields = fieldRepository.findByEntityTypeOrderByDisplayOrderAsc(schema.entityType)
        val lastUpdated = (sections.mapNotNull { it.updatedAt } + fields.mapNotNull { it.updatedAt } + listOfNotNull(schema.updatedAt))
            .maxOrNull()
        return FormSchemaResponse(
            uid = schema.uid,
            entityType = schema.entityType,
            version = schema.version,
            sections = sections.map { FormSectionDto(it.uid, it.name, it.displayOrder, it.visible) },
            fields = fields.map { it.toDto() },
            lastUpdated = lastUpdated?.toString(),
        )
    }

    private fun FormField.toDto() = FormFieldDto(
        uid = uid,
        source = source,
        fieldKey = fieldKey,
        displayName = displayName,
        dataType = dataType,
        widgetKey = widgetKey,
        sectionUid = sectionUid,
        visible = visible,
        mandatory = mandatory,
        enabled = enabled,
        displayOrder = displayOrder,
        defaultValue = defaultValue,
        optionSource = optionSource,
        enumValues = enumValues,
        dynamicSourceKey = dynamicSourceKey,
        validationRules = validationRules,
        placeholder = placeholder,
        helpText = helpText,
    )

    private fun parseLastSync(lastSync: String?): Instant? {
        if (lastSync.isNullOrBlank()) return null
        return runCatching { Instant.parse(URLDecoder.decode(lastSync, StandardCharsets.UTF_8)) }.getOrNull()
    }

    private fun badRequest(message: String) = ResponseStatusException(HttpStatus.BAD_REQUEST, message)
}

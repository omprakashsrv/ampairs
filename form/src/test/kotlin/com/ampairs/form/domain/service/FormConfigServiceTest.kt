package com.ampairs.form.domain.service

import com.ampairs.form.domain.dto.FormFieldDto
import com.ampairs.form.domain.dto.FormSchemaRequest
import com.ampairs.form.domain.dto.FormSectionDto
import com.ampairs.form.domain.model.EntityType
import com.ampairs.form.domain.model.FieldDataType
import com.ampairs.form.domain.model.FieldSource
import com.ampairs.form.domain.model.FormField
import com.ampairs.form.domain.model.FormSchema
import com.ampairs.form.domain.model.FormSection
import com.ampairs.form.domain.repository.FormFieldRepository
import com.ampairs.form.domain.repository.FormSchemaRepository
import com.ampairs.form.domain.repository.FormSectionRepository
import com.ampairs.form.domain.service.registry.FormFieldRegistry
import com.ampairs.form.domain.service.registry.StandardFieldProvider
import com.ampairs.form.domain.service.registry.StandardFieldSpec
import com.ampairs.form.domain.service.registry.StandardSectionSpec
import com.ampairs.form.domain.service.validation.ValidationEngine
import com.ampairs.form.domain.model.validation.ValidationRule
import com.ampairs.form.domain.model.validation.ValidationRuleType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * Unit tests for [FormConfigService] over in-memory mocked repositories:
 * - T018 seed-on-read: empty tables → full default schema from the registry; idempotent.
 * - T077 merge preservation: a customized field survives the arrival of a new registry field.
 * - T047 integrity rules: essential standard fields can't be removed/hidden; unknown standard keys,
 *   incomplete choice/custom configs, and contradictory validation rules are rejected.
 * - Optimistic concurrency: stale base_version → 409; matching base_version bumps the version.
 * - Delete-by-absence: members omitted from a pushed aggregate are deleted.
 */
class FormConfigServiceTest {

    // ---- in-memory state behind the mocks ----------------------------------------------------
    private val schemas = mutableListOf<FormSchema>()
    private val sections = mutableListOf<FormSection>()
    private val fields = mutableListOf<FormField>()
    private var uidCounter = 0

    // Mutable registry backing — T077 adds a new spec mid-test.
    private val customerFields = mutableListOf(
        StandardFieldSpec(
            key = "name", label = "Name", dataType = FieldDataType.TEXT, section = "Basic Info",
            order = 0, essential = true, mandatory = true,
        ),
        StandardFieldSpec(key = "email", label = "Email", dataType = FieldDataType.TEXT, section = "Contact", order = 0),
    )

    private val provider = object : StandardFieldProvider {
        override fun entityType() = EntityType.CUSTOMER
        override fun sections() = listOf(
            StandardSectionSpec(name = "Basic Info", order = 0),
            StandardSectionSpec(name = "Contact", order = 1),
        )
        override fun standardFields(): List<StandardFieldSpec> = customerFields
    }

    private lateinit var service: FormConfigService

    @BeforeEach
    fun setUp() {
        schemas.clear(); sections.clear(); fields.clear(); uidCounter = 0

        val schemaRepository = mock<FormSchemaRepository> {
            on { findByEntityType(any()) } doAnswer { inv ->
                schemas.firstOrNull { it.entityType == inv.getArgument<String>(0) }
            }
            on { save(any<FormSchema>()) } doAnswer { inv ->
                val e = inv.getArgument<FormSchema>(0)
                if (e.uid.isBlank()) e.uid = e.entityType
                if (e.id == 0L) e.id = (schemas.size + 1).toLong()
                if (e !in schemas) schemas += e
                e
            }
        }
        val sectionRepository = mock<FormSectionRepository> {
            on { findByEntityTypeOrderByDisplayOrderAsc(any()) } doAnswer { inv ->
                sections.filter { it.entityType == inv.getArgument<String>(0) }.sortedBy { it.displayOrder }
            }
            on { save(any<FormSection>()) } doAnswer { inv ->
                val e = inv.getArgument<FormSection>(0)
                if (e.uid.isBlank()) e.uid = "SEC${++uidCounter}"
                if (e !in sections) sections += e
                e
            }
            on { deleteByUidIn(any()) } doAnswer { inv ->
                val uids = inv.getArgument<Collection<String>>(0)
                sections.removeAll { it.uid in uids }
                Unit
            }
        }
        val fieldRepository = mock<FormFieldRepository> {
            on { findByEntityTypeOrderByDisplayOrderAsc(any()) } doAnswer { inv ->
                fields.filter { it.entityType == inv.getArgument<String>(0) }.sortedBy { it.displayOrder }
            }
            on { save(any<FormField>()) } doAnswer { inv ->
                val e = inv.getArgument<FormField>(0)
                if (e.uid.isBlank()) e.uid = "FLD${++uidCounter}"
                if (e !in fields) fields += e
                e
            }
            on { deleteByUidIn(any()) } doAnswer { inv ->
                val uids = inv.getArgument<Collection<String>>(0)
                fields.removeAll { it.uid in uids }
                Unit
            }
        }

        service = FormConfigService(
            schemaRepository = schemaRepository,
            sectionRepository = sectionRepository,
            fieldRepository = fieldRepository,
            registry = FormFieldRegistry(listOf(provider)),
            validationEngine = ValidationEngine(),
            entityChangePublisher = mock(),
        )
    }

    // ---- T018: seed-on-read --------------------------------------------------------------------

    @Test
    fun `empty tables seed the complete default schema from the registry`() {
        val response = service.getConfigSchema("customer")

        assertEquals("customer", response.entityType)
        assertEquals(setOf("Basic Info", "Contact", "General"), response.sections.map { it.name }.toSet())
        assertEquals(setOf("name", "email"), response.fields.map { it.fieldKey }.toSet())

        val nameField = response.fields.single { it.fieldKey == "name" }
        val basicUid = response.sections.single { it.name == "Basic Info" }.uid
        assertEquals(basicUid, nameField.sectionUid, "standard field lands in its declared section")
        assertEquals(true, nameField.mandatory)
        assertEquals(FieldSource.STANDARD.value, nameField.source)
    }

    @Test
    fun `seed-on-read is idempotent — a second call does not duplicate`() {
        service.getConfigSchema("customer")
        val second = service.getConfigSchema("customer")

        assertEquals(2, second.fields.size)
        assertEquals(3, second.sections.size)
        assertEquals(1, fields.count { it.fieldKey == "name" })
    }

    // ---- T077: merge preservation ----------------------------------------------------------------

    @Test
    fun `a new registry field is added without touching a customized existing field`() {
        service.getConfigSchema("customer")

        // Workspace customizes the email field: hidden + relabeled.
        val email = fields.single { it.fieldKey == "email" }
        email.visible = false
        email.displayName = "Work Email"

        // A later release introduces a new standard field.
        customerFields += StandardFieldSpec(
            key = "phone", label = "Phone", dataType = FieldDataType.TEXT, section = "Contact", order = 1,
        )

        val merged = service.getConfigSchema("customer")

        assertEquals(setOf("name", "email", "phone"), merged.fields.map { it.fieldKey }.toSet())
        val mergedEmail = merged.fields.single { it.fieldKey == "email" }
        assertEquals(false, mergedEmail.visible, "customized visibility preserved")
        assertEquals("Work Email", mergedEmail.displayName, "customized label preserved")
    }

    // ---- T047: aggregate integrity rules ---------------------------------------------------------

    private fun seededBaseRequest(baseVersion: Long? = null): FormSchemaRequest {
        val seeded = service.getConfigSchema("customer")
        return FormSchemaRequest(
            entityType = "customer",
            baseVersion = baseVersion,
            sections = seeded.sections,
            fields = seeded.fields,
        )
    }

    @Test
    fun `removing an essential standard field is rejected`() {
        val base = seededBaseRequest()
        val request = base.copy(fields = base.fields.filterNot { it.fieldKey == "name" })

        val ex = assertThrows(ResponseStatusException::class.java) { service.replaceAggregates(listOf(request)) }
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        assertTrue(ex.reason!!.contains("cannot be removed"))
    }

    @Test
    fun `hiding an essential standard field is rejected`() {
        val base = seededBaseRequest()
        val request = base.copy(
            fields = base.fields.map { if (it.fieldKey == "name") it.copy(visible = false) else it },
        )

        val ex = assertThrows(ResponseStatusException::class.java) { service.replaceAggregates(listOf(request)) }
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        assertTrue(ex.reason!!.contains("cannot be hidden"))
    }

    @Test
    fun `an unknown standard field key is rejected`() {
        val base = seededBaseRequest()
        val request = base.copy(
            fields = base.fields + FormFieldDto(
                source = FieldSource.STANDARD.value, fieldKey = "fabricated", displayName = "Nope",
                dataType = FieldDataType.TEXT.value, sectionUid = base.sections.first().uid!!,
            ),
        )

        val ex = assertThrows(ResponseStatusException::class.java) { service.replaceAggregates(listOf(request)) }
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        assertTrue(ex.reason!!.contains("Unknown standard field"))
    }

    @Test
    fun `choice and custom field configs must be complete`() {
        val base = seededBaseRequest()
        val sectionUid = base.sections.first().uid!!

        fun pushWith(field: FormFieldDto) =
            assertThrows(ResponseStatusException::class.java) {
                service.replaceAggregates(listOf(base.copy(fields = base.fields + field)))
            }

        // CHOICE without option_source
        assertTrue(pushWith(FormFieldDto(fieldKey = "c1", displayName = "C1", dataType = "choice", sectionUid = sectionUid))
            .reason!!.contains("needs an option_source"))
        // STATIC without enum_values
        assertTrue(pushWith(FormFieldDto(fieldKey = "c2", displayName = "C2", dataType = "choice", optionSource = "static", sectionUid = sectionUid))
            .reason!!.contains("needs enum_values"))
        // DYNAMIC without dynamic_source_key
        assertTrue(pushWith(FormFieldDto(fieldKey = "c3", displayName = "C3", dataType = "multi_choice", optionSource = "dynamic", sectionUid = sectionUid))
            .reason!!.contains("needs a dynamic_source_key"))
        // CUSTOM without widget_key
        assertTrue(pushWith(FormFieldDto(fieldKey = "c4", displayName = "C4", dataType = "custom", sectionUid = sectionUid))
            .reason!!.contains("needs a widget_key"))
    }

    @Test
    fun `contradictory validation rules are rejected`() {
        val base = seededBaseRequest()
        val request = base.copy(
            fields = base.fields + FormFieldDto(
                fieldKey = "notes", displayName = "Notes", dataType = "text",
                sectionUid = base.sections.first().uid!!,
                validationRules = listOf(ValidationRule(ValidationRuleType.LENGTH_RANGE, min = 10, max = 2)),
            ),
        )

        assertThrows(IllegalArgumentException::class.java) { service.replaceAggregates(listOf(request)) }
    }

    // ---- Optimistic concurrency ------------------------------------------------------------------

    @Test
    fun `a stale base_version is rejected with 409 and a fresh one bumps the version`() {
        service.getConfigSchema("customer")
        schemas.single().version = 5

        val stale = seededBaseRequest(baseVersion = 4)
        val ex = assertThrows(ResponseStatusException::class.java) { service.replaceAggregates(listOf(stale)) }
        assertEquals(HttpStatus.CONFLICT, ex.statusCode)

        val fresh = seededBaseRequest(baseVersion = 5)
        val result = service.replaceAggregates(listOf(fresh)).single()
        assertEquals(6L, result.version)
    }

    // ---- Delete-by-absence -----------------------------------------------------------------------

    @Test
    fun `a field omitted from the pushed aggregate is deleted`() {
        val base = seededBaseRequest()
        // Add a custom field via push, then push again without it.
        val withCustom = base.copy(
            fields = base.fields + FormFieldDto(
                uid = "FLD_CUSTOM", fieldKey = "notes", displayName = "Notes",
                dataType = "text", sectionUid = base.sections.first().uid!!,
            ),
        )
        val afterAdd = service.replaceAggregates(listOf(withCustom)).single()
        assertTrue(afterAdd.fields.any { it.fieldKey == "notes" })

        val withoutCustom = afterAdd.let {
            FormSchemaRequest(
                entityType = "customer",
                baseVersion = it.version,
                sections = it.sections,
                fields = it.fields.filterNot { f -> f.fieldKey == "notes" },
            )
        }
        val afterRemove = service.replaceAggregates(listOf(withoutCustom)).single()

        assertFalse(afterRemove.fields.any { it.fieldKey == "notes" }, "absent member is deleted server-side")
        assertFalse(fields.any { it.fieldKey == "notes" })
    }
}

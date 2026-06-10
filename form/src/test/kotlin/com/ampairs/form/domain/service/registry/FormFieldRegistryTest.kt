package com.ampairs.form.domain.service.registry

import com.ampairs.form.domain.model.EntityType
import com.ampairs.form.domain.model.FieldDataType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [FormFieldRegistry] — the authoritative aggregation of STANDARD field providers.
 * Uses a local fake provider so the test stays within the `form` module (no cross-module deps).
 */
class FormFieldRegistryTest {

    private class FakeCustomerProvider : StandardFieldProvider {
        override fun entityType() = EntityType.CUSTOMER
        override fun sections() = listOf(
            StandardSectionSpec(name = "Basic Info", order = 0),
            StandardSectionSpec(name = "Contact", order = 1),
        )
        override fun standardFields() = listOf(
            StandardFieldSpec(key = "name", label = "Name", dataType = FieldDataType.TEXT, section = "Basic Info", essential = true, mandatory = true),
            StandardFieldSpec(key = "email", label = "Email", dataType = FieldDataType.TEXT, section = "Contact"),
        )
    }

    private val registry = FormFieldRegistry(listOf(FakeCustomerProvider()))

    @Test
    fun `appends a General section when the provider does not declare one`() {
        val sections = registry.sectionsFor(EntityType.CUSTOMER)
        assertTrue(sections.any { it.name == GENERAL_SECTION }, "General fallback section must be appended")
        assertEquals("General", sections.last().name)
        assertEquals(2, sections.last().order)
    }

    @Test
    fun `an entity with no provider still gets a General section`() {
        val sections = registry.sectionsFor(EntityType.PRODUCT)
        assertEquals(1, sections.size)
        assertEquals(GENERAL_SECTION, sections.single().name)
        assertTrue(registry.standardFieldsFor(EntityType.PRODUCT).isEmpty())
    }

    @Test
    fun `essentialKeys returns only fields flagged essential`() {
        assertEquals(setOf("name"), registry.essentialKeys(EntityType.CUSTOMER))
    }

    @Test
    fun `isKnownStandardField distinguishes declared from undeclared keys`() {
        assertTrue(registry.isKnownStandardField(EntityType.CUSTOMER, "name"))
        assertTrue(registry.isKnownStandardField(EntityType.CUSTOMER, "email"))
        assertFalse(registry.isKnownStandardField(EntityType.CUSTOMER, "not_a_field"))
        assertFalse(registry.isKnownStandardField(EntityType.PRODUCT, "name"))
    }
}

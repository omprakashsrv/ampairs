package com.ampairs.customer.service

import com.ampairs.form.domain.model.EntityType
import com.ampairs.form.domain.model.FieldDataType
import com.ampairs.form.domain.model.OptionSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CustomerStandardFieldProviderTest {

    private val provider = CustomerStandardFieldProvider()

    @Test
    fun `entity type is CUSTOMER`() {
        assertEquals(EntityType.CUSTOMER, provider.entityType())
    }

    @Test
    fun `declares the four standard sections in order`() {
        val sections = provider.sections()
        assertEquals(listOf("Basic Info", "Contact", "Tax & Credit", "Address"), sections.map { it.name })
        assertEquals(listOf(0, 1, 2, 3), sections.map { it.order })
    }

    @Test
    fun `name field is essential mandatory text`() {
        val name = provider.standardFields().first { it.key == "name" }
        assertEquals(FieldDataType.TEXT, name.dataType)
        assertTrue(name.essential)
        assertTrue(name.mandatory)
        assertEquals("Basic Info", name.section)
    }

    @Test
    fun `customer type and group are dynamic choice fields`() {
        val fields = provider.standardFields().associateBy { it.key }
        val type = fields.getValue("customerType")
        val group = fields.getValue("customerGroup")
        assertEquals(FieldDataType.CHOICE, type.dataType)
        assertEquals(OptionSource.DYNAMIC, type.optionSource)
        assertEquals("customer_types", type.dynamicSourceKey)
        assertEquals(OptionSource.DYNAMIC, group.optionSource)
        assertEquals("customer_groups", group.dynamicSourceKey)
    }

    @Test
    fun `status is a static choice with ACTIVE default`() {
        val status = provider.standardFields().first { it.key == "status" }
        assertEquals(OptionSource.STATIC, status.optionSource)
        assertEquals(listOf("ACTIVE", "INACTIVE"), status.enumValues)
        assertEquals("ACTIVE", status.defaultValue)
    }

    @Test
    fun `country field defaults to India`() {
        val country = provider.standardFields().first { it.key == "country" }
        assertEquals("India", country.defaultValue)
        assertEquals("Address", country.section)
    }

    @Test
    fun `field keys are unique`() {
        val keys = provider.standardFields().map { it.key }
        assertEquals(keys.size, keys.toSet().size, "standard field keys must be unique")
    }

    @Test
    fun `every field references a declared section`() {
        val sectionNames = provider.sections().map { it.name }.toSet()
        provider.standardFields().forEach { field ->
            assertTrue(
                sectionNames.contains(field.section),
                "field ${field.key} references undeclared section ${field.section}"
            )
        }
    }

    @Test
    fun `gst and pan fields carry validation rules`() {
        val gst = provider.standardFields().first { it.key == "gstNumber" }
        val pan = provider.standardFields().first { it.key == "panNumber" }
        assertNotNull(gst.validationRules.firstOrNull())
        assertNotNull(pan.validationRules.firstOrNull())
    }
}

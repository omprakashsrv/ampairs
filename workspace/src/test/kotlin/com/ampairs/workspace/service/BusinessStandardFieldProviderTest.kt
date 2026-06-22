package com.ampairs.workspace.service

import com.ampairs.form.domain.model.EntityType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BusinessStandardFieldProviderTest {

    private val provider = BusinessStandardFieldProvider()

    @Test
    fun `entityType is BUSINESS`() {
        assertEquals(EntityType.BUSINESS, provider.entityType())
    }

    @Test
    fun `declares five sections`() {
        assertEquals(5, provider.sections().size)
        assertTrue(provider.sections().any { it.name == "Business Profile" })
    }

    @Test
    fun `name field is mandatory and essential`() {
        val nameField = provider.standardFields().first { it.key == "name" }
        assertTrue(nameField.mandatory)
        assertTrue(nameField.essential)
    }

    @Test
    fun `currency field declares static enum values`() {
        val currency = provider.standardFields().first { it.key == "currency" }
        assertTrue(currency.enumValues?.contains("INR") == true)
        assertEquals("INR", currency.defaultValue)
    }
}

package com.ampairs.communication.service.template

import com.ampairs.communication.domain.model.MessageTemplate
import com.ampairs.communication.domain.model.TemplateVariant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class PreviewServiceTest {

    private val templateService: TemplateService = mock()
    private val service = PreviewService(templateService, TemplateRenderer())

    private fun emailVariant() = TemplateVariant().apply {
        channel = "EMAIL"; locale = "en"; subject = "Invoice {{invoice_number}}"
        htmlBody = "<p>Hi {{customer_name}}</p>"
    }

    @Test
    fun `renders subject + html and reports missing placeholders`() {
        whenever(templateService.findByCode("INV")).thenReturn(
            MessageTemplate().apply { code = "INV"; defaultLocale = "en" } to listOf(emailVariant())
        )
        val r = service.preview("INV", "EMAIL", "en", mapOf("invoice_number" to "INV-1"))
        assertEquals("Invoice INV-1", r.subject)
        assertEquals("<p>Hi </p>", r.renderedHtml)
        assertTrue(r.missingVariables.contains("customer_name"))
        // plain text derived from html
        assertEquals("Hi", r.renderedText)
    }
}

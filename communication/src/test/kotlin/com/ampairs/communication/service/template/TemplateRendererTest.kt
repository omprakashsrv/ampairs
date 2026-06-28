package com.ampairs.communication.service.template

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TemplateRendererTest {

    private val renderer = TemplateRenderer()

    @Test
    fun `substitutes placeholders from the context`() {
        val result = renderer.render(
            "Hi {{customer_name}}, invoice {{invoice_number}} for {{total_amount}} is ready.",
            mapOf("customer_name" to "Asha", "invoice_number" to "INV-1", "total_amount" to "₹999"),
        )
        assertEquals("Hi Asha, invoice INV-1 for ₹999 is ready.", result.output)
        assertTrue(result.missingVariables.isEmpty())
    }

    @Test
    fun `tolerates whitespace inside tokens and reports missing variables`() {
        val result = renderer.render("Hello {{ name }} from {{ city }}", mapOf("name" to "Sam"))
        assertEquals("Hello Sam from ", result.output)
        assertEquals(listOf("city"), result.missingVariables)
    }

    @Test
    fun `renderRequired throws when a placeholder is unresolved`() {
        val ex = assertThrows(TemplateRenderException::class.java) {
            renderer.renderRequired("Hi {{name}} {{missing}}", mapOf("name" to "Sam"))
        }
        assertTrue(ex.message!!.contains("missing"))
    }

    @Test
    fun `derives plain text from html`() {
        val text = renderer.htmlToPlainText("<p>Hi <b>Asha</b></p><p>Your invoice &amp; receipt</p>")
        assertEquals("Hi Asha\n\nYour invoice & receipt", text)
    }

    @Test
    fun `empty template renders to empty without missing vars`() {
        val result = renderer.render(null, emptyMap())
        assertEquals("", result.output)
        assertTrue(result.missingVariables.isEmpty())
    }
}

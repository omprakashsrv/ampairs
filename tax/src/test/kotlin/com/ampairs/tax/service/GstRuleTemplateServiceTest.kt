package com.ampairs.tax.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Exemplar unit test for a pure-logic service (no Spring context, no DB).
 *
 * Pattern: instantiate the service directly, exercise each public method with
 * representative + boundary inputs, assert on the returned value. This is the
 * cheapest, fastest tier of test — prefer it for any class whose behaviour does
 * not depend on Spring wiring or persistence. Copy this shape for the other
 * calculation-heavy services in the tax/order/payment modules.
 */
class GstRuleTemplateServiceTest {

    private val service = GstRuleTemplateService()

    @Test
    fun `standard GST splits intra-state into equal CGST and SGST halves`() {
        val composition = service.generateStandardGstComposition(taxRate = 18.0)

        val intra = requireNotNull(composition["INTRA_STATE"]) { "INTRA_STATE scenario missing" }
        assertEquals(18.0, intra.totalRate)
        assertEquals(2, intra.components.size)

        val cgst = intra.components.single { it.name == "CGST" }
        val sgst = intra.components.single { it.name == "SGST" }
        assertEquals(9.0, cgst.rate)
        assertEquals(9.0, sgst.rate)
        // Halves must sum back to the headline rate — the core GST invariant.
        assertEquals(intra.totalRate, cgst.rate + sgst.rate)
        // Ordering is significant for rendering on the invoice.
        assertEquals(1, cgst.order)
        assertEquals(2, sgst.order)
    }

    @Test
    fun `standard GST charges full rate as a single IGST for inter-state`() {
        val composition = service.generateStandardGstComposition(taxRate = 18.0)

        val inter = requireNotNull(composition["INTER_STATE"]) { "INTER_STATE scenario missing" }
        assertEquals(18.0, inter.totalRate)
        assertEquals(1, inter.components.size)
        val igst = inter.components.single()
        assertEquals("IGST", igst.name)
        assertEquals(18.0, igst.rate)
        assertEquals(1, igst.order)
    }

    @Test
    fun `component ids encode the integer rate`() {
        val composition = service.generateStandardGstComposition(taxRate = 12.0)

        val intra = composition.getValue("INTRA_STATE")
        assertEquals("COMP_CGST_6", intra.components[0].id)
        assertEquals("COMP_SGST_6", intra.components[1].id)
        assertEquals("COMP_IGST_12", composition.getValue("INTER_STATE").components[0].id)
    }

    @Test
    fun `cess composition appends CESS to both scenarios and folds it into the total`() {
        val composition = service.generateGstWithCess(baseRate = 28.0, cessRate = 12.0)

        val intra = composition.getValue("INTRA_STATE")
        assertEquals(40.0, intra.totalRate) // 28 base + 12 cess
        assertEquals(listOf("CGST", "SGST", "CESS"), intra.components.map { it.name })
        assertEquals(14.0, intra.components.single { it.name == "CGST" }.rate)
        assertEquals(12.0, intra.components.single { it.name == "CESS" }.rate)

        val inter = composition.getValue("INTER_STATE")
        assertEquals(40.0, inter.totalRate)
        assertEquals(listOf("IGST", "CESS"), inter.components.map { it.name })
        assertEquals(28.0, inter.components.single { it.name == "IGST" }.rate)
    }

    @Test
    fun `zero rate is well-formed`() {
        val composition = service.generateStandardGstComposition(taxRate = 0.0)
        val intra = composition.getValue("INTRA_STATE")
        assertEquals(0.0, intra.totalRate)
        assertTrue(intra.components.all { it.rate == 0.0 })
    }

    @Test
    fun `isStandardGstRate recognises the canonical GST slabs and rejects others`() {
        listOf(0.0, 0.25, 3.0, 5.0, 12.0, 18.0, 28.0).forEach { rate ->
            assertTrue(service.isStandardGstRate(rate), "$rate should be a standard slab")
        }
        listOf(1.0, 6.0, 15.0, 20.0, 28.01).forEach { rate ->
            assertFalse(service.isStandardGstRate(rate), "$rate should not be a standard slab")
        }
    }
}

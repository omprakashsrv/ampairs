package com.ampairs.dms.domain

import com.ampairs.invoice.domain.enums.InvoiceStatus
import com.ampairs.invoice.domain.model.Invoice
import com.ampairs.invoice.domain.model.InvoiceItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class InvoiceSaleLineMapperTest {

    private fun invoice(status: InvoiceStatus, customer: String, vararg items: Triple<String, Double, Double>) =
        Invoice().apply {
            this.status = status
            invoiceDate = Instant.parse("2026-06-15T10:00:00Z")
            customerId = customer
            items.forEach { (productId, qty, total) ->
                invoiceItems.add(InvoiceItem().apply { this.productId = productId; quantity = qty; totalCost = total })
            }
        }

    @Test
    fun `only INVOICED invoices map, lines key on product and customer, period is the month`() {
        val lines = InvoiceSaleLineMapper.toSaleLines(
            "DIST",
            listOf(
                invoice(InvoiceStatus.INVOICED, "CUS-1", Triple("DPROD-1", 2.0, 200.0), Triple("DPROD-2", 1.0, 50.0)),
                invoice(InvoiceStatus.DRAFT, "CUS-9", Triple("DPROD-9", 5.0, 500.0)), // excluded (not finalized)
                invoice(InvoiceStatus.NEW, "CUS-8", Triple("DPROD-8", 3.0, 300.0)),   // excluded
            ),
        )
        assertEquals(2, lines.size) // two lines from the one INVOICED invoice
        val first = lines.first()
        assertEquals("DPROD-1", first.productUid)
        assertEquals("CUS-1", first.customerUid)
        assertEquals("DIST", first.distributorWorkspaceId)
        assertEquals("2026-06", first.periodKey)
        assertEquals(0, java.math.BigDecimal.valueOf(200.0).compareTo(first.value))
    }

    @Test
    fun `no INVOICED invoices yields no lines`() {
        val lines = InvoiceSaleLineMapper.toSaleLines("DIST", listOf(invoice(InvoiceStatus.DRAFT, "CUS-1", Triple("P", 1.0, 1.0))))
        assertEquals(0, lines.size)
    }
}

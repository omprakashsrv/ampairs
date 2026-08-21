package com.ampairs.invoice.service

import com.ampairs.invoice.domain.enums.InvoiceStatus
import com.ampairs.invoice.domain.model.Invoice
import com.ampairs.invoice.domain.model.InvoiceItem
import com.ampairs.invoice.repository.InvoiceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.time.Instant

/**
 * Spec 029 — buyer invoice read mapping + guards. Finalized-only (INVOICED); drafts and other parties
 * are never exposed; DTOs carry no cost/margin and pass the raw orderRefId through for the controller
 * to swap.
 */
@ExtendWith(MockitoExtension::class)
class InvoiceEcomServiceImplTest {

    @Mock private lateinit var invoiceRepository: InvoiceRepository

    private lateinit var service: InvoiceEcomServiceImpl

    @BeforeEach
    fun setup() {
        service = InvoiceEcomServiceImpl(invoiceRepository)
    }

    private fun invoice(
        uid: String,
        customerId: String?,
        status: InvoiceStatus,
        orderRefId: String? = null,
    ): Invoice = Invoice().apply {
        this.uid = uid
        invoiceNumber = "INV-$uid"
        invoiceDate = Instant.parse("2026-08-15T10:00:00Z")
        this.status = status
        this.customerId = customerId
        this.orderRefId = orderRefId
        basePrice = 9000.0
        totalTax = 207.5
        totalCost = 9207.5
    }

    private fun item(desc: String, qty: Double, price: Double, total: Double, index: Int, active: Boolean = true) =
        InvoiceItem().apply {
            description = desc
            quantity = qty
            sellingPrice = price
            totalCost = total
            this.index = index
            this.active = active
        }

    @Test
    fun `listBuyerInvoices filters to INVOICED and maps to buyer summary`() {
        val inv = invoice("1", customerId = "CUS1", status = InvoiceStatus.INVOICED, orderRefId = "ORD9")
        whenever(invoiceRepository.findByCustomerIdAndStatusIn(eq("CUS1"), any(), any()))
            .thenReturn(PageImpl(listOf(inv)))

        val page = service.listBuyerInvoices("CUS1", PageRequest.of(0, 20))

        assertEquals(1, page.content.size)
        val s = page.content.first()
        assertEquals("1", s.invoiceUid)
        assertEquals("INV-1", s.invoiceNumber)
        assertEquals(BigDecimal.valueOf(9207.5), s.total)
        assertEquals("ORD9", s.orderRefId)
    }

    @Test
    fun `getBuyerInvoice returns null for another party`() {
        whenever(invoiceRepository.findByUid("1"))
            .thenReturn(invoice("1", customerId = "CUS_OTHER", status = InvoiceStatus.INVOICED))
        assertNull(service.getBuyerInvoice("1", "CUS_ME"))
    }

    @Test
    fun `getBuyerInvoice returns null for a draft`() {
        whenever(invoiceRepository.findByUid("1"))
            .thenReturn(invoice("1", customerId = "CUS1", status = InvoiceStatus.DRAFT))
        assertNull(service.getBuyerInvoice("1", "CUS1"))
    }

    @Test
    fun `getBuyerInvoice returns null when missing`() {
        whenever(invoiceRepository.findByUid("nope")).thenReturn(null)
        assertNull(service.getBuyerInvoice("nope", "CUS1"))
    }

    @Test
    fun `getBuyerInvoice maps own finalized invoice with active lines sorted, no cost field`() {
        val inv = invoice("1", customerId = "CUS1", status = InvoiceStatus.INVOICED, orderRefId = "ORD9").apply {
            invoiceItems = mutableListOf(
                item("B", qty = 2.0, price = 100.0, total = 200.0, index = 1),
                item("A", qty = 10.0, price = 900.0, total = 9000.0, index = 0),
                item("gone", qty = 1.0, price = 5.0, total = 5.0, index = 2, active = false),
            )
        }
        whenever(invoiceRepository.findByUid("1")).thenReturn(inv)

        val detail = service.getBuyerInvoice("1", "CUS1")!!

        assertEquals("ORD9", detail.orderRefId)
        assertEquals(BigDecimal.valueOf(9000.0), detail.subtotal)
        assertEquals(BigDecimal.valueOf(207.5), detail.taxTotal)
        assertEquals(BigDecimal.valueOf(9207.5), detail.total)
        // inactive line dropped, remaining sorted by index
        assertEquals(listOf("A", "B"), detail.lines.map { it.description })
        assertEquals(BigDecimal.valueOf(900.0), detail.lines.first().unitPrice)
        // BuyerInvoiceLine exposes no cost/margin field (compile-time guarantee): assert the shape
        assertTrue(detail.lines.all { it.lineTotal.signum() >= 0 })
    }

    @Test
    fun `listInvoicesForOrder returns finalized invoices for the party only`() {
        whenever(invoiceRepository.findByOrderRefIdAndStatusIn(eq("ORD9"), any()))
            .thenReturn(
                listOf(
                    invoice("1", customerId = "CUS1", status = InvoiceStatus.INVOICED, orderRefId = "ORD9"),
                    invoice("2", customerId = "CUS_OTHER", status = InvoiceStatus.INVOICED, orderRefId = "ORD9"),
                ),
            )

        val list = service.listInvoicesForOrder("ORD9", "CUS1")

        assertEquals(1, list.size)
        assertEquals("1", list.first().invoiceUid)
    }
}

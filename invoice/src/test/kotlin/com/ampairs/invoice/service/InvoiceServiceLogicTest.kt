package com.ampairs.invoice.service

import com.ampairs.event.domain.events.InvoiceCancelledEvent
import com.ampairs.event.domain.events.InvoiceCreatedEvent
import com.ampairs.event.domain.events.InvoiceFinalizedEvent
import com.ampairs.event.domain.events.InvoiceStatusChangedEvent
import com.ampairs.event.domain.events.InvoiceUpdatedEvent
import com.ampairs.form.domain.model.EntityType
import com.ampairs.invoice.domain.dto.InvoiceItemRequest
import com.ampairs.invoice.domain.dto.InvoiceUpdateRequest
import com.ampairs.invoice.domain.dto.toInvoice
import com.ampairs.invoice.domain.dto.toInvoiceItems
import com.ampairs.invoice.domain.dto.toResponse
import com.ampairs.invoice.domain.enums.InvoiceStatus
import com.ampairs.invoice.domain.model.Invoice
import com.ampairs.invoice.domain.model.InvoiceItem
import com.ampairs.invoice.repository.InvoiceItemRepository
import com.ampairs.invoice.repository.InvoicePagingRepository
import com.ampairs.invoice.repository.InvoiceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Instant
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvoiceServiceLogicTest {

    @Mock private lateinit var invoiceRepository: InvoiceRepository
    @Mock private lateinit var invoiceItemRepository: InvoiceItemRepository
    @Mock private lateinit var invoicePagingRepository: InvoicePagingRepository
    @Mock private lateinit var eventPublisher: ApplicationEventPublisher

    private lateinit var service: InvoiceService

    @BeforeEach
    fun setUp() {
        service = InvoiceService(invoiceRepository, invoiceItemRepository, invoicePagingRepository, eventPublisher)
        whenever(invoiceRepository.save(any<Invoice>())).thenAnswer { it.arguments[0] }
        whenever(invoiceItemRepository.save(any<InvoiceItem>())).thenAnswer { it.arguments[0] }
        whenever(invoiceRepository.findMaxInvoiceNumber()).thenReturn(Optional.of("0"))
    }

    private fun item(block: InvoiceItem.() -> Unit = {}) = InvoiceItem().apply {
        uid = "II-1"
        description = "Widget"
        quantity = 2.0
        sellingPrice = 100.0
        totalCost = 236.0
        totalTax = 36.0
        block()
    }

    // ==================== updateInvoice ====================

    @Test
    fun `updateInvoice creates a new invoice, assigns next number and publishes created event`() {
        whenever(invoiceRepository.findByUid("INV-new")).thenReturn(null)
        whenever(invoiceRepository.findMaxInvoiceNumber()).thenReturn(Optional.of("41"))

        val invoice = Invoice().apply {
            uid = "INV-new"
            customerName = "Acme"
            totalCost = 500.0
            status = InvoiceStatus.DRAFT
        }

        val response = service.updateInvoice(invoice, listOf(item()))

        assertEquals("42", invoice.invoiceNumber)        // max 41 + 1
        assertEquals("INV-new", response.id)
        verify(eventPublisher).publishEvent(any<InvoiceCreatedEvent>())
        verify(eventPublisher, never()).publishEvent(any<InvoiceUpdatedEvent>())
    }

    @Test
    fun `updateInvoice defaults to 1 when no prior max number exists`() {
        whenever(invoiceRepository.findByUid("INV-new")).thenReturn(null)
        whenever(invoiceRepository.findMaxInvoiceNumber()).thenReturn(Optional.empty())

        val invoice = Invoice().apply { uid = "INV-new" }
        service.updateInvoice(invoice, emptyList())

        assertEquals("1", invoice.invoiceNumber)
    }

    @Test
    fun `updateInvoice on an existing invoice publishes updated event and keeps existing number`() {
        val existing = Invoice().apply { uid = "INV-1"; invoiceNumber = "7"; status = InvoiceStatus.DRAFT }
        whenever(invoiceRepository.findByUid("INV-1")).thenReturn(existing)

        val invoice = Invoice().apply { uid = "INV-1"; status = InvoiceStatus.DRAFT }
        service.updateInvoice(invoice, listOf(item { uid = "" }))

        assertEquals("7", invoice.invoiceNumber)
        verify(eventPublisher).publishEvent(any<InvoiceUpdatedEvent>())
        verify(eventPublisher, never()).publishEvent(any<InvoiceStatusChangedEvent>())
    }

    @Test
    fun `updateInvoice publishes a status-changed event when the status differs`() {
        val existing = Invoice().apply { uid = "INV-1"; invoiceNumber = "7"; status = InvoiceStatus.DRAFT }
        whenever(invoiceRepository.findByUid("INV-1")).thenReturn(existing)
        whenever(invoiceItemRepository.findByUid("II-1")).thenReturn(InvoiceItem().apply { uid = "II-1"; id = 99 })

        val invoice = Invoice().apply { uid = "INV-1"; status = InvoiceStatus.INVOICED }
        service.updateInvoice(invoice, listOf(item()))

        verify(eventPublisher).publishEvent(any<InvoiceStatusChangedEvent>())
    }

    // ==================== bulkUpsertInvoices (upsert path / finalization) ====================

    @Test
    fun `bulk upsert of a new INVOICED invoice publishes a finalized event and derives the number`() {
        whenever(invoiceRepository.findBySeriesAndSequenceNumber("INV", 5)).thenReturn(null)
        whenever(invoiceRepository.findByUid("INV-fin")).thenReturn(null)

        val request = InvoiceUpdateRequest(
            id = "INV-fin", series = "INV", sequenceNumber = 5, status = InvoiceStatus.INVOICED,
            customerId = "CUS-1", totalCost = 999.0,
        )

        val result = service.bulkUpsertInvoices(listOf(request))

        assertEquals(1, result.size)
        assertEquals("INV-5", result[0].invoiceNumber)
        verify(eventPublisher).publishEvent(any<InvoiceFinalizedEvent>())
    }

    @Test
    fun `bulk upsert that moves an INVOICED invoice back to DRAFT publishes a cancelled event`() {
        val existing = Invoice().apply { uid = "INV-1"; invoiceNumber = "INV-1"; series = "INV"; sequenceNumber = 1; status = InvoiceStatus.INVOICED }
        whenever(invoiceRepository.findBySeriesAndSequenceNumber("INV", 1)).thenReturn(existing)
        whenever(invoiceRepository.findByUid("INV-1")).thenReturn(existing)

        val request = InvoiceUpdateRequest(id = "INV-1", series = "INV", sequenceNumber = 1, status = InvoiceStatus.DRAFT)
        service.bulkUpsertInvoices(listOf(request))

        verify(eventPublisher).publishEvent(any<InvoiceCancelledEvent>())
    }

    @Test
    fun `bulk upsert preserves an existing invoice number when the request omits it`() {
        val existing = Invoice().apply { uid = "INV-1"; invoiceNumber = "INV-1"; series = "INV"; sequenceNumber = 1; status = InvoiceStatus.DRAFT; createdAt = Instant.parse("2025-01-01T00:00:00Z") }
        whenever(invoiceRepository.findBySeriesAndSequenceNumber("INV", 1)).thenReturn(existing)
        whenever(invoiceRepository.findByUid("INV-1")).thenReturn(existing)

        val request = InvoiceUpdateRequest(id = "INV-1", invoiceNumber = "", series = "INV", sequenceNumber = 1, status = InvoiceStatus.DRAFT)
        val result = service.bulkUpsertInvoices(listOf(request))

        assertEquals("INV-1", result[0].invoiceNumber)
        // no finalization transition (DRAFT -> DRAFT)
        verify(eventPublisher, never()).publishEvent(any<InvoiceFinalizedEvent>())
        verify(eventPublisher, never()).publishEvent(any<InvoiceCancelledEvent>())
    }

    // ==================== getInvoice / getInvoices / sync feed ====================

    @Test
    fun `getInvoice delegates to the repository`() {
        val inv = Invoice().apply { uid = "INV-1" }
        whenever(invoiceRepository.findByUid("INV-1")).thenReturn(inv)
        assertEquals("INV-1", service.getInvoice("INV-1")!!.uid)
    }

    @Test
    fun `getInvoices uses EPOCH when lastUpdated is null`() {
        whenever(invoicePagingRepository.findAllByUpdatedAtGreaterThanEqual(eq(Instant.EPOCH), any()))
            .thenReturn(emptyList())
        service.getInvoices(null)
        verify(invoicePagingRepository).findAllByUpdatedAtGreaterThanEqual(eq(Instant.EPOCH), any())
    }

    @Test
    fun `getInvoices passes through the supplied cursor`() {
        val cursor = Instant.parse("2025-03-01T00:00:00Z")
        whenever(invoicePagingRepository.findAllByUpdatedAtGreaterThanEqual(eq(cursor), any()))
            .thenReturn(listOf(Invoice().apply { uid = "INV-1" }))
        assertEquals(1, service.getInvoices(cursor).size)
    }

    @Test
    fun `getInvoicesAfterSync falls back to full feed on an unparseable cursor`() {
        val pageable = PageRequest.of(0, 100)
        whenever(invoicePagingRepository.findAllBy(pageable)).thenReturn(PageImpl(emptyList()))
        service.getInvoicesAfterSync("not-a-date", pageable)
        verify(invoicePagingRepository).findAllBy(pageable)
        verify(invoicePagingRepository, never()).findByUpdatedAtGreaterThanEqual(any(), any())
    }

    @Test
    fun `getInvoicesAfterSync decodes a URL-encoded cursor`() {
        val pageable = PageRequest.of(0, 100)
        whenever(invoicePagingRepository.findByUpdatedAtGreaterThanEqual(any(), eq(pageable)))
            .thenReturn(PageImpl(emptyList()))
        service.getInvoicesAfterSync("2025-02-01T00%3A00%3A00Z", pageable) // %3A == ':'
        verify(invoicePagingRepository).findByUpdatedAtGreaterThanEqual(eq(Instant.parse("2025-02-01T00:00:00Z")), eq(pageable))
    }

    // ==================== DTO mappers ====================

    @Test
    fun `request maps to invoice with series fallback and item mapping`() {
        val req = InvoiceUpdateRequest(
            id = "INV-1", invoiceNumber = "INV-1", series = "", sequenceNumber = 3,
            customerGst = "27AAAAA0000A1Z5", placeOfSupply = "MAHARASHTRA",
            totalCost = 236.0, totalTax = 36.0, basePrice = 200.0, priceMode = "TAX_INCLUSIVE",
            invoiceDate = Instant.parse("2025-04-01T00:00:00Z"),
            invoiceItems = listOf(InvoiceItemRequest(id = "II-1", description = "Widget", quantity = 2.0, price = 100.0, totalTax = 36.0)),
        )

        val invoice = req.toInvoice()
        assertEquals("INV", invoice.series) // blank fell back to INV
        assertEquals(3, invoice.sequenceNumber)
        assertEquals("TAX_INCLUSIVE", invoice.priceMode)
        assertEquals(Instant.parse("2025-04-01T00:00:00Z"), invoice.invoiceDate)

        val items = req.invoiceItems.toInvoiceItems()
        assertEquals(1, items.size)
        assertEquals("II-1", items[0].uid)
        assertEquals(100.0, items[0].sellingPrice)

        val response = invoice.toResponse(items)
        assertEquals("INV-1", response.id)
        assertEquals(236.0, response.totalCost)
        assertEquals(36.0, response.totalTax)
        assertEquals(1, response.invoiceItems.size)
    }

    // ==================== standard field provider ====================

    @Test
    fun `standard field provider declares invoice fields and sections`() {
        val provider = InvoiceStandardFieldProvider()
        assertEquals(EntityType.INVOICE, provider.entityType())
        assertEquals(2, provider.sections().size)
        val keys = provider.standardFields().map { it.key }
        assertTrue(keys.contains("series"))
        assertTrue(keys.contains("priceMode"))
        assertTrue(keys.contains("customerId"))
    }
}

package com.ampairs.invoice.service

import com.ampairs.inventory.service.InventoryStockService
import com.ampairs.invoice.domain.dto.InvoiceUpdateRequest
import com.ampairs.invoice.domain.model.Invoice
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
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Instant

/**
 * Unit tests for the offline-sync paths added in spec 010, including the client-numbering
 * collision rule (C5/FR-B09): the server never renumbers — colliding rows are skipped.
 */
@ExtendWith(MockitoExtension::class)
class InvoiceServiceSyncTest {

    @Mock private lateinit var invoiceRepository: InvoiceRepository
    @Mock private lateinit var invoiceItemRepository: InvoiceItemRepository
    @Mock private lateinit var invoicePagingRepository: InvoicePagingRepository
    @Mock private lateinit var inventoryStockService: InventoryStockService
    @Mock private lateinit var eventPublisher: ApplicationEventPublisher

    private lateinit var service: InvoiceService

    @BeforeEach
    fun setup() {
        service = InvoiceService(invoiceRepository, invoiceItemRepository, invoicePagingRepository, inventoryStockService, eventPublisher)
    }

    @Test
    fun `bulk upsert assigns invoice number from series and sequence when blank`() {
        whenever(invoiceRepository.findBySeriesAndSequenceNumber("INV", 7)).thenReturn(null)
        whenever(invoiceRepository.findByUid("INV-a")).thenReturn(null)
        whenever(invoiceRepository.save(any<Invoice>())).thenAnswer { it.getArgument(0) }

        val request = InvoiceUpdateRequest(id = "INV-a", invoiceNumber = "", series = "INV", sequenceNumber = 7)

        val result = service.bulkUpsertInvoices(listOf(request))

        assertEquals(1, result.size)
        assertEquals("INV-a", result[0].id)
        assertEquals("INV", result[0].series)
        assertEquals(7, result[0].sequenceNumber)
        assertEquals("INV-7", result[0].invoiceNumber)
    }

    @Test
    fun `bulk upsert skips a row whose series and sequence collide with a different invoice`() {
        val clash = Invoice().apply { uid = "INV-existing"; series = "INV"; sequenceNumber = 7 }
        whenever(invoiceRepository.findBySeriesAndSequenceNumber("INV", 7)).thenReturn(clash)

        val request = InvoiceUpdateRequest(id = "INV-new", series = "INV", sequenceNumber = 7)

        val result = service.bulkUpsertInvoices(listOf(request))

        assertTrue(result.isEmpty())                         // skipped — not returned
        verify(invoiceRepository, never()).save(any<Invoice>())  // and never persisted/renumbered
    }

    @Test
    fun `bulk upsert dedups duplicate series and sequence within a single batch`() {
        whenever(invoiceRepository.findBySeriesAndSequenceNumber("INV", 9)).thenReturn(null)
        whenever(invoiceRepository.findByUid(any())).thenReturn(null)
        whenever(invoiceRepository.save(any<Invoice>())).thenAnswer { it.getArgument(0) }

        val first = InvoiceUpdateRequest(id = "INV-x", series = "INV", sequenceNumber = 9)
        val dup = InvoiceUpdateRequest(id = "INV-y", series = "INV", sequenceNumber = 9)

        val result = service.bulkUpsertInvoices(listOf(first, dup))

        assertEquals(1, result.size)        // only the first is accepted
        assertEquals("INV-x", result[0].id)
    }

    @Test
    fun `sync feed uses full feed when cursor absent and cursor when valid`() {
        val pageable = PageRequest.of(0, 100)
        whenever(invoicePagingRepository.findAllBy(pageable)).thenReturn(PageImpl<Invoice>(emptyList()))
        service.getInvoicesAfterSync(null, pageable)
        verify(invoicePagingRepository).findAllBy(pageable)

        whenever(invoicePagingRepository.findByUpdatedAtGreaterThanEqual(any(), eq(pageable)))
            .thenReturn(PageImpl<Invoice>(emptyList()))
        service.getInvoicesAfterSync("2025-02-01T00:00:00Z", pageable)
        verify(invoicePagingRepository).findByUpdatedAtGreaterThanEqual(eq(Instant.parse("2025-02-01T00:00:00Z")), eq(pageable))
    }
}

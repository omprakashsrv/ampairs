package com.ampairs.subscription

import com.ampairs.subscription.domain.model.Invoice
import com.ampairs.subscription.domain.model.InvoiceStatus
import com.ampairs.subscription.domain.repository.SubscriptionInvoiceRepository
import com.ampairs.subscription.domain.service.SubscriptionInvoiceQueryService
import com.ampairs.subscription.exception.SubscriptionException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionInvoiceQueryServiceTest {

    @Mock
    private lateinit var invoiceRepository: SubscriptionInvoiceRepository

    private lateinit var service: SubscriptionInvoiceQueryService

    @BeforeEach
    fun setUp() {
        service = SubscriptionInvoiceQueryService(invoiceRepository)
    }

    private fun invoice(block: Invoice.() -> Unit = {}): Invoice = Invoice().apply {
        uid = "INV-1"
        workspaceId = "WS-1"
        invoiceNumber = "INV-2025-001"
        status = InvoiceStatus.PENDING
        totalAmount = BigDecimal("100.00")
        paidAmount = BigDecimal.ZERO
        dueDate = Instant.now().plus(10, ChronoUnit.DAYS)
        block()
    }

    @Test
    fun `getInvoicesForWorkspace filters by status and workspace`() {
        val mine = invoice { workspaceId = "WS-1"; status = InvoiceStatus.OVERDUE }
        val theirs = invoice { workspaceId = "WS-2"; status = InvoiceStatus.OVERDUE }
        whenever(invoiceRepository.findByStatus(InvoiceStatus.OVERDUE)).thenReturn(listOf(mine, theirs))

        val page = service.getInvoicesForWorkspace("WS-1", InvoiceStatus.OVERDUE, PageRequest.of(0, 10))

        assertEquals(1, page.content.size)
        assertEquals("WS-1", page.content.first().workspaceId)
    }

    @Test
    fun `getInvoicesForWorkspace without status delegates to repository`() {
        val pageable = PageRequest.of(0, 10)
        whenever(invoiceRepository.findByWorkspaceIdOrderByCreatedAtDesc("WS-1", pageable))
            .thenReturn(org.springframework.data.domain.PageImpl(listOf(invoice())))

        val page = service.getInvoicesForWorkspace("WS-1", null, pageable)

        assertEquals(1, page.content.size)
    }

    @Test
    fun `getInvoice returns invoice by uid`() {
        whenever(invoiceRepository.findByUid("INV-1")).thenReturn(invoice())

        assertEquals("INV-2025-001", service.getInvoice("INV-1").invoiceNumber)
    }

    @Test
    fun `getInvoice throws when not found`() {
        whenever(invoiceRepository.findByUid("NOPE")).thenReturn(null)

        assertThrows(SubscriptionException.InvoiceNotFound::class.java) { service.getInvoice("NOPE") }
    }

    @Test
    fun `getSummary aggregates pending, overdue and outstanding`() {
        val now = Instant.now()
        val pendingFuture = invoice {
            status = InvoiceStatus.PENDING
            totalAmount = BigDecimal("200.00")
            paidAmount = BigDecimal("50.00")
            dueDate = now.plus(5, ChronoUnit.DAYS)
        }
        val overdue = invoice {
            status = InvoiceStatus.OVERDUE
            totalAmount = BigDecimal("300.00")
            paidAmount = BigDecimal.ZERO
            dueDate = now.minus(5, ChronoUnit.DAYS)
        }
        val paid = invoice {
            status = InvoiceStatus.PAID
            totalAmount = BigDecimal("100.00")
            paidAmount = BigDecimal("100.00")
        }
        whenever(invoiceRepository.findByWorkspaceId("WS-1")).thenReturn(listOf(pendingFuture, overdue, paid))

        val summary = service.getSummary("WS-1")

        assertEquals(3, summary.totalInvoices)
        // pending statuses: PENDING + OVERDUE = 2
        assertEquals(2, summary.pendingInvoices)
        // overdue: the OVERDUE one (status or isOverdue)
        assertEquals(1, summary.overdueInvoices)
        // outstanding = remaining of pending(150) + overdue(300) = 450
        assertEquals(0, BigDecimal("450.00").compareTo(summary.totalOutstanding))
        // next due is the non-overdue pending invoice
        assertEquals(pendingFuture.dueDate, summary.nextDueDate)
        assertEquals(0, BigDecimal("200.00").compareTo(summary.nextInvoiceAmount!!))
    }

    @Test
    fun `getSummary with no invoices returns zeroes`() {
        whenever(invoiceRepository.findByWorkspaceId("WS-EMPTY")).thenReturn(emptyList())

        val summary = service.getSummary("WS-EMPTY")

        assertEquals(0, summary.totalInvoices)
        assertEquals(0, summary.pendingInvoices)
        assertEquals(0, summary.overdueInvoices)
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.totalOutstanding))
        assertNull(summary.nextDueDate)
        assertNull(summary.nextInvoiceAmount)
    }
}

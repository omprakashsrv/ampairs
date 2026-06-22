package com.ampairs.subscription

import com.ampairs.subscription.domain.model.Invoice
import com.ampairs.subscription.domain.model.InvoiceStatus
import com.ampairs.subscription.domain.repository.SubscriptionInvoiceRepository
import com.ampairs.subscription.domain.service.EmailNotificationService
import com.ampairs.subscription.domain.service.WorkspaceSuspensionService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceSuspensionServiceTest {

    @Mock
    private lateinit var invoiceRepository: SubscriptionInvoiceRepository

    @Mock
    private lateinit var emailNotificationService: EmailNotificationService

    private lateinit var service: WorkspaceSuspensionService

    @BeforeEach
    fun setUp() {
        service = WorkspaceSuspensionService(invoiceRepository, emailNotificationService)
        whenever(invoiceRepository.save(any<Invoice>())).thenAnswer { it.arguments[0] }
    }

    private fun overdueInvoice(daysPastDue: Long, block: Invoice.() -> Unit = {}): Invoice = Invoice().apply {
        uid = "INV-1"
        workspaceId = "WS-1"
        invoiceNumber = "INV-2025-001"
        status = InvoiceStatus.PENDING
        totalAmount = BigDecimal("100.00")
        paidAmount = BigDecimal.ZERO
        dueDate = Instant.now().minus(daysPastDue, ChronoUnit.DAYS)
        block()
    }

    @Test
    fun `checkOverdueInvoices suspends invoice past suspension threshold`() {
        val invoice = overdueInvoice(20)
        whenever(invoiceRepository.findOverdueInvoices(any())).thenReturn(listOf(invoice))

        service.checkOverdueInvoices()

        assertEquals(InvoiceStatus.SUSPENDED, invoice.status)
        verify(emailNotificationService).sendWorkspaceSuspensionEmail(invoice)
    }

    @Test
    fun `checkOverdueInvoices sends reminder on reminder days`() {
        val invoice = overdueInvoice(7)
        whenever(invoiceRepository.findOverdueInvoices(any())).thenReturn(listOf(invoice))

        service.checkOverdueInvoices()

        assertEquals(InvoiceStatus.OVERDUE, invoice.status)
        verify(emailNotificationService).sendOverdueInvoiceReminder(invoice, 7)
        verify(emailNotificationService, never()).sendWorkspaceSuspensionEmail(any())
    }

    @Test
    fun `checkOverdueInvoices does nothing on non-reminder, non-suspension day`() {
        val invoice = overdueInvoice(5)
        whenever(invoiceRepository.findOverdueInvoices(any())).thenReturn(listOf(invoice))

        service.checkOverdueInvoices()

        verify(emailNotificationService, never()).sendOverdueInvoiceReminder(any(), any())
        verify(emailNotificationService, never()).sendWorkspaceSuspensionEmail(any())
    }

    @Test
    fun `sendPreDueReminders notifies invoices due soon`() {
        val dueSoon = Invoice().apply {
            uid = "INV-2"
            workspaceId = "WS-1"
            invoiceNumber = "INV-2025-002"
            status = InvoiceStatus.PENDING
            totalAmount = BigDecimal("100.00")
            dueDate = Instant.now().plus(2, ChronoUnit.DAYS)
            lastReminderSentAt = null
        }
        whenever(invoiceRepository.findByStatus(InvoiceStatus.PENDING)).thenReturn(listOf(dueSoon))

        service.sendPreDueReminders()

        verify(emailNotificationService).sendInvoiceDueSoonReminder(org.mockito.kotlin.eq(dueSoon), any())
        assertEquals(1, dueSoon.reminderCount)
    }

    @Test
    fun `sendPreDueReminders skips invoices due too far out`() {
        val notSoon = Invoice().apply {
            workspaceId = "WS-1"
            invoiceNumber = "INV-2025-003"
            status = InvoiceStatus.PENDING
            dueDate = Instant.now().plus(10, ChronoUnit.DAYS)
        }
        whenever(invoiceRepository.findByStatus(InvoiceStatus.PENDING)).thenReturn(listOf(notSoon))

        service.sendPreDueReminders()

        verify(emailNotificationService, never()).sendInvoiceDueSoonReminder(any(), any())
    }

    @Test
    fun `sendPreDueReminders skips when reminder already sent today`() {
        val dueSoon = Invoice().apply {
            workspaceId = "WS-1"
            invoiceNumber = "INV-2025-004"
            status = InvoiceStatus.PENDING
            dueDate = Instant.now().plus(2, ChronoUnit.DAYS)
            lastReminderSentAt = Instant.now()
        }
        whenever(invoiceRepository.findByStatus(InvoiceStatus.PENDING)).thenReturn(listOf(dueSoon))

        service.sendPreDueReminders()

        verify(emailNotificationService, never()).sendInvoiceDueSoonReminder(any(), any())
    }

    @Test
    fun `reactivateWorkspace sends email when no pending invoices remain`() {
        val invoice = Invoice().apply {
            workspaceId = "WS-1"
            invoiceNumber = "INV-2025-005"
            status = InvoiceStatus.SUSPENDED
        }
        whenever(invoiceRepository.findPendingByWorkspaceId("WS-1")).thenReturn(emptyList())

        service.reactivateWorkspace(invoice)

        verify(emailNotificationService).sendWorkspaceReactivationEmail(invoice)
    }

    @Test
    fun `reactivateWorkspace does not send email when pending invoices remain`() {
        val invoice = Invoice().apply {
            workspaceId = "WS-1"
            invoiceNumber = "INV-2025-006"
            status = InvoiceStatus.SUSPENDED
        }
        whenever(invoiceRepository.findPendingByWorkspaceId("WS-1")).thenReturn(listOf(Invoice()))

        service.reactivateWorkspace(invoice)

        verify(emailNotificationService, never()).sendWorkspaceReactivationEmail(any())
    }

    @Test
    fun `reactivateWorkspace is a no-op for non-suspended invoice`() {
        val invoice = Invoice().apply { status = InvoiceStatus.PENDING; invoiceNumber = "INV-2025-007" }

        service.reactivateWorkspace(invoice)

        verify(invoiceRepository, never()).findPendingByWorkspaceId(any())
        verify(emailNotificationService, never()).sendWorkspaceReactivationEmail(any())
    }
}

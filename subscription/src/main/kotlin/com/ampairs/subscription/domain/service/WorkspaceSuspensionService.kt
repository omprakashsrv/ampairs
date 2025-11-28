package com.ampairs.subscription.domain.service

import com.ampairs.subscription.domain.model.Invoice
import com.ampairs.subscription.domain.model.InvoiceStatus
import com.ampairs.subscription.domain.repository.InvoiceRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Service for managing workspace suspension based on overdue invoices.
 * Implements grace period and suspension logic for postpaid billing.
 */
@Service
class WorkspaceSuspensionService(
    private val invoiceRepository: InvoiceRepository,
    private val emailNotificationService: EmailNotificationService
) {

    private val logger = LoggerFactory.getLogger(WorkspaceSuspensionService::class.java)

    companion object {
        // Grace period reminder days (before due date)
        const val REMINDER_BEFORE_DUE_DAYS = 3

        // Overdue reminder days (after due date)
        val OVERDUE_REMINDER_DAYS = listOf(3, 7, 14)

        // Default suspension threshold (15 days past due)
        const val DEFAULT_SUSPENSION_DAYS = 15
    }

    /**
     * Scheduled job to check overdue invoices and send reminders.
     * Runs daily at midnight UTC.
     */
    @Scheduled(cron = "0 0 0 * * ?", zone = "UTC")
    @Transactional
    fun checkOverdueInvoices() {
        logger.info("Starting overdue invoice check...")

        val now = Instant.now()
        val overdueInvoices = invoiceRepository.findOverdueInvoices(now)

        logger.info("Found ${overdueInvoices.size} overdue invoices")

        var suspendedCount = 0
        var remindersCount = 0

        overdueInvoices.forEach { invoice ->
            try {
                val daysPastDue = invoice.getDaysPastDue()

                when {
                    // Suspend workspace after grace period
                    daysPastDue >= DEFAULT_SUSPENSION_DAYS -> {
                        suspendWorkspaceForInvoice(invoice)
                        suspendedCount++
                    }

                    // Send overdue reminders
                    daysPastDue.toInt() in OVERDUE_REMINDER_DAYS -> {
                        sendOverdueReminder(invoice, daysPastDue.toInt())
                        remindersCount++
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to process overdue invoice ${invoice.invoiceNumber}", e)
            }
        }

        logger.info("Overdue invoice check completed. Suspended: $suspendedCount, Reminders: $remindersCount")
    }

    /**
     * Scheduled job to send pre-due date reminders.
     * Runs daily at 10 AM UTC.
     */
    @Scheduled(cron = "0 0 10 * * ?", zone = "UTC")
    @Transactional
    fun sendPreDueReminders() {
        logger.info("Checking for invoices due soon...")

        val now = Instant.now()
        val reminderThreshold = now.plus(REMINDER_BEFORE_DUE_DAYS.toLong(), ChronoUnit.DAYS)

        // Find pending invoices due within next 3 days
        val pendingInvoices = invoiceRepository.findByStatus(InvoiceStatus.PENDING)
        val dueSoonInvoices = pendingInvoices.filter { invoice ->
            invoice.dueDate.isAfter(now) && invoice.dueDate.isBefore(reminderThreshold)
        }

        logger.info("Found ${dueSoonInvoices.size} invoices due within $REMINDER_BEFORE_DUE_DAYS days")

        var remindersSent = 0
        dueSoonInvoices.forEach { invoice ->
            try {
                // Send reminder only if not already sent today
                if (shouldSendReminder(invoice)) {
                    sendPreDueReminder(invoice)
                    updateReminderTimestamp(invoice)
                    remindersSent++
                }
            } catch (e: Exception) {
                logger.error("Failed to send pre-due reminder for invoice ${invoice.invoiceNumber}", e)
            }
        }

        logger.info("Pre-due reminders sent: $remindersSent")
    }

    /**
     * Suspend workspace for unpaid invoice
     */
    private fun suspendWorkspaceForInvoice(invoice: Invoice) {
        logger.warn("Suspending workspace ${invoice.workspaceId} for overdue invoice ${invoice.invoiceNumber}")

        val now = Instant.now()

        // Update invoice status
        invoice.status = InvoiceStatus.SUSPENDED
        invoice.suspendedAt = now
        invoiceRepository.save(invoice)

        // Send suspension notification
        emailNotificationService.sendWorkspaceSuspensionEmail(invoice)

        // Note: Actual workspace status update should be done via WorkspaceService
        // This service only manages invoice status
        logger.info("Workspace ${invoice.workspaceId} suspended for invoice ${invoice.invoiceNumber}")
    }

    /**
     * Send overdue payment reminder
     */
    private fun sendOverdueReminder(invoice: Invoice, daysPastDue: Int) {
        logger.info("Sending overdue reminder for invoice ${invoice.invoiceNumber} ($daysPastDue days overdue)")

        val now = Instant.now()

        // Update invoice status to OVERDUE if still PENDING
        if (invoice.status == InvoiceStatus.PENDING) {
            invoice.status = InvoiceStatus.OVERDUE
        }

        invoice.lastReminderSentAt = now
        invoice.reminderCount++
        invoiceRepository.save(invoice)

        // Send email reminder
        emailNotificationService.sendOverdueInvoiceReminder(invoice, daysPastDue)
    }

    /**
     * Send pre-due date reminder
     */
    private fun sendPreDueReminder(invoice: Invoice) {
        val daysUntilDue = ChronoUnit.DAYS.between(Instant.now(), invoice.dueDate)
        logger.info("Sending pre-due reminder for invoice ${invoice.invoiceNumber} ($daysUntilDue days until due)")

        emailNotificationService.sendInvoiceDueSoonReminder(invoice, daysUntilDue.toInt())
    }

    /**
     * Check if reminder should be sent (avoid duplicate reminders)
     */
    private fun shouldSendReminder(invoice: Invoice): Boolean {
        if (invoice.lastReminderSentAt == null) return true

        // Only send one reminder per day
        val daysSinceLastReminder = ChronoUnit.DAYS.between(invoice.lastReminderSentAt, Instant.now())
        return daysSinceLastReminder >= 1
    }

    /**
     * Update reminder timestamp
     */
    private fun updateReminderTimestamp(invoice: Invoice) {
        invoice.lastReminderSentAt = Instant.now()
        invoice.reminderCount++
        invoiceRepository.save(invoice)
    }

    /**
     * Reactivate suspended workspace after payment
     */
    @Transactional
    fun reactivateWorkspace(invoice: Invoice) {
        if (invoice.status != InvoiceStatus.SUSPENDED) {
            logger.warn("Attempted to reactivate non-suspended invoice ${invoice.invoiceNumber}")
            return
        }

        logger.info("Reactivating workspace ${invoice.workspaceId} after payment of invoice ${invoice.invoiceNumber}")

        // Check if there are any other unpaid invoices
        val pendingInvoices = invoiceRepository.findPendingByWorkspaceId(invoice.workspaceId)
        if (pendingInvoices.isEmpty()) {
            // No more pending invoices, workspace can be fully reactivated
            emailNotificationService.sendWorkspaceReactivationEmail(invoice)
            logger.info("Workspace ${invoice.workspaceId} fully reactivated")
        } else {
            logger.warn("Workspace ${invoice.workspaceId} still has ${pendingInvoices.size} pending invoices")
        }
    }
}

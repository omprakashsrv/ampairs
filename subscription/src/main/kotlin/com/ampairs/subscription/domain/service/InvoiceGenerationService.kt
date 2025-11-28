package com.ampairs.subscription.domain.service

import com.ampairs.subscription.domain.model.*
import com.ampairs.subscription.domain.repository.*
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Service for generating invoices for postpaid subscriptions.
 * Runs monthly on the 1st of every month at 2 AM UTC.
 */
@Service
class InvoiceGenerationService(
    private val subscriptionRepository: SubscriptionRepository,
    private val invoiceRepository: InvoiceRepository,
    private val billingPreferencesRepository: BillingPreferencesRepository,
    private val invoicePaymentService: InvoicePaymentService
) {

    private val logger = LoggerFactory.getLogger(InvoiceGenerationService::class.java)
    private val invoiceCounter = AtomicInteger(0)

    /**
     * Scheduled job to generate monthly invoices.
     * Runs on the 1st of every month at 2 AM UTC.
     */
    @Scheduled(cron = "0 0 2 1 * ?", zone = "UTC")
    @Transactional
    fun generateMonthlyInvoices() {
        logger.info("Starting monthly invoice generation...")

        val activeSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE)
        logger.info("Found ${activeSubscriptions.size} active subscriptions")

        var successCount = 0
        var failureCount = 0

        activeSubscriptions.forEach { subscription ->
            try {
                // Only generate invoices for postpaid subscriptions
                val billingPrefs = billingPreferencesRepository.findByWorkspaceId(subscription.workspaceId)
                if (billingPrefs?.billingMode == BillingMode.POSTPAID) {
                    val invoice = createInvoiceForSubscription(subscription, billingPrefs)
                    logger.info("Generated invoice ${invoice.invoiceNumber} for subscription ${subscription.uid}")

                    // Process payment based on preferences
                    processInvoicePayment(invoice, billingPrefs)
                    successCount++
                }
            } catch (e: Exception) {
                logger.error("Failed to generate invoice for subscription ${subscription.uid}", e)
                failureCount++
            }
        }

        logger.info("Monthly invoice generation completed. Success: $successCount, Failures: $failureCount")
    }

    /**
     * Create invoice for a subscription manually
     */
    @Transactional
    fun generateInvoiceManually(
        subscription: Subscription,
        billingPeriodStart: Instant,
        billingPeriodEnd: Instant,
        notes: String? = null
    ): Invoice {
        logger.info("Manually generating invoice for subscription ${subscription.uid}")

        val billingPrefs = billingPreferencesRepository.findByWorkspaceId(subscription.workspaceId)
            ?: createDefaultBillingPreferences(subscription.workspaceId)

        return createInvoiceForSubscription(
            subscription = subscription,
            billingPreferences = billingPrefs,
            customPeriodStart = billingPeriodStart,
            customPeriodEnd = billingPeriodEnd,
            customNotes = notes
        )
    }

    /**
     * Create invoice for a subscription
     */
    private fun createInvoiceForSubscription(
        subscription: Subscription,
        billingPreferences: BillingPreferences,
        customPeriodStart: Instant? = null,
        customPeriodEnd: Instant? = null,
        customNotes: String? = null
    ): Invoice {
        val now = Instant.now()

        // Calculate billing period (last month)
        val periodStart = customPeriodStart ?: getLastMonthStart()
        val periodEnd = customPeriodEnd ?: getLastMonthEnd()
        val dueDate = now.plus(billingPreferences.gracePeriodDays.toLong(), ChronoUnit.DAYS)

        // Check if invoice already exists for this period
        val existingInvoices = invoiceRepository.findByWorkspaceIdAndBillingPeriod(
            subscription.workspaceId, periodStart, periodEnd
        )
        if (existingInvoices.isNotEmpty()) {
            logger.warn("Invoice already exists for workspace ${subscription.workspaceId} for period $periodStart to $periodEnd")
            return existingInvoices.first()
        }

        // Create invoice
        val invoice = Invoice().apply {
            workspaceId = subscription.workspaceId
            ownerId = subscription.workspaceId // Using workspaceId as ownerId
            invoiceNumber = generateInvoiceNumber()
            subscriptionId = subscription.id
            billingPeriodStart = periodStart
            billingPeriodEnd = periodEnd
            this.dueDate = dueDate
            status = InvoiceStatus.PENDING
            currency = billingPreferences.billingCurrency
            autoPaymentEnabled = billingPreferences.isAutoPaymentConfigured()
            paymentMethodId = billingPreferences.defaultPaymentMethodId
            generatedAt = now
            this.notes = customNotes
        }

        // Add subscription plan line item
        addSubscriptionPlanLineItem(invoice, subscription, periodStart, periodEnd)

        // Calculate tax
        calculateTax(invoice, billingPreferences)

        // Recalculate totals
        invoice.recalculateTotals()

        // Save invoice
        return invoiceRepository.save(invoice)
    }

    /**
     * Add subscription plan as line item
     */
    private fun addSubscriptionPlanLineItem(
        invoice: Invoice,
        subscription: Subscription,
        periodStart: Instant,
        periodEnd: Instant
    ) {
        val plan = subscription.plan ?: return
        val planName = subscription.planCode // Use planCode as fallback
        val lineItem = InvoiceLineItem().apply {
            this.invoice = invoice
            description = "$planName - ${formatBillingPeriod(periodStart, periodEnd)}"
            itemType = "SUBSCRIPTION"
            quantity = 1
            // Use next billing amount or default to 0
            unitPrice = subscription.nextBillingAmount ?: BigDecimal.ZERO
            amount = this.unitPrice
            this.periodStart = periodStart
            this.periodEnd = periodEnd
        }

        invoice.addLineItem(lineItem)
    }

    /**
     * Calculate and apply tax to invoice
     */
    private fun calculateTax(invoice: Invoice, billingPreferences: BillingPreferences) {
        // Apply 18% GST for India, 0% for others (customize as needed)
        val taxRate = when (billingPreferences.billingCountry) {
            "IN" -> BigDecimal("0.18") // 18% GST
            "US" -> BigDecimal("0.00") // No tax (varies by state)
            else -> BigDecimal("0.00")
        }

        invoice.taxAmount = invoice.subtotal.multiply(taxRate).setScale(4, RoundingMode.HALF_UP)
    }

    /**
     * Process invoice payment based on billing preferences
     */
    private fun processInvoicePayment(invoice: Invoice, billingPreferences: BillingPreferences) {
        if (billingPreferences.isAutoPaymentConfigured()) {
            // Attempt auto-charge
            try {
                invoicePaymentService.processAutoPayment(invoice)
                logger.info("Auto-payment successful for invoice ${invoice.invoiceNumber}")
            } catch (e: Exception) {
                logger.error("Auto-payment failed for invoice ${invoice.invoiceNumber}, sending payment link", e)
                sendPaymentLink(invoice)
            }
        } else {
            // Send payment link
            sendPaymentLink(invoice)
        }
    }

    /**
     * Send payment link via email
     */
    private fun sendPaymentLink(invoice: Invoice) {
        try {
            val paymentLink = invoicePaymentService.generatePaymentLink(invoice)
            logger.info("Payment link generated for invoice ${invoice.invoiceNumber}: $paymentLink")
            // TODO: Send email with payment link
        } catch (e: Exception) {
            logger.error("Failed to generate payment link for invoice ${invoice.invoiceNumber}", e)
        }
    }

    /**
     * Generate unique invoice number
     * Format: INV-YYYY-NNNN (e.g., INV-2025-0001)
     */
    private fun generateInvoiceNumber(): String {
        val year = YearMonth.now().year
        val sequence = invoiceCounter.incrementAndGet()
        return String.format("INV-%04d-%04d", year, sequence)
    }

    /**
     * Create default billing preferences for workspace
     */
    private fun createDefaultBillingPreferences(workspaceId: String): BillingPreferences {
        val preferences = BillingPreferences().apply {
            this.workspaceId = workspaceId
            billingMode = BillingMode.POSTPAID
            billingEmail = "" // Will be populated from workspace
            billingCurrency = "INR"
            gracePeriodDays = 15
            autoPaymentEnabled = false
            sendPaymentReminders = true
        }
        return billingPreferencesRepository.save(preferences)
    }

    /**
     * Get start of last month
     */
    private fun getLastMonthStart(): Instant {
        val lastMonth = YearMonth.now().minusMonths(1)
        return lastMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC)
    }

    /**
     * Get end of last month
     */
    private fun getLastMonthEnd(): Instant {
        val lastMonth = YearMonth.now().minusMonths(1)
        return lastMonth.atEndOfMonth().atTime(23, 59, 59).toInstant(ZoneOffset.UTC)
    }

    /**
     * Format billing period for display
     */
    private fun formatBillingPeriod(start: Instant, end: Instant): String {
        val startMonth = YearMonth.from(start.atZone(ZoneOffset.UTC))
        return "${startMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${startMonth.year}"
    }
}

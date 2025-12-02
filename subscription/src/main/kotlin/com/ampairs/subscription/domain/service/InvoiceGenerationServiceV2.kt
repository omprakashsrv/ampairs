package com.ampairs.subscription.domain.service

import com.ampairs.subscription.domain.model.*
import com.ampairs.subscription.domain.repository.*
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Improved invoice generation service with:
 * - Daily reconciliation (checks if invoices need to be generated)
 * - Idempotency via InvoiceGenerationLog
 * - Failure tracking and retry mechanism
 * - Per-subscription transactions (one failure doesn't affect others)
 * - Payment processing tracking
 */
@Service
class InvoiceGenerationServiceV2(
    private val subscriptionRepository: SubscriptionRepository,
    private val subscriptionInvoiceRepository: SubscriptionInvoiceRepository,
    private val billingPreferencesRepository: BillingPreferencesRepository,
    private val invoiceGenerationLogRepository: InvoiceGenerationLogRepository,
    private val invoicePaymentService: InvoicePaymentService
) {

    private val logger = LoggerFactory.getLogger(InvoiceGenerationServiceV2::class.java)

    /**
     * Daily job that:
     * 1. Checks if current month invoices need to be generated
     * 2. Checks if previous months were missed
     * 3. Retries failed generations
     * 4. Processes payment links for generated invoices
     *
     * Runs daily at 2 AM UTC
     */
    @Scheduled(cron = "0 0 2 * * ?", zone = "UTC")
    fun dailyInvoiceReconciliation() {
        logger.info("Starting daily invoice reconciliation...")

        try {
            // Step 1: Generate invoices for months that need them
            generateMissingInvoices()

            // Step 2: Retry failed generations
            retryFailedGenerations()

            // Step 3: Process pending payment links
            processPendingPaymentLinks()

            logger.info("Daily invoice reconciliation completed successfully")
        } catch (e: Exception) {
            logger.error("Error during daily invoice reconciliation", e)
        }
    }

    /**
     * Generate invoices for any months that are missing them.
     * Checks current month and up to 3 previous months.
     */
    private fun generateMissingInvoices() {
        logger.info("Checking for missing invoices...")

        val now = YearMonth.now()
        val monthsToCheck = listOf(
            now,                    // Current month
            now.minusMonths(1),     // Last month
            now.minusMonths(2),     // 2 months ago
            now.minusMonths(3)      // 3 months ago
        )

        var totalGenerated = 0

        monthsToCheck.forEach { yearMonth ->
            // Only generate if we're past the 1st of the month
            val firstOfMonth = yearMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC)
            if (Instant.now().isAfter(firstOfMonth)) {
                val generated = generateInvoicesForMonth(yearMonth.year, yearMonth.monthValue)
                totalGenerated += generated
                logger.info("Generated $generated invoices for ${yearMonth.month} ${yearMonth.year}")
            }
        }

        logger.info("Total missing invoices generated: $totalGenerated")
    }

    /**
     * Generate invoices for a specific month.
     * Only generates for subscriptions that don't have a log entry yet.
     */
    private fun generateInvoicesForMonth(year: Int, month: Int): Int {
        val activeSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE)
        var generatedCount = 0

        activeSubscriptions.forEach { subscription ->
            try {
                // Only process POSTPAID subscriptions
                val billingPrefs = billingPreferencesRepository.findByWorkspaceId(subscription.workspaceId)
                if (billingPrefs?.billingMode != BillingMode.POSTPAID) {
                    return@forEach
                }

                // Check if log already exists (idempotency)
                val existingLog = invoiceGenerationLogRepository
                    .findByWorkspaceIdAndBillingPeriodYearAndBillingPeriodMonth(
                        subscription.workspaceId, year, month
                    )

                if (existingLog != null) {
                    logger.debug("Invoice already attempted for workspace ${subscription.workspaceId} " +
                            "for $year-$month (status: ${existingLog.status})")
                    return@forEach
                }

                // Generate invoice for this subscription
                generateInvoiceForSubscription(subscription, year, month, billingPrefs)
                generatedCount++

            } catch (e: Exception) {
                logger.error("Error checking/generating invoice for subscription ${subscription.uid}", e)
            }
        }

        return generatedCount
    }

    /**
     * Generate invoice for a single subscription with proper transaction handling.
     * Each subscription gets its own transaction so one failure doesn't affect others.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun generateInvoiceForSubscription(
        subscription: Subscription,
        year: Int,
        month: Int,
        billingPreferences: BillingPreferences
    ) {
        val yearMonth = YearMonth.of(year, month)
        val periodStart = yearMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        val periodEnd = yearMonth.atEndOfMonth().atTime(23, 59, 59).toInstant(ZoneOffset.UTC)

        // Create or get generation log
        var log = invoiceGenerationLogRepository
            .findByWorkspaceIdAndBillingPeriodYearAndBillingPeriodMonth(
                subscription.workspaceId, year, month
            )

        if (log == null) {
            log = InvoiceGenerationLog().apply {
                workspaceId = subscription.workspaceId
                subscriptionId = subscription.id
                billingPeriodYear = year
                billingPeriodMonth = month
                billingPeriodStart = periodStart
                billingPeriodEnd = periodEnd
                status = InvoiceGenerationStatus.PENDING
            }
            log = invoiceGenerationLogRepository.save(log)
        }

        // Mark as in progress
        log.markInProgress()
        log = invoiceGenerationLogRepository.save(log)

        try {
            // Check if invoice already exists (double-check)
            val existingInvoices = subscriptionInvoiceRepository.findByWorkspaceIdAndBillingPeriod(
                subscription.workspaceId, periodStart, periodEnd
            )

            if (existingInvoices.isNotEmpty()) {
                val existing = existingInvoices.first()
                log.markSucceeded(existing.id, existing.invoiceNumber)
                invoiceGenerationLogRepository.save(log)
                logger.info("Invoice already exists for workspace ${subscription.workspaceId}, marked as success")
                return
            }

            // Create invoice
            val invoice = createInvoice(subscription, billingPreferences, periodStart, periodEnd)

            // Mark as succeeded
            log.markSucceeded(invoice.id, invoice.invoiceNumber)
            invoiceGenerationLogRepository.save(log)

            logger.info("Successfully generated invoice ${invoice.invoiceNumber} for workspace ${subscription.workspaceId}")

            // Process payment (in separate step to not fail invoice generation)
            processPaymentForInvoice(log, invoice, billingPreferences)

        } catch (e: Exception) {
            logger.error("Failed to generate invoice for workspace ${subscription.workspaceId} " +
                    "for $year-$month", e)
            log.markFailed(e, shouldRetryAgain = true)
            invoiceGenerationLogRepository.save(log)
        }
    }

    /**
     * Create invoice entity
     */
    private fun createInvoice(
        subscription: Subscription,
        billingPreferences: BillingPreferences,
        periodStart: Instant,
        periodEnd: Instant
    ): Invoice {
        val now = Instant.now()
        val dueDate = now.plus(billingPreferences.gracePeriodDays.toLong(), ChronoUnit.DAYS)

        // Create invoice
        val invoice = Invoice().apply {
            workspaceId = subscription.workspaceId
            ownerId = subscription.workspaceId
            invoiceNumber = generateInvoiceNumber(periodStart)
            subscriptionId = subscription.id
            billingPeriodStart = periodStart
            billingPeriodEnd = periodEnd
            this.dueDate = dueDate
            status = InvoiceStatus.PENDING
            currency = billingPreferences.billingCurrency
            autoPaymentEnabled = billingPreferences.isAutoPaymentConfigured()
            paymentMethodId = billingPreferences.defaultPaymentMethodId
            generatedAt = now
        }

        // Add subscription plan line item
        addSubscriptionPlanLineItem(invoice, subscription, periodStart, periodEnd)

        // Calculate tax
        calculateTax(invoice, billingPreferences)

        // Recalculate totals
        invoice.recalculateTotals()

        // Save invoice
        return subscriptionInvoiceRepository.save(invoice)
    }

    /**
     * Process payment for a generated invoice
     */
    private fun processPaymentForInvoice(
        log: InvoiceGenerationLog,
        invoice: Invoice,
        billingPreferences: BillingPreferences
    ) {
        try {
            if (billingPreferences.isAutoPaymentConfigured()) {
                // Attempt auto-charge
                log.markPaymentProcessing(PaymentProcessingStatus.AUTO_CHARGING)
                invoiceGenerationLogRepository.save(log)

                try {
                    invoicePaymentService.processAutoPayment(invoice)
                    log.markPaymentProcessing(PaymentProcessingStatus.AUTO_CHARGE_SUCCESS)
                    invoiceGenerationLogRepository.save(log)
                    logger.info("Auto-payment successful for invoice ${invoice.invoiceNumber}")
                } catch (e: Exception) {
                    logger.warn("Auto-payment failed for invoice ${invoice.invoiceNumber}, will send payment link", e)
                    log.markPaymentProcessing(PaymentProcessingStatus.AUTO_CHARGE_FAILED, e.message)
                    invoiceGenerationLogRepository.save(log)
                    sendPaymentLink(log, invoice)
                }
            } else {
                // Send payment link
                sendPaymentLink(log, invoice)
            }
        } catch (e: Exception) {
            logger.error("Error processing payment for invoice ${invoice.invoiceNumber}", e)
            log.markPaymentProcessing(PaymentProcessingStatus.LINK_FAILED, e.message)
            invoiceGenerationLogRepository.save(log)
        }
    }

    /**
     * Send payment link for invoice
     */
    private fun sendPaymentLink(log: InvoiceGenerationLog, invoice: Invoice) {
        try {
            log.markPaymentProcessing(PaymentProcessingStatus.LINK_GENERATING)
            invoiceGenerationLogRepository.save(log)

            val paymentLink = invoicePaymentService.generatePaymentLink(invoice)

            log.markPaymentProcessing(PaymentProcessingStatus.LINK_SENT)
            invoiceGenerationLogRepository.save(log)

            logger.info("Payment link generated for invoice ${invoice.invoiceNumber}: $paymentLink")
            // TODO: Send email with payment link

        } catch (e: Exception) {
            logger.error("Failed to generate payment link for invoice ${invoice.invoiceNumber}", e)
            log.markPaymentProcessing(PaymentProcessingStatus.LINK_FAILED, e.message)
            invoiceGenerationLogRepository.save(log)
        }
    }

    /**
     * Retry failed invoice generations with exponential backoff
     */
    private fun retryFailedGenerations() {
        logger.info("Checking for failed generations to retry...")

        val failedLogs = invoiceGenerationLogRepository.findFailedLogsReadyForRetry(Instant.now())

        if (failedLogs.isEmpty()) {
            logger.info("No failed generations ready for retry")
            return
        }

        logger.info("Found ${failedLogs.size} failed generations to retry")

        var retrySuccessCount = 0
        var retryFailureCount = 0

        failedLogs.forEach { log ->
            try {
                val subscription = subscriptionRepository.findById(log.subscriptionId)
                    .orElse(null)

                if (subscription == null) {
                    logger.warn("Subscription ${log.subscriptionId} not found for retry, marking as non-retryable")
                    log.shouldRetry = false
                    invoiceGenerationLogRepository.save(log)
                    return@forEach
                }

                val billingPrefs = billingPreferencesRepository.findByWorkspaceId(subscription.workspaceId)
                if (billingPrefs?.billingMode != BillingMode.POSTPAID) {
                    logger.warn("Workspace ${subscription.workspaceId} no longer in POSTPAID mode, skipping retry")
                    log.shouldRetry = false
                    invoiceGenerationLogRepository.save(log)
                    return@forEach
                }

                // Retry generation
                generateInvoiceForSubscription(
                    subscription,
                    log.billingPeriodYear,
                    log.billingPeriodMonth,
                    billingPrefs
                )
                retrySuccessCount++

            } catch (e: Exception) {
                logger.error("Retry failed for workspace ${log.workspaceId} " +
                        "${log.billingPeriodYear}-${log.billingPeriodMonth}", e)
                retryFailureCount++
            }
        }

        logger.info("Retry completed: $retrySuccessCount succeeded, $retryFailureCount failed")
    }

    /**
     * Process pending payment links for successfully generated invoices
     */
    private fun processPendingPaymentLinks() {
        logger.info("Processing pending payment links...")

        val logsWithPendingPayment = invoiceGenerationLogRepository.findLogsWithPendingPaymentProcessing()

        if (logsWithPendingPayment.isEmpty()) {
            logger.info("No pending payment links to process")
            return
        }

        logger.info("Found ${logsWithPendingPayment.size} invoices with pending payment processing")

        logsWithPendingPayment.forEach { log ->
            try {
                val invoice = subscriptionInvoiceRepository.findById(log.invoiceId!!)
                    .orElse(null)

                if (invoice == null) {
                    logger.warn("Invoice ${log.invoiceId} not found, skipping payment processing")
                    return@forEach
                }

                // Skip if already paid
                if (invoice.status == InvoiceStatus.PAID) {
                    log.markPaymentProcessing(PaymentProcessingStatus.AUTO_CHARGE_SUCCESS)
                    invoiceGenerationLogRepository.save(log)
                    return@forEach
                }

                val billingPrefs = billingPreferencesRepository.findByWorkspaceId(log.workspaceId)
                    ?: return@forEach

                processPaymentForInvoice(log, invoice, billingPrefs)

            } catch (e: Exception) {
                logger.error("Error processing pending payment for log ${log.uid}", e)
            }
        }
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
        val planName = subscription.planCode
        val lineItem = InvoiceLineItem().apply {
            this.invoice = invoice
            description = "$planName - ${formatBillingPeriod(periodStart, periodEnd)}"
            itemType = "SUBSCRIPTION"
            quantity = 1
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
        val taxRate = when (billingPreferences.billingCountry) {
            "IN" -> BigDecimal("0.18") // 18% GST
            "US" -> BigDecimal("0.00") // No tax
            else -> BigDecimal("0.00")
        }

        invoice.taxAmount = invoice.subtotal.multiply(taxRate).setScale(4, RoundingMode.HALF_UP)
    }

    /**
     * Generate unique invoice number
     * Format: INV-YYYY-MM-NNNNNN (e.g., INV-2025-01-000123)
     * Uses database sequence for uniqueness
     */
    private fun generateInvoiceNumber(periodStart: Instant): String {
        val yearMonth = YearMonth.from(periodStart.atZone(ZoneOffset.UTC))
        val year = yearMonth.year
        val month = yearMonth.monthValue

        // Count existing invoices for this month to get sequence
        val existingCount = subscriptionInvoiceRepository.count()
        val sequence = existingCount + 1

        return String.format("INV-%04d-%02d-%06d", year, month, sequence)
    }

    /**
     * Format billing period for display
     */
    private fun formatBillingPeriod(start: Instant, end: Instant): String {
        val startMonth = YearMonth.from(start.atZone(ZoneOffset.UTC))
        return "${startMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${startMonth.year}"
    }

    /**
     * Manual trigger for invoice generation (for admin/testing)
     */
    @Transactional
    fun manuallyGenerateInvoice(
        workspaceId: String,
        year: Int,
        month: Int
    ): Invoice? {
        logger.info("Manually generating invoice for workspace $workspaceId for $year-$month")

        val subscription = subscriptionRepository.findByWorkspaceId(workspaceId)
            ?: throw IllegalArgumentException("No subscription found for workspace $workspaceId")

        val billingPrefs = billingPreferencesRepository.findByWorkspaceId(workspaceId)
            ?: throw IllegalArgumentException("No billing preferences found for workspace $workspaceId")

        if (billingPrefs.billingMode != BillingMode.POSTPAID) {
            throw IllegalArgumentException("Workspace is not in POSTPAID mode")
        }

        generateInvoiceForSubscription(subscription, year, month, billingPrefs)

        // Return the generated invoice
        val log = invoiceGenerationLogRepository
            .findByWorkspaceIdAndBillingPeriodYearAndBillingPeriodMonth(workspaceId, year, month)

        return if (log?.invoiceId != null) {
            subscriptionInvoiceRepository.findById(log.invoiceId!!).orElse(null)
        } else {
            null
        }
    }

    /**
     * Get generation statistics for a period
     */
    fun getGenerationStats(year: Int, month: Int): GenerationStats {
        val totalLogs = invoiceGenerationLogRepository
            .findByBillingPeriodYearAndBillingPeriodMonth(year, month)

        val successCount = totalLogs.count { it.status == InvoiceGenerationStatus.SUCCESS }
        val failedCount = totalLogs.count { it.status == InvoiceGenerationStatus.FAILED }
        val pendingCount = totalLogs.count { it.status == InvoiceGenerationStatus.PENDING }
        val inProgressCount = totalLogs.count { it.status == InvoiceGenerationStatus.IN_PROGRESS }

        return GenerationStats(
            year = year,
            month = month,
            totalAttempts = totalLogs.size,
            successCount = successCount,
            failedCount = failedCount,
            pendingCount = pendingCount,
            inProgressCount = inProgressCount,
            failedWorkspaces = totalLogs.filter { it.status == InvoiceGenerationStatus.FAILED }
                .map { it.workspaceId to it.errorMessage }
        )
    }
}

/**
 * Statistics for invoice generation
 */
data class GenerationStats(
    val year: Int,
    val month: Int,
    val totalAttempts: Int,
    val successCount: Int,
    val failedCount: Int,
    val pendingCount: Int,
    val inProgressCount: Int,
    val failedWorkspaces: List<Pair<String, String?>>
)

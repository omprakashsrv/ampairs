package com.ampairs.subscription.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.subscription.domain.dto.*
import com.ampairs.subscription.domain.model.Invoice
import com.ampairs.subscription.domain.model.InvoiceStatus
import com.ampairs.subscription.domain.repository.InvoiceRepository
import com.ampairs.subscription.domain.repository.SubscriptionRepository
import com.ampairs.subscription.domain.service.InvoiceGenerationService
import com.ampairs.subscription.domain.service.InvoicePaymentService
import com.ampairs.subscription.domain.service.WorkspaceSuspensionService
import com.ampairs.subscription.exception.SubscriptionException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.Instant
import jakarta.validation.Valid

/**
 * Controller for invoice management in postpaid billing system.
 */
@RestController
@RequestMapping("/api/v1/billing/invoices")
@PreAuthorize("isAuthenticated()")
class InvoiceController(
    private val invoiceRepository: InvoiceRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val invoiceGenerationService: InvoiceGenerationService,
    private val invoicePaymentService: InvoicePaymentService,
    private val workspaceSuspensionService: WorkspaceSuspensionService
) {

    /**
     * Get all invoices for current workspace
     */
    @GetMapping
    fun getInvoices(
        @RequestParam(required = false) workspaceId: String?,
        @RequestParam(required = false) status: InvoiceStatus?,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ApiResponse<Page<InvoiceResponse>> {
        // TODO: Get workspaceId from authentication context
        val effectiveWorkspaceId = workspaceId ?: throw SubscriptionException.WorkspaceRequired()

        val invoices = if (status != null) {
            invoiceRepository.findByStatus(status)
                .filter { it.workspaceId == effectiveWorkspaceId }
                .let { org.springframework.data.domain.PageImpl(it, pageable, it.size.toLong()) }
        } else {
            invoiceRepository.findByWorkspaceIdOrderByCreatedAtDesc(effectiveWorkspaceId, pageable)
        }

        return ApiResponse.success(invoices.map { it.asInvoiceResponse() })
    }

    /**
     * Get invoice by UID
     */
    @GetMapping("/{invoiceUid}")
    fun getInvoice(@PathVariable invoiceUid: String): ApiResponse<InvoiceResponse> {
        val invoice = invoiceRepository.findByUid(invoiceUid)
            ?: throw SubscriptionException.InvoiceNotFound(invoiceUid)

        return ApiResponse.success(invoice.asInvoiceResponse())
    }

    /**
     * Generate invoice manually
     */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    fun generateInvoice(
        @Valid @RequestBody request: GenerateInvoiceRequest
    ): ApiResponse<InvoiceResponse> {
        val subscription = subscriptionRepository.findById(request.subscriptionId)
            .orElseThrow { SubscriptionException.NotFound("Subscription not found") }

        val invoice = invoiceGenerationService.generateInvoiceManually(
            subscription = subscription,
            billingPeriodStart = request.billingPeriodStart,
            billingPeriodEnd = request.billingPeriodEnd,
            notes = request.notes
        )

        return ApiResponse.success(invoice.asInvoiceResponse())
    }

    /**
     * Pay an invoice (initiate payment)
     */
    @PostMapping("/{invoiceUid}/pay")
    fun payInvoice(
        @PathVariable invoiceUid: String,
        @Valid @RequestBody request: PayInvoiceRequest
    ): ApiResponse<PaymentLinkResponse> {
        val invoice = invoiceRepository.findByUid(invoiceUid)
            ?: throw SubscriptionException.InvoiceNotFound(invoiceUid)

        // Check if invoice is already paid
        if (invoice.isFullyPaid()) {
            throw SubscriptionException.PaymentFailed("Invoice is already paid")
        }

        val paymentLink = if (request.useAutoCharge && invoice.autoPaymentEnabled) {
            // Process auto-charge
            invoicePaymentService.processAutoPayment(invoice)
            null // No payment link needed
        } else {
            // Generate payment link
            invoicePaymentService.generatePaymentLink(invoice)
        }

        return if (paymentLink != null) {
            ApiResponse.success(
                PaymentLinkResponse(
                    invoiceUid = invoice.uid,
                    paymentLinkUrl = paymentLink,
                    expiresAt = null // TODO: Add expiry if provider supports it
                )
            )
        } else {
            // Auto-charge was processed
            throw SubscriptionException.PaymentFailed("Payment processed successfully")
        }
    }

    /**
     * Get invoice summary for dashboard
     */
    @GetMapping("/summary")
    fun getInvoiceSummary(
        @RequestParam(required = false) workspaceId: String?
    ): ApiResponse<InvoiceSummaryResponse> {
        val effectiveWorkspaceId = workspaceId ?: throw SubscriptionException.WorkspaceRequired()

        val allInvoices = invoiceRepository.findByWorkspaceId(effectiveWorkspaceId)
        val pendingInvoices = allInvoices.filter {
            it.status in listOf(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE, InvoiceStatus.PARTIALLY_PAID)
        }
        val overdueInvoices = allInvoices.filter { it.status == InvoiceStatus.OVERDUE || it.isOverdue() }

        val totalOutstanding = pendingInvoices.sumOf { it.getRemainingBalance() }

        val nextInvoice = pendingInvoices
            .filter { !it.isOverdue() }
            .minByOrNull { it.dueDate }

        val summary = InvoiceSummaryResponse(
            totalInvoices = allInvoices.size,
            pendingInvoices = pendingInvoices.size,
            overdueInvoices = overdueInvoices.size,
            totalOutstanding = totalOutstanding,
            nextDueDate = nextInvoice?.dueDate,
            nextInvoiceAmount = nextInvoice?.totalAmount
        )

        return ApiResponse.success(summary)
    }

    /**
     * Download invoice PDF
     * TODO: Implement PDF generation
     */
    @GetMapping("/{invoiceUid}/download")
    fun downloadInvoice(@PathVariable invoiceUid: String): ApiResponse<String> {
        val invoice = invoiceRepository.findByUid(invoiceUid)
            ?: throw SubscriptionException.InvoiceNotFound(invoiceUid)

        // TODO: Generate PDF and return download URL
        return ApiResponse.success("PDF generation not yet implemented")
    }

    /**
     * Retry failed payment
     */
    @PostMapping("/{invoiceUid}/retry-payment")
    fun retryPayment(@PathVariable invoiceUid: String): ApiResponse<PaymentLinkResponse> {
        val invoice = invoiceRepository.findByUid(invoiceUid)
            ?: throw SubscriptionException.InvoiceNotFound(invoiceUid)

        if (invoice.isFullyPaid()) {
            throw SubscriptionException.PaymentFailed("Invoice is already paid")
        }

        // Try auto-payment first, fallback to payment link
        val paymentLink = if (invoice.autoPaymentEnabled && invoice.paymentMethodId != null) {
            try {
                invoicePaymentService.processAutoPayment(invoice)
                null
            } catch (e: Exception) {
                invoicePaymentService.generatePaymentLink(invoice)
            }
        } else {
            invoicePaymentService.generatePaymentLink(invoice)
        }

        return if (paymentLink != null) {
            ApiResponse.success(
                PaymentLinkResponse(
                    invoiceUid = invoice.uid,
                    paymentLinkUrl = paymentLink
                )
            )
        } else {
            throw SubscriptionException.PaymentFailed("Payment processed successfully")
        }
    }
}

/**
 * Controller for billing preferences management
 */
@RestController
@RequestMapping("/api/v1/billing/preferences")
@PreAuthorize("isAuthenticated()")
class BillingPreferencesController(
    private val billingPreferencesRepository: com.ampairs.subscription.domain.repository.BillingPreferencesRepository,
    private val paymentMethodRepository: com.ampairs.subscription.domain.repository.PaymentMethodRepository
) {

    /**
     * Get billing preferences for workspace
     */
    @GetMapping
    fun getBillingPreferences(
        @RequestParam(required = false) workspaceId: String?
    ): ApiResponse<BillingPreferencesResponse> {
        val effectiveWorkspaceId = workspaceId ?: throw SubscriptionException.WorkspaceRequired()

        val preferences = billingPreferencesRepository.findByWorkspaceId(effectiveWorkspaceId)
            ?: throw SubscriptionException.NotFound("Billing preferences not found")

        return ApiResponse.success(preferences.asBillingPreferencesResponse())
    }

    /**
     * Update billing preferences
     */
    @PutMapping
    fun updateBillingPreferences(
        @RequestParam(required = false) workspaceId: String?,
        @Valid @RequestBody request: UpdateBillingPreferencesRequest
    ): ApiResponse<BillingPreferencesResponse> {
        val effectiveWorkspaceId = workspaceId ?: throw SubscriptionException.WorkspaceRequired()

        var preferences = billingPreferencesRepository.findByWorkspaceId(effectiveWorkspaceId)
            ?: com.ampairs.subscription.domain.model.BillingPreferences().apply {
                this.workspaceId = effectiveWorkspaceId
            }

        // Update fields
        request.autoPaymentEnabled?.let { preferences.autoPaymentEnabled = it }
        request.defaultPaymentMethodId?.let {
            // Verify payment method exists and belongs to workspace
            val paymentMethod = paymentMethodRepository.findById(it)
                .orElseThrow { SubscriptionException.PaymentMethodNotFound(it.toString()) }

            if (paymentMethod.workspaceId != effectiveWorkspaceId) {
                throw SubscriptionException.PaymentFailed("Payment method does not belong to this workspace")
            }

            preferences.defaultPaymentMethodId = it
        }
        request.billingMode?.let { preferences.billingMode = it }
        request.billingEmail?.let { preferences.billingEmail = it }
        request.sendPaymentReminders?.let { preferences.sendPaymentReminders = it }
        request.gracePeriodDays?.let { preferences.gracePeriodDays = it }
        request.taxIdentifier?.let { preferences.taxIdentifier = it }
        request.billingAddress?.let { address ->
            preferences.billingAddressLine1 = address.line1
            preferences.billingAddressLine2 = address.line2
            preferences.billingCity = address.city
            preferences.billingState = address.state
            preferences.billingPostalCode = address.postalCode
            preferences.billingCountry = address.country
        }

        preferences = billingPreferencesRepository.save(preferences)

        return ApiResponse.success(preferences.asBillingPreferencesResponse())
    }
}

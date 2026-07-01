package com.ampairs.subscription.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.subscription.domain.dto.*
import com.ampairs.subscription.domain.model.InvoiceStatus
import com.ampairs.subscription.domain.service.BillingPreferencesService
import com.ampairs.subscription.domain.service.InvoiceGenerationService
import com.ampairs.subscription.domain.service.InvoicePaymentService
import com.ampairs.subscription.domain.service.SubscriptionInvoiceQueryService
import com.ampairs.subscription.domain.service.SubscriptionService
import com.ampairs.subscription.exception.SubscriptionException
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/subscription/v1/invoices")
@PreAuthorize("isAuthenticated()")
class SubscriptionInvoiceController(
    private val subscriptionInvoiceQueryService: SubscriptionInvoiceQueryService,
    private val subscriptionService: SubscriptionService,
    private val invoiceGenerationService: InvoiceGenerationService,
    private val invoicePaymentService: InvoicePaymentService,
) {

    @GetMapping
    fun getInvoices(
        @RequestParam(required = false) status: InvoiceStatus?,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ApiResponse<Page<InvoiceResponse>> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        val invoices = subscriptionInvoiceQueryService.getInvoicesForWorkspace(workspaceId, status, pageable)
        return ApiResponse.success(invoices)
    }

    @GetMapping("/{invoiceUid}")
    fun getInvoice(@PathVariable invoiceUid: String): ApiResponse<InvoiceResponse> {
        val invoice = subscriptionInvoiceQueryService.getInvoice(invoiceUid)
        return ApiResponse.success(invoice.asInvoiceResponse())
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    fun generateInvoice(@Valid @RequestBody request: GenerateInvoiceRequest): ApiResponse<InvoiceResponse> {
        val subscription = subscriptionService.getSubscriptionById(request.subscriptionId)

        val invoice = invoiceGenerationService.generateInvoiceManually(
            subscription = subscription,
            billingPeriodStart = request.billingPeriodStart,
            billingPeriodEnd = request.billingPeriodEnd,
            notes = request.notes
        )

        return ApiResponse.success(invoice.asInvoiceResponse())
    }

    @PostMapping("/{invoiceUid}/pay")
    fun payInvoice(
        @PathVariable invoiceUid: String,
        @Valid @RequestBody request: PayInvoiceRequest
    ): ApiResponse<PaymentLinkResponse> {
        val invoice = subscriptionInvoiceQueryService.getInvoice(invoiceUid)

        if (invoice.isFullyPaid()) {
            throw SubscriptionException.PaymentFailed("Invoice is already paid")
        }

        val paymentLink = if (request.useAutoCharge && invoice.autoPaymentEnabled) {
            invoicePaymentService.processAutoPayment(invoice)
            null
        } else {
            invoicePaymentService.generatePaymentLink(invoice)
        }

        return ApiResponse.success(
            PaymentLinkResponse(
                invoiceUid = invoice.uid,
                paymentLinkUrl = paymentLink,
                autoCharged = paymentLink == null
            )
        )
    }

    @GetMapping("/summary")
    fun getInvoiceSummary(): ApiResponse<InvoiceSummaryResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        return ApiResponse.success(subscriptionInvoiceQueryService.getSummary(workspaceId))
    }

    @GetMapping("/{invoiceUid}/download")
    fun downloadInvoice(@PathVariable invoiceUid: String): ApiResponse<String> {
        subscriptionInvoiceQueryService.getInvoice(invoiceUid)
        // TODO: Generate PDF and return download URL
        return ApiResponse.success("PDF generation not yet implemented")
    }

    @PostMapping("/{invoiceUid}/retry-payment")
    fun retryPayment(@PathVariable invoiceUid: String): ApiResponse<PaymentLinkResponse> {
        val invoice = subscriptionInvoiceQueryService.getInvoice(invoiceUid)

        if (invoice.isFullyPaid()) {
            throw SubscriptionException.PaymentFailed("Invoice is already paid")
        }

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

        return ApiResponse.success(
            PaymentLinkResponse(
                invoiceUid = invoice.uid,
                paymentLinkUrl = paymentLink,
                autoCharged = paymentLink == null
            )
        )
    }
}

@RestController
@RequestMapping("/subscription/v1/billing-preferences")
@PreAuthorize("isAuthenticated()")
class SubscriptionBillingPreferencesController(
    private val billingPreferencesService: BillingPreferencesService
) {

    @GetMapping
    fun getBillingPreferences(): ApiResponse<BillingPreferencesResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        return ApiResponse.success(billingPreferencesService.getBillingPreferences(workspaceId).asBillingPreferencesResponse())
    }

    @PutMapping
    fun updateBillingPreferences(
        @Valid @RequestBody request: UpdateBillingPreferencesRequest
    ): ApiResponse<BillingPreferencesResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        val updated = billingPreferencesService.updateBillingPreferences(workspaceId, request)
        return ApiResponse.success(updated.asBillingPreferencesResponse())
    }
}

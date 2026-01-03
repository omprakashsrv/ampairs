package com.ampairs.subscription.domain.dto

import com.ampairs.subscription.domain.model.*
import java.math.BigDecimal
import java.time.Instant

// =====================
// Request DTOs
// =====================

/**
 * Request to generate invoice manually
 */
data class GenerateInvoiceRequest(
    val subscriptionId: Long,
    val billingPeriodStart: Instant,
    val billingPeriodEnd: Instant,
    val notes: String? = null
)

/**
 * Request to pay an invoice
 */
data class PayInvoiceRequest(
    val invoiceUid: String,
    val paymentMethodUid: String? = null, // If null, use default or payment link
    val useAutoCharge: Boolean = false
)

/**
 * Request to update billing preferences
 */
data class UpdateBillingPreferencesRequest(
    val autoPaymentEnabled: Boolean? = null,
    val defaultPaymentMethodId: Long? = null,
    val billingMode: BillingMode? = null,
    val billingEmail: String? = null,
    val sendPaymentReminders: Boolean? = null,
    val gracePeriodDays: Int? = null,
    val taxIdentifier: String? = null,
    val billingAddress: BillingAddressDto? = null
)

/**
 * Billing address DTO
 */
data class BillingAddressDto(
    val line1: String? = null,
    val line2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null
)

// =====================
// Response DTOs
// =====================

/**
 * Invoice response DTO
 */
data class InvoiceResponse(
    val uid: String,
    val invoiceNumber: String,
    val subscriptionId: Long,
    val billingPeriodStart: Instant,
    val billingPeriodEnd: Instant,
    val dueDate: Instant,
    val status: InvoiceStatus,
    val subtotal: BigDecimal,
    val taxAmount: BigDecimal,
    val discountAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val paidAmount: BigDecimal,
    val remainingBalance: BigDecimal,
    val currency: String,
    val autoPaymentEnabled: Boolean,
    val paymentLinkUrl: String? = null,
    val generatedAt: Instant?,
    val paidAt: Instant?,
    val suspendedAt: Instant?,
    val isOverdue: Boolean,
    val daysPastDue: Long,
    val lineItems: List<InvoiceLineItemResponse>,
    val notes: String? = null,
    val createdAt: Instant?,
    val updatedAt: Instant?
)

/**
 * Invoice line item response DTO
 */
data class InvoiceLineItemResponse(
    val description: String,
    val itemType: String?,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val amount: BigDecimal,
    val taxPercent: BigDecimal,
    val periodStart: Instant?,
    val periodEnd: Instant?
)

/**
 * Billing preferences response DTO
 */
data class BillingPreferencesResponse(
    val workspaceId: String,
    val autoPaymentEnabled: Boolean,
    val defaultPaymentMethodId: Long?,
    val billingMode: BillingMode,
    val billingEmail: String,
    val sendPaymentReminders: Boolean,
    val gracePeriodDays: Int,
    val billingCurrency: String,
    val taxIdentifier: String?,
    val billingAddress: String?,
    val isAutoPaymentConfigured: Boolean
)

/**
 * Payment link response
 */
data class PaymentLinkResponse(
    val invoiceUid: String,
    val paymentLinkUrl: String,
    val expiresAt: Instant? = null
)

/**
 * Invoice summary for dashboard
 */
data class InvoiceSummaryResponse(
    val totalInvoices: Int,
    val pendingInvoices: Int,
    val overdueInvoices: Int,
    val totalOutstanding: BigDecimal,
    val nextDueDate: Instant?,
    val nextInvoiceAmount: BigDecimal?
)

// =====================
// Extension Functions (Entity -> DTO)
// =====================

/**
 * Convert Invoice entity to response DTO
 */
fun Invoice.asInvoiceResponse(): InvoiceResponse {
    return InvoiceResponse(
        uid = this.uid,
        invoiceNumber = this.invoiceNumber,
        subscriptionId = this.subscriptionId,
        billingPeriodStart = this.billingPeriodStart,
        billingPeriodEnd = this.billingPeriodEnd,
        dueDate = this.dueDate,
        status = this.status,
        subtotal = this.subtotal,
        taxAmount = this.taxAmount,
        discountAmount = this.discountAmount,
        totalAmount = this.totalAmount,
        paidAmount = this.paidAmount,
        remainingBalance = this.getRemainingBalance(),
        currency = this.currency,
        autoPaymentEnabled = this.autoPaymentEnabled,
        paymentLinkUrl = this.paymentLinkUrl,
        generatedAt = this.generatedAt,
        paidAt = this.paidAt,
        suspendedAt = this.suspendedAt,
        isOverdue = this.isOverdue(),
        daysPastDue = this.getDaysPastDue(),
        lineItems = this.lineItems.map { it.asInvoiceLineItemResponse() },
        notes = this.notes,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

/**
 * Convert InvoiceLineItem entity to response DTO
 */
fun InvoiceLineItem.asInvoiceLineItemResponse(): InvoiceLineItemResponse {
    return InvoiceLineItemResponse(
        description = this.description,
        itemType = this.itemType,
        quantity = this.quantity,
        unitPrice = this.unitPrice,
        amount = this.amount,
        taxPercent = this.taxPercent,
        periodStart = this.periodStart,
        periodEnd = this.periodEnd
    )
}

/**
 * Convert BillingPreferences entity to response DTO
 */
fun BillingPreferences.asBillingPreferencesResponse(): BillingPreferencesResponse {
    return BillingPreferencesResponse(
        workspaceId = this.workspaceId,
        autoPaymentEnabled = this.autoPaymentEnabled,
        defaultPaymentMethodId = this.defaultPaymentMethodId,
        billingMode = this.billingMode,
        billingEmail = this.billingEmail,
        sendPaymentReminders = this.sendPaymentReminders,
        gracePeriodDays = this.gracePeriodDays,
        billingCurrency = this.billingCurrency,
        taxIdentifier = this.taxIdentifier,
        billingAddress = this.getFullBillingAddress(),
        isAutoPaymentConfigured = this.isAutoPaymentConfigured()
    )
}

/**
 * Convert list of invoices to response DTOs
 */
fun List<Invoice>.asInvoiceResponses(): List<InvoiceResponse> {
    return this.map { it.asInvoiceResponse() }
}

package com.ampairs.subscription.domain.service

import com.ampairs.subscription.domain.model.*
import com.ampairs.subscription.domain.repository.*
import com.ampairs.subscription.exception.SubscriptionException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Service for processing invoice payments.
 * Supports both auto-charge (saved payment methods) and payment links (Razorpay/Stripe).
 */
@Service
class InvoicePaymentService(
    private val subscriptionInvoiceRepository: SubscriptionInvoiceRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val paymentTransactionRepository: PaymentTransactionRepository
) {

    private val logger = LoggerFactory.getLogger(InvoicePaymentService::class.java)

    /**
     * Generate payment link for manual payment
     */
    @Transactional
    fun generatePaymentLink(invoice: Invoice): String {
        logger.info("Generating payment link for invoice ${invoice.invoiceNumber}")

        val paymentLink = when (invoice.currency) {
            "INR" -> generateRazorpayPaymentLink(invoice)
            else -> generateStripePaymentLink(invoice)
        }

        // Update invoice with payment link
        invoice.paymentLinkUrl = paymentLink
        subscriptionInvoiceRepository.save(invoice)

        return paymentLink
    }

    /**
     * Process auto-charge for invoice using saved payment method
     */
    @Transactional
    fun processAutoPayment(invoice: Invoice) {
        logger.info("Processing auto-payment for invoice ${invoice.invoiceNumber}")

        if (!invoice.autoPaymentEnabled || invoice.paymentMethodId == null) {
            throw SubscriptionException.PaymentFailed("Auto-payment not enabled for this invoice")
        }

        val paymentMethod = paymentMethodRepository.findById(invoice.paymentMethodId!!)
            .orElseThrow { SubscriptionException.PaymentMethodNotFound(invoice.paymentMethodId.toString()) }

        if (!paymentMethod.isVerified()) {
            throw SubscriptionException.PaymentFailed("Payment method is not verified or expired")
        }

        // Charge payment method based on provider
        val transactionId = when (paymentMethod.paymentProvider) {
            PaymentProvider.RAZORPAY -> chargeRazorpayPaymentMethod(invoice, paymentMethod)
            PaymentProvider.STRIPE -> chargeStripePaymentMethod(invoice, paymentMethod)
            else -> throw SubscriptionException.PaymentFailed("Unsupported payment provider: ${paymentMethod.paymentProvider}")
        }

        // Mark invoice as paid
        markInvoiceAsPaid(invoice, transactionId)
        logger.info("Auto-payment successful for invoice ${invoice.invoiceNumber}, transaction: $transactionId")
    }

    /**
     * Process manual payment (webhook callback)
     */
    @Transactional
    fun processManualPayment(invoice: Invoice, externalPaymentId: String, provider: PaymentProvider) {
        logger.info("Processing manual payment for invoice ${invoice.invoiceNumber}, payment: $externalPaymentId")

        // Verify payment with provider
        val isVerified = when (provider) {
            PaymentProvider.RAZORPAY -> verifyRazorpayPayment(externalPaymentId)
            PaymentProvider.STRIPE -> verifyStripePayment(externalPaymentId)
            else -> false
        }

        if (!isVerified) {
            throw SubscriptionException.PaymentFailed("Payment verification failed for $externalPaymentId")
        }

        // Mark invoice as paid
        markInvoiceAsPaid(invoice, externalPaymentId)
        logger.info("Manual payment successful for invoice ${invoice.invoiceNumber}")
    }

    /**
     * Mark invoice as paid and create payment transaction
     */
    private fun markInvoiceAsPaid(invoice: Invoice, externalPaymentId: String) {
        val now = Instant.now()

        // Update invoice status
        invoice.status = InvoiceStatus.PAID
        invoice.paidAmount = invoice.totalAmount
        invoice.paidAt = now
        subscriptionInvoiceRepository.save(invoice)

        // Create payment transaction record
        val transaction = PaymentTransaction().apply {
            this.workspaceId = invoice.workspaceId
            this.subscriptionId = invoice.subscriptionId.toString()
            this.externalPaymentId = externalPaymentId
            this.externalInvoiceId = invoice.invoiceNumber
            this.paymentProvider = when (invoice.currency) {
                "INR" -> PaymentProvider.RAZORPAY
                else -> PaymentProvider.STRIPE
            }
            this.status = PaymentStatus.SUCCEEDED
            this.amount = invoice.totalAmount
            this.currency = invoice.currency
            this.netAmount = invoice.totalAmount
        }
        paymentTransactionRepository.save(transaction)
    }

    // =====================
    // Razorpay Integration
    // =====================

    /**
     * Generate Razorpay payment link
     * TODO: Implement actual Razorpay integration
     */
    private fun generateRazorpayPaymentLink(invoice: Invoice): String {
        logger.warn("Razorpay integration not yet implemented for invoice ${invoice.invoiceNumber}")
        // TODO: Implement Razorpay invoice creation
        return "https://razorpay.com/payment-link-placeholder"
    }

    /**
     * Charge Razorpay payment method
     * TODO: Implement actual Razorpay integration
     */
    private fun chargeRazorpayPaymentMethod(invoice: Invoice, paymentMethod: PaymentMethod): String {
        logger.warn("Razorpay auto-charge not yet implemented for invoice ${invoice.invoiceNumber}")
        throw SubscriptionException.PaymentFailed("Auto-charge not yet implemented")
    }

    /**
     * Verify Razorpay payment
     * TODO: Implement actual Razorpay integration
     */
    private fun verifyRazorpayPayment(paymentId: String): Boolean {
        logger.warn("Razorpay payment verification not yet implemented for payment $paymentId")
        return false
    }

    // =====================
    // Stripe Integration
    // =====================

    /**
     * Generate Stripe payment link
     * TODO: Implement actual Stripe integration
     */
    private fun generateStripePaymentLink(invoice: Invoice): String {
        logger.warn("Stripe integration not yet implemented for invoice ${invoice.invoiceNumber}")
        // TODO: Implement Stripe invoice creation
        return "https://stripe.com/payment-link-placeholder"
    }

    /**
     * Charge Stripe payment method
     * TODO: Implement actual Stripe integration
     */
    private fun chargeStripePaymentMethod(invoice: Invoice, paymentMethod: PaymentMethod): String {
        logger.warn("Stripe auto-charge not yet implemented for invoice ${invoice.invoiceNumber}")
        throw SubscriptionException.PaymentFailed("Auto-charge not yet implemented")
    }

    /**
     * Verify Stripe payment
     * TODO: Implement actual Stripe integration
     */
    private fun verifyStripePayment(paymentIntentId: String): Boolean {
        logger.warn("Stripe payment verification not yet implemented for payment $paymentIntentId")
        return false
    }
}

/**
 * Data classes for payment provider responses
 */
data class RazorpayInvoice(
    val id: String,
    val shortUrl: String
)

data class RazorpayPayment(
    val id: String,
    val status: String
)

data class StripeInvoice(
    val id: String,
    val hostedInvoiceUrl: String
)

data class StripePaymentIntent(
    val id: String,
    val status: String
)

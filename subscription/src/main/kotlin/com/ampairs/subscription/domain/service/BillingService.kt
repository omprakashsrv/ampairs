package com.ampairs.subscription.domain.service

import com.ampairs.subscription.domain.dto.*
import com.ampairs.subscription.domain.model.*
import com.ampairs.subscription.domain.repository.*
import com.ampairs.subscription.exception.SubscriptionException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

@Service
@Transactional
class BillingService(
    private val paymentTransactionRepository: PaymentTransactionRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val subscriptionRepository: SubscriptionRepository
) {
    private val logger = LoggerFactory.getLogger(BillingService::class.java)

    // =====================
    // Payment Transactions
    // =====================

    /**
     * Record a payment transaction
     */
    fun recordTransaction(
        subscriptionId: String,
        workspaceId: String,
        provider: PaymentProvider,
        externalPaymentId: String?,
        externalInvoiceId: String?,
        amount: BigDecimal,
        currency: String,
        status: PaymentStatus,
        description: String?,
        billingPeriodStart: Instant?,
        billingPeriodEnd: Instant?,
        paymentMethodType: PaymentMethodType?,
        paymentMethodLast4: String?,
        cardBrand: String?,
        receiptUrl: String?
    ): PaymentTransaction {
        val transaction = PaymentTransaction().apply {
            this.subscriptionId = subscriptionId
            this.workspaceId = workspaceId
            this.paymentProvider = provider
            this.externalPaymentId = externalPaymentId
            this.externalInvoiceId = externalInvoiceId
            this.amount = amount
            this.netAmount = amount // Can add tax/discount calculation
            this.currency = currency
            this.status = status
            this.description = description
            this.billingPeriodStart = billingPeriodStart
            this.billingPeriodEnd = billingPeriodEnd
            this.paymentMethodType = paymentMethodType
            this.paymentMethodLast4 = paymentMethodLast4
            this.cardBrand = cardBrand
            this.receiptUrl = receiptUrl
            if (status == PaymentStatus.SUCCEEDED) {
                this.paidAt = Instant.now()
            }
        }

        logger.info(
            "Recording payment transaction for workspace {}: {} {} ({})",
            workspaceId, amount, currency, status
        )
        return paymentTransactionRepository.save(transaction)
    }

    /**
     * Update transaction status
     */
    fun updateTransactionStatus(transactionUid: String, status: PaymentStatus): PaymentTransaction {
        val transaction = paymentTransactionRepository.findByUid(transactionUid)
            ?: throw SubscriptionException.TransactionNotFound(transactionUid)

        transaction.status = status
        if (status == PaymentStatus.SUCCEEDED) {
            transaction.paidAt = Instant.now()
        }

        return paymentTransactionRepository.save(transaction)
    }

    /**
     * Record failed payment
     */
    fun recordFailedPayment(
        transactionUid: String,
        failureCode: String?,
        failureReason: String?
    ): PaymentTransaction {
        val transaction = paymentTransactionRepository.findByUid(transactionUid)
            ?: throw SubscriptionException.TransactionNotFound(transactionUid)

        transaction.apply {
            this.status = PaymentStatus.FAILED
            this.failureCode = failureCode
            this.failureReason = failureReason
        }

        return paymentTransactionRepository.save(transaction)
    }

    /**
     * Record refund
     */
    fun recordRefund(
        transactionUid: String,
        refundAmount: BigDecimal,
        refundReason: String?
    ): PaymentTransaction {
        val transaction = paymentTransactionRepository.findByUid(transactionUid)
            ?: throw SubscriptionException.TransactionNotFound(transactionUid)

        transaction.apply {
            this.status = PaymentStatus.REFUNDED
            this.refundAmount = refundAmount
            this.refundReason = refundReason
            this.refundedAt = Instant.now()
        }

        logger.info("Recorded refund for transaction {}: {} {}", transactionUid, refundAmount, transaction.currency)
        return paymentTransactionRepository.save(transaction)
    }

    /**
     * Get payment history for a workspace
     */
    fun getPaymentHistory(workspaceId: String, page: Int, size: Int): Page<PaymentTransactionResponse> {
        val pageable = PageRequest.of(page, size)
        return paymentTransactionRepository
            .findByWorkspaceIdOrderByCreatedAtDesc(workspaceId, pageable)
            .map { it.asPaymentTransactionResponse() }
    }

    /**
     * Get transaction by external ID
     */
    fun getTransactionByExternalId(externalPaymentId: String): PaymentTransaction? {
        return paymentTransactionRepository.findByExternalPaymentId(externalPaymentId)
    }

    // =====================
    // Payment Methods
    // =====================

    /**
     * Add a payment method
     */
    fun addPaymentMethod(
        workspaceId: String,
        provider: PaymentProvider,
        externalPaymentMethodId: String,
        type: PaymentMethodType,
        last4: String?,
        brand: String?,
        expMonth: Int?,
        expYear: Int?,
        cardholderName: String?,
        upiId: String?,
        bankName: String?,
        billingEmail: String?,
        fingerprint: String?,
        setAsDefault: Boolean = false
    ): PaymentMethodResponse {
        // Check for duplicate
        val existing = paymentMethodRepository.findByExternalPaymentMethodId(externalPaymentMethodId)
        if (existing != null) {
            return existing.asPaymentMethodResponse()
        }

        // If setting as default, clear existing defaults
        if (setAsDefault) {
            paymentMethodRepository.clearDefaultByWorkspaceId(workspaceId, Instant.now())
        }

        // Check if this is the first payment method (make it default)
        val existingMethods = paymentMethodRepository.findActiveByWorkspaceId(workspaceId)
        val isFirst = existingMethods.isEmpty()

        val paymentMethod = PaymentMethod().apply {
            this.workspaceId = workspaceId
            this.paymentProvider = provider
            this.externalPaymentMethodId = externalPaymentMethodId
            this.type = type
            this.last4 = last4
            this.brand = brand
            this.expMonth = expMonth
            this.expYear = expYear
            this.cardholderName = cardholderName
            this.upiId = upiId
            this.bankName = bankName
            this.billingEmail = billingEmail
            this.fingerprint = fingerprint
            this.isDefault = setAsDefault || isFirst
            this.active = true
        }

        logger.info(
            "Added payment method for workspace {}: {} {} (default: {})",
            workspaceId, type, brand ?: "", paymentMethod.isDefault
        )
        return paymentMethodRepository.save(paymentMethod).asPaymentMethodResponse()
    }

    /**
     * Get payment methods for a workspace
     */
    fun getPaymentMethods(workspaceId: String): List<PaymentMethodResponse> {
        return paymentMethodRepository.findActiveByWorkspaceId(workspaceId)
            .asPaymentMethodResponses()
    }

    /**
     * Get default payment method
     */
    fun getDefaultPaymentMethod(workspaceId: String): PaymentMethodResponse? {
        return paymentMethodRepository.findDefaultByWorkspaceId(workspaceId)
            ?.asPaymentMethodResponse()
    }

    /**
     * Set default payment method
     */
    fun setDefaultPaymentMethod(workspaceId: String, paymentMethodUid: String): PaymentMethodResponse {
        val paymentMethod = paymentMethodRepository.findByUid(paymentMethodUid)
            ?: throw SubscriptionException.PaymentMethodNotFound(paymentMethodUid)

        if (paymentMethod.workspaceId != workspaceId) {
            throw SubscriptionException.PaymentMethodNotFound(paymentMethodUid)
        }

        val now = Instant.now()
        paymentMethodRepository.clearDefaultByWorkspaceId(workspaceId, now)
        paymentMethodRepository.setAsDefault(paymentMethodUid, now)

        paymentMethod.isDefault = true
        logger.info("Set default payment method for workspace {}: {}", workspaceId, paymentMethodUid)
        return paymentMethod.asPaymentMethodResponse()
    }

    /**
     * Remove payment method
     */
    fun removePaymentMethod(workspaceId: String, paymentMethodUid: String): Boolean {
        val paymentMethod = paymentMethodRepository.findByUid(paymentMethodUid)
            ?: throw SubscriptionException.PaymentMethodNotFound(paymentMethodUid)

        if (paymentMethod.workspaceId != workspaceId) {
            throw SubscriptionException.PaymentMethodNotFound(paymentMethodUid)
        }

        // Check if it's the default and only method
        if (paymentMethod.isDefault) {
            val otherMethods = paymentMethodRepository.findActiveByWorkspaceId(workspaceId)
                .filter { it.uid != paymentMethodUid }

            if (otherMethods.isNotEmpty()) {
                // Set another method as default
                paymentMethodRepository.setAsDefault(otherMethods.first().uid, Instant.now())
            }
        }

        paymentMethodRepository.deactivate(paymentMethodUid, Instant.now())
        logger.info("Removed payment method {} from workspace {}", paymentMethodUid, workspaceId)
        return true
    }

    /**
     * Get payment method by external ID
     */
    fun getPaymentMethodByExternalId(externalId: String): PaymentMethod? {
        return paymentMethodRepository.findByExternalPaymentMethodId(externalId)
    }
}

package com.ampairs.subscription.provider

import com.ampairs.subscription.domain.dto.VerifyPurchaseRequest
import com.ampairs.subscription.domain.model.BillingCycle
import com.ampairs.subscription.domain.model.PaymentProvider
import com.ampairs.subscription.domain.repository.SubscriptionPlanRepository
import com.ampairs.subscription.domain.repository.SubscriptionRepository
import com.ampairs.subscription.domain.service.*
import com.ampairs.subscription.exception.SubscriptionException
import com.stripe.Stripe
import com.stripe.exception.SignatureVerificationException
import com.stripe.model.Customer
import com.stripe.model.Subscription
import com.stripe.model.checkout.Session
import com.stripe.net.Webhook
import com.stripe.param.CustomerCreateParams
import com.stripe.param.SubscriptionCancelParams
import com.stripe.param.SubscriptionUpdateParams
import com.stripe.param.checkout.SessionCreateParams
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import jakarta.annotation.PostConstruct

/**
 * Stripe integration service.
 * Handles subscription creation and management for international payments.
 */
@Service
class StripeService(
    @Value("\${stripe.secret-key}") private val secretKey: String,
    @Value("\${stripe.webhook-secret}") val webhookSecret: String,
    @Value("\${stripe.success-url}") private val successUrl: String,
    @Value("\${stripe.cancel-url}") private val cancelUrl: String,
    private val subscriptionPlanRepository: SubscriptionPlanRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val workspaceRepository: com.ampairs.workspace.repository.WorkspaceRepository
) : PaymentProviderService {

    private val logger = LoggerFactory.getLogger(StripeService::class.java)

    override val provider = PaymentProvider.STRIPE

    @PostConstruct
    fun init() {
        Stripe.apiKey = secretKey
        logger.info("Stripe service initialized")
    }

    override suspend fun verifyPurchase(request: VerifyPurchaseRequest): PurchaseVerificationResult {
        // Stripe verification happens via webhook, not client-initiated
        throw UnsupportedOperationException("Use webhook for Stripe payment verification")
    }

    override suspend fun createSubscription(
        workspaceId: String,
        planCode: String,
        billingCycle: BillingCycle,
        currency: String,
        customerId: String?
    ): ProviderSubscriptionResult {
        return try {
            logger.debug("Creating Stripe checkout session: workspace={}, plan={}, cycle={}",
                workspaceId, planCode, billingCycle)

            // Get or create Stripe customer
            val stripeCustomer = if (customerId != null) {
                try {
                    Customer.retrieve(customerId)
                } catch (e: Exception) {
                    logger.warn("Failed to retrieve existing customer, creating new one", e)
                    createStripeCustomer(workspaceId)
                }
            } else {
                createStripeCustomer(workspaceId)
            }

            // Get Stripe price ID from our subscription plan
            val plan = subscriptionPlanRepository.findByPlanCode(planCode)
                ?: throw SubscriptionException.PlanNotFoundException(planCode)

            val stripePriceId = when (billingCycle) {
                BillingCycle.MONTHLY -> plan.stripePriceIdMonthly
                BillingCycle.ANNUAL -> plan.stripePriceIdAnnual
                else -> throw IllegalArgumentException("Stripe only supports MONTHLY and ANNUAL cycles, got: $billingCycle")
            } ?: throw IllegalStateException("Stripe price ID not configured for $planCode $billingCycle")

            // Create Checkout Session
            val sessionParams = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(stripeCustomer.id)
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setPrice(stripePriceId)
                        .setQuantity(1L)
                        .build()
                )
                .setSuccessUrl("$successUrl?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .putMetadata("workspace_id", workspaceId)
                .putMetadata("plan_code", planCode)
                .putMetadata("billing_cycle", billingCycle.name)
                .setClientReferenceId(workspaceId)
                .build()

            val session = Session.create(sessionParams)

            logger.info("Stripe checkout session created: id={}", session.id)

            ProviderSubscriptionResult(
                success = true,
                externalSubscriptionId = session.id,
                externalCustomerId = stripeCustomer.id,
                checkoutUrl = session.url,
                clientSecret = null, // Client secret not used for Checkout Sessions
                orderId = null,
                currentPeriodStart = null,
                currentPeriodEnd = null
            )
        } catch (e: Exception) {
            logger.error("Error creating Stripe checkout session", e)
            ProviderSubscriptionResult(
                success = false,
                externalSubscriptionId = null,
                externalCustomerId = null,
                checkoutUrl = null,
                clientSecret = null,
                orderId = null,
                currentPeriodStart = null,
                currentPeriodEnd = null,
                errorMessage = e.message ?: "Unknown error"
            )
        }
    }

    override suspend fun cancelSubscription(
        externalSubscriptionId: String,
        immediate: Boolean
    ): Boolean {
        return try {
            logger.debug("Cancelling Stripe subscription: id={}, immediate={}", externalSubscriptionId, immediate)

            val subscription = Subscription.retrieve(externalSubscriptionId)

            if (immediate) {
                // Cancel immediately
                val params = SubscriptionCancelParams.builder()
                    .setProrate(true)
                    .build()
                subscription.cancel(params)
            } else {
                // Cancel at period end
                val params = SubscriptionUpdateParams.builder()
                    .setCancelAtPeriodEnd(true)
                    .build()
                subscription.update(params)
            }

            logger.info("Stripe subscription cancelled: id={}", externalSubscriptionId)
            true
        } catch (e: Exception) {
            logger.error("Error cancelling Stripe subscription", e)
            false
        }
    }

    override suspend fun pauseSubscription(externalSubscriptionId: String): Boolean {
        return try {
            logger.debug("Pausing Stripe subscription: id={}", externalSubscriptionId)

            val params = SubscriptionUpdateParams.builder()
                .setPauseCollection(
                    SubscriptionUpdateParams.PauseCollection.builder()
                        .setBehavior(SubscriptionUpdateParams.PauseCollection.Behavior.VOID)
                        .build()
                )
                .build()

            val subscription = Subscription.retrieve(externalSubscriptionId)
            subscription.update(params)

            logger.info("Stripe subscription paused: id={}", externalSubscriptionId)
            true
        } catch (e: Exception) {
            logger.error("Error pausing Stripe subscription", e)
            false
        }
    }

    override suspend fun resumeSubscription(externalSubscriptionId: String): Boolean {
        return try {
            logger.debug("Resuming Stripe subscription: id={}", externalSubscriptionId)

            val params = SubscriptionUpdateParams.builder()
                .setPauseCollection(SubscriptionUpdateParams.PauseCollection.builder().build())
                .build()

            val subscription = Subscription.retrieve(externalSubscriptionId)
            subscription.update(params)

            logger.info("Stripe subscription resumed: id={}", externalSubscriptionId)
            true
        } catch (e: Exception) {
            logger.error("Error resuming Stripe subscription", e)
            false
        }
    }

    override suspend fun changePlan(
        externalSubscriptionId: String,
        newPlanCode: String,
        billingCycle: BillingCycle,
        immediate: Boolean
    ): ProviderSubscriptionResult {
        return try {
            logger.debug("Changing Stripe subscription plan: id={}, newPlan={}", externalSubscriptionId, newPlanCode)

            // Get new Stripe price ID
            val plan = subscriptionPlanRepository.findByPlanCode(newPlanCode)
                ?: throw SubscriptionException.PlanNotFoundException(newPlanCode)

            val newStripePriceId = when (billingCycle) {
                BillingCycle.MONTHLY -> plan.stripePriceIdMonthly
                BillingCycle.ANNUAL -> plan.stripePriceIdAnnual
                else -> throw IllegalArgumentException("Unsupported billing cycle: $billingCycle")
            } ?: throw IllegalStateException("Stripe price ID not configured")

            val subscription = Subscription.retrieve(externalSubscriptionId)
            val currentItem = subscription.items.data.first()

            // Update subscription items
            val params = SubscriptionUpdateParams.builder()
                .addItem(
                    SubscriptionUpdateParams.Item.builder()
                        .setId(currentItem.id)
                        .setPrice(newStripePriceId)
                        .build()
                )
                .setProrationBehavior(
                    if (immediate) {
                        SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS
                    } else {
                        SubscriptionUpdateParams.ProrationBehavior.NONE
                    }
                )
                .build()

            val updated = subscription.update(params)

            logger.info("Stripe subscription plan changed: id={}", externalSubscriptionId)

            ProviderSubscriptionResult(
                success = true,
                externalSubscriptionId = externalSubscriptionId,
                externalCustomerId = updated.customer,
                checkoutUrl = null,
                clientSecret = null,
                orderId = null,
                currentPeriodStart = Instant.ofEpochSecond(updated.currentPeriodStart),
                currentPeriodEnd = Instant.ofEpochSecond(updated.currentPeriodEnd)
            )
        } catch (e: Exception) {
            logger.error("Error changing Stripe subscription plan", e)
            ProviderSubscriptionResult(
                success = false,
                externalSubscriptionId = null,
                externalCustomerId = null,
                checkoutUrl = null,
                clientSecret = null,
                orderId = null,
                currentPeriodStart = null,
                currentPeriodEnd = null,
                errorMessage = e.message
            )
        }
    }

    override suspend fun getSubscriptionStatus(externalSubscriptionId: String): ProviderSubscriptionStatus {
        return try {
            val subscription = Subscription.retrieve(externalSubscriptionId)

            val active = subscription.status in listOf("active", "trialing")
            val currentPeriodStart = Instant.ofEpochSecond(subscription.currentPeriodStart)
            val currentPeriodEnd = Instant.ofEpochSecond(subscription.currentPeriodEnd)
            val cancelAtPeriodEnd = subscription.cancelAtPeriodEnd ?: false
            val cancelledAt = subscription.canceledAt?.let { Instant.ofEpochSecond(it) }

            ProviderSubscriptionStatus(
                active = active,
                status = subscription.status,
                currentPeriodStart = currentPeriodStart,
                currentPeriodEnd = currentPeriodEnd,
                cancelAtPeriodEnd = cancelAtPeriodEnd,
                cancelledAt = cancelledAt
            )
        } catch (e: Exception) {
            logger.error("Error getting Stripe subscription status", e)
            throw SubscriptionException.ProviderError("STRIPE", e.message ?: "Unknown error")
        }
    }

    override fun verifyWebhookSignature(payload: String, signature: String): Boolean {
        return try {
            Webhook.constructEvent(payload, signature, webhookSecret)
            true
        } catch (e: SignatureVerificationException) {
            logger.warn("Stripe webhook signature verification failed", e)
            false
        } catch (e: Exception) {
            logger.error("Error verifying Stripe webhook signature", e)
            false
        }
    }

    /**
     * Create Stripe customer for a workspace
     */
    private fun createStripeCustomer(workspaceId: String): Customer {
        val workspace = workspaceRepository.findByUid(workspaceId).orElseThrow {
            SubscriptionException.SubscriptionNotFoundException(workspaceId)
        }

        val params = CustomerCreateParams.builder()
            .setName(workspace.name)
            .setEmail(workspace.email ?: "noreply@ampairs.com")
            .setPhone(workspace.phone)
            .putMetadata("workspace_id", workspaceId)
            .build()

        return Customer.create(params)
    }
}

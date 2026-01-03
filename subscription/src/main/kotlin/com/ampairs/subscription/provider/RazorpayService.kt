package com.ampairs.subscription.provider

import com.ampairs.subscription.domain.dto.VerifyPurchaseRequest
import com.ampairs.subscription.domain.model.BillingCycle
import com.ampairs.subscription.domain.model.PaymentProvider
import com.ampairs.subscription.domain.repository.SubscriptionPlanRepository
import com.ampairs.subscription.domain.repository.SubscriptionRepository
import com.ampairs.subscription.domain.service.PaymentProviderService
import com.ampairs.subscription.domain.service.ProviderSubscriptionResult
import com.ampairs.subscription.domain.service.ProviderSubscriptionStatus
import com.ampairs.subscription.domain.service.PurchaseVerificationResult
import com.ampairs.subscription.exception.SubscriptionException
import com.razorpay.RazorpayClient
import com.razorpay.RazorpayException
import org.apache.commons.codec.digest.HmacUtils
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Razorpay integration service.
 * Handles subscription creation and management for Indian market.
 */
@Service
class RazorpayService(
    @Value("\${razorpay.key-id}") private val keyId: String,
    @Value("\${razorpay.key-secret}") private val keySecret: String,
    @Value("\${razorpay.webhook-secret}") private val webhookSecret: String,
    private val subscriptionPlanRepository: SubscriptionPlanRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val workspaceRepository: com.ampairs.workspace.repository.WorkspaceRepository
) : PaymentProviderService {

    private val logger = LoggerFactory.getLogger(RazorpayService::class.java)

    override val provider = PaymentProvider.RAZORPAY

    private val razorpayClient: RazorpayClient by lazy {
        RazorpayClient(keyId, keySecret)
    }

    override suspend fun verifyPurchase(request: VerifyPurchaseRequest): PurchaseVerificationResult {
        // Razorpay verification happens via webhook, not client-initiated
        throw UnsupportedOperationException("Use webhook for Razorpay payment verification")
    }

    override suspend fun createSubscription(
        workspaceId: String,
        planCode: String,
        billingCycle: BillingCycle,
        currency: String,
        customerId: String?
    ): ProviderSubscriptionResult {
        return try {
            logger.debug(
                "Creating Razorpay subscription: workspace={}, plan={}, cycle={}",
                workspaceId, planCode, billingCycle
            )

            // Get or create Razorpay customer
            val rzpCustomer: JSONObject = if (customerId != null) {
                try {
                    razorpayClient.customers.fetch(customerId).toJson()
                } catch (e: RazorpayException) {
                    logger.warn("Failed to fetch existing customer, creating new one", e)
                    createRazorpayCustomer(workspaceId)
                }
            } else {
                createRazorpayCustomer(workspaceId)
            }

            // Get Razorpay plan ID from our subscription plan
            val plan = subscriptionPlanRepository.findByPlanCode(planCode)
                ?: throw SubscriptionException.PlanNotFoundException(planCode)

            val razorpayPlanId = when (billingCycle) {
                BillingCycle.MONTHLY -> plan.razorpayPlanIdMonthly
                BillingCycle.ANNUAL -> plan.razorpayPlanIdAnnual
                else -> throw IllegalArgumentException("Razorpay only supports MONTHLY and ANNUAL cycles, got: $billingCycle")
            } ?: throw IllegalStateException("Razorpay plan ID not configured for $planCode $billingCycle")

            // Create Razorpay subscription
            // rzpCustomer is already JSONObject from line 63
            val customerId = rzpCustomer.getString("id")
            val customerEmail = rzpCustomer.optString("email", "noreply@ampairs.com")

            val subscriptionRequest = JSONObject().apply {
                put("plan_id", razorpayPlanId)
                put("customer_id", customerId)
                put("total_count", 120) // 10 years worth (monthly) or 10 cycles (annual)
                put("quantity", 1)
                put("customer_notify", 1) // Send email to customer
                put("notify_info", JSONObject().apply {
                    put("notify_email", customerEmail)
                })
            }

            val rzpSubscription = razorpayClient.subscriptions.create(subscriptionRequest)
            val subscriptionJson = rzpSubscription.toJson()

            val externalSubscriptionId = subscriptionJson.getString("id")
            val shortUrl = if (subscriptionJson.has("short_url")) subscriptionJson.getString("short_url") else null
            val status = subscriptionJson.getString("status")

            logger.info("Razorpay subscription created: id={}, status={}", externalSubscriptionId, status)

            ProviderSubscriptionResult(
                success = true,
                externalSubscriptionId = externalSubscriptionId,
                externalCustomerId = customerId,
                checkoutUrl = shortUrl?.takeIf { !it.isEmpty() }, // Hosted checkout page
                clientSecret = null,
                orderId = null,
                currentPeriodStart = Instant.now(),
                currentPeriodEnd = null // Will be set after first payment
            )
        } catch (e: Exception) {
            logger.error("Error creating Razorpay subscription", e)
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
            logger.debug("Cancelling Razorpay subscription: id={}, immediate={}", externalSubscriptionId, immediate)

            val cancelRequest = JSONObject().apply {
                put("cancel_at_cycle_end", !immediate)
            }

            razorpayClient.subscriptions.cancel(externalSubscriptionId, cancelRequest)

            logger.info("Razorpay subscription cancelled: id={}", externalSubscriptionId)
            true
        } catch (e: Exception) {
            logger.error("Error cancelling Razorpay subscription", e)
            false
        }
    }

    override suspend fun pauseSubscription(externalSubscriptionId: String): Boolean {
        return try {
            logger.debug("Pausing Razorpay subscription: id={}", externalSubscriptionId)

            val pauseRequest = JSONObject().apply {
                put("pause_at", "now")
            }

            razorpayClient.subscriptions.update(externalSubscriptionId, pauseRequest)

            logger.info("Razorpay subscription paused: id={}", externalSubscriptionId)
            true
        } catch (e: Exception) {
            logger.error("Error pausing Razorpay subscription", e)
            false
        }
    }

    override suspend fun resumeSubscription(externalSubscriptionId: String): Boolean {
        return try {
            logger.debug("Resuming Razorpay subscription: id={}", externalSubscriptionId)

            val resumeRequest = JSONObject().apply {
                put("resume_at", "now")
            }

            razorpayClient.subscriptions.update(externalSubscriptionId, resumeRequest)

            logger.info("Razorpay subscription resumed: id={}", externalSubscriptionId)
            true
        } catch (e: Exception) {
            logger.error("Error resuming Razorpay subscription", e)
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
            logger.debug("Changing Razorpay subscription plan: id={}, newPlan={}", externalSubscriptionId, newPlanCode)

            // Get new Razorpay plan ID
            val plan = subscriptionPlanRepository.findByPlanCode(newPlanCode)
                ?: throw SubscriptionException.PlanNotFoundException(newPlanCode)

            val newRazorpayPlanId = when (billingCycle) {
                BillingCycle.MONTHLY -> plan.razorpayPlanIdMonthly
                BillingCycle.ANNUAL -> plan.razorpayPlanIdAnnual
                else -> throw IllegalArgumentException("Unsupported billing cycle: $billingCycle")
            } ?: throw IllegalStateException("Razorpay plan ID not configured")

            // Update subscription
            val updateRequest = JSONObject().apply {
                put("plan_id", newRazorpayPlanId)
                put("quantity", 1)
                if (immediate) {
                    put("schedule_change_at", "now")
                } else {
                    put("schedule_change_at", "cycle_end")
                }
            }

            val updatedSubscription = razorpayClient.subscriptions.update(externalSubscriptionId, updateRequest)

            logger.info("Razorpay subscription plan changed: id={}", externalSubscriptionId)

            ProviderSubscriptionResult(
                success = true,
                externalSubscriptionId = externalSubscriptionId,
                externalCustomerId = updatedSubscription.get("customer_id") as String?,
                checkoutUrl = null,
                clientSecret = null,
                orderId = null,
                currentPeriodStart = null,
                currentPeriodEnd = null
            )
        } catch (e: Exception) {
            logger.error("Error changing Razorpay subscription plan", e)
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
            val subscription = razorpayClient.subscriptions.fetch(externalSubscriptionId)
            val subscriptionJson = subscription.toJson()

            val status = subscriptionJson.getString("status")
            val active = status in listOf("active", "authenticated", "pending")
            val currentStart = if (subscriptionJson.has("current_start")) {
                Instant.ofEpochSecond(subscriptionJson.getLong("current_start"))
            } else null
            val currentEnd = if (subscriptionJson.has("current_end")) {
                Instant.ofEpochSecond(subscriptionJson.getLong("current_end"))
            } else null
            val cancelAtCycleEnd = subscriptionJson.optBoolean("cancel_at_cycle_end", false)
            val cancelledAt = if (subscriptionJson.has("cancelled_at")) {
                Instant.ofEpochSecond(subscriptionJson.getLong("cancelled_at"))
            } else null

            ProviderSubscriptionStatus(
                active = active,
                status = status,
                currentPeriodStart = currentStart,
                currentPeriodEnd = currentEnd,
                cancelAtPeriodEnd = cancelAtCycleEnd,
                cancelledAt = cancelledAt
            )
        } catch (e: Exception) {
            logger.error("Error getting Razorpay subscription status", e)
            throw SubscriptionException.ProviderError("RAZORPAY", e.message ?: "Unknown error")
        }
    }

    override fun verifyWebhookSignature(payload: String, signature: String): Boolean {
        return try {
            val expectedSignature = HmacUtils.hmacSha256Hex(webhookSecret, payload)
            val valid = expectedSignature.equals(signature, ignoreCase = true)

            if (!valid) {
                logger.warn("Razorpay webhook signature verification failed")
            }

            valid
        } catch (e: Exception) {
            logger.error("Error verifying Razorpay webhook signature", e)
            false
        }
    }

    /**
     * Create Razorpay customer for a workspace
     */
    private fun createRazorpayCustomer(workspaceId: String): JSONObject {
        val workspace = workspaceRepository.findByUid(workspaceId).orElseThrow {
            SubscriptionException.SubscriptionNotFoundException(workspaceId)
        }

        val customerRequest = JSONObject().apply {
            put("name", workspace.name)
            put("email", workspace.email ?: "noreply@ampairs.com")
            put("contact", workspace.phone ?: "")
            put("notes", JSONObject().apply {
                put("workspace_id", workspaceId)
            })
        }

        val customer = razorpayClient.customers.create(customerRequest)
        return customer.toJson()
    }
}

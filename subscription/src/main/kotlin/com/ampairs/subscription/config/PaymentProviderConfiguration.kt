package com.ampairs.subscription.config

import com.ampairs.subscription.domain.service.PaymentOrchestrationService
import com.ampairs.subscription.provider.AppleAppStoreService
import com.ampairs.subscription.provider.GooglePlayBillingService
import com.ampairs.subscription.provider.RazorpayService
import com.ampairs.subscription.provider.StripeService
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

/**
 * Registers payment provider services with the orchestration service on startup.
 *
 * A provider is only registered when its credentials are actually configured. Providers left on
 * the PLACEHOLDER_* defaults (or blank) are skipped, so any attempt to use them fails immediately
 * with "Provider not configured" (PaymentOrchestrationService.getProviderService) instead of
 * silently failing at charge time with an invalid-credential error from the provider.
 */
@Configuration
class PaymentProviderConfiguration(
    private val orchestrationService: PaymentOrchestrationService,
    private val googlePlayService: GooglePlayBillingService,
    private val appleService: AppleAppStoreService,
    private val razorpayService: RazorpayService,
    private val stripeService: StripeService,
    private val environment: Environment,
) {
    private val logger = LoggerFactory.getLogger(PaymentProviderConfiguration::class.java)

    @PostConstruct
    fun registerProviders() {
        logger.info("Registering payment provider services...")

        registerIfConfigured("Google Play Billing", "google-play.service-account-json-path") {
            orchestrationService.registerProvider(googlePlayService)
        }
        registerIfConfigured("Apple App Store", "apple-app-store.shared-secret") {
            orchestrationService.registerProvider(appleService)
        }
        registerIfConfigured("Razorpay", "razorpay.key-secret", "razorpay.key-id") {
            orchestrationService.registerProvider(razorpayService)
        }
        registerIfConfigured("Stripe", "stripe.secret-key") {
            orchestrationService.registerProvider(stripeService)
        }
    }

    private fun registerIfConfigured(name: String, vararg requiredProperties: String, register: () -> Unit) {
        val unconfigured = requiredProperties.filter { property ->
            val value = environment.getProperty(property)
            value.isNullOrBlank() || value.contains("PLACEHOLDER", ignoreCase = true)
        }
        if (unconfigured.isEmpty()) {
            register()
            logger.info("Registered {} service", name)
        } else {
            val message = "$name NOT registered — unconfigured/placeholder properties: $unconfigured. " +
                    "Payments via this provider will fail with 'Provider not configured'."
            if (environment.activeProfiles.contains("production")) {
                logger.error(message)
            } else {
                logger.warn(message)
            }
        }
    }
}

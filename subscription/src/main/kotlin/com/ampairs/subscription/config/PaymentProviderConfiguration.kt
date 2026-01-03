package com.ampairs.subscription.config

import com.ampairs.subscription.domain.service.PaymentOrchestrationService
import com.ampairs.subscription.provider.AppleAppStoreService
import com.ampairs.subscription.provider.GooglePlayBillingService
import com.ampairs.subscription.provider.RazorpayService
import com.ampairs.subscription.provider.StripeService
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration

/**
 * Configuration for registering payment provider services with the orchestration service.
 * All payment provider services are automatically registered on application startup.
 */
@Configuration
class PaymentProviderConfiguration(
    private val orchestrationService: PaymentOrchestrationService,
    private val googlePlayService: GooglePlayBillingService,
    private val appleService: AppleAppStoreService,
    private val razorpayService: RazorpayService,
    private val stripeService: StripeService
) {
    private val logger = LoggerFactory.getLogger(PaymentProviderConfiguration::class.java)

    @PostConstruct
    fun registerProviders() {
        logger.info("Registering payment provider services...")

        orchestrationService.registerProvider(googlePlayService)
        logger.info("Registered Google Play Billing service")

        orchestrationService.registerProvider(appleService)
        logger.info("Registered Apple App Store service")

        orchestrationService.registerProvider(razorpayService)
        logger.info("Registered Razorpay service")

        orchestrationService.registerProvider(stripeService)
        logger.info("Registered Stripe service")

        logger.info("All payment provider services registered successfully")
    }
}

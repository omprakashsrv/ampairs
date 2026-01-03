package com.ampairs.subscription.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration properties for subscription module
 */
@Configuration
@ConfigurationProperties(prefix = "ampairs.subscription")
class SubscriptionConfig {

    /**
     * Google Play Billing configuration
     */
    var googlePlay = GooglePlayConfig()

    /**
     * Apple App Store configuration
     */
    var appStore = AppStoreConfig()

    /**
     * Razorpay configuration
     */
    var razorpay = RazorpayConfig()

    /**
     * Stripe configuration
     */
    var stripe = StripeConfig()

    /**
     * Device registration settings
     */
    var device = DeviceConfig()

    /**
     * Trial settings
     */
    var trial = TrialConfig()

    class GooglePlayConfig {
        /** Google Cloud service account credentials JSON */
        var serviceAccountJson: String = ""

        /** Package name for the Android app */
        var packageName: String = "com.ampairs.app"

        /** Enable purchase verification */
        var enabled: Boolean = false
    }

    class AppStoreConfig {
        /** App Store Connect API Key ID */
        var keyId: String = ""

        /** App Store Connect Issuer ID */
        var issuerId: String = ""

        /** Private key for API authentication */
        var privateKey: String = ""

        /** Bundle ID for the iOS app */
        var bundleId: String = "com.ampairs.app"

        /** Shared secret for receipt validation */
        var sharedSecret: String = ""

        /** Enable purchase verification */
        var enabled: Boolean = false

        /** Use sandbox environment */
        var sandbox: Boolean = true
    }

    class RazorpayConfig {
        /** Razorpay API Key ID */
        var keyId: String = ""

        /** Razorpay API Key Secret */
        var keySecret: String = ""

        /** Webhook secret for signature verification */
        var webhookSecret: String = ""

        /** Enable Razorpay integration */
        var enabled: Boolean = false
    }

    class StripeConfig {
        /** Stripe API Secret Key */
        var secretKey: String = ""

        /** Stripe Publishable Key (for frontend) */
        var publishableKey: String = ""

        /** Webhook signing secret */
        var webhookSecret: String = ""

        /** Enable Stripe integration */
        var enabled: Boolean = false
    }

    class DeviceConfig {
        /** Token validity period in days */
        var tokenValidityDays: Long = 7

        /** Grace period after token expiry in days */
        var gracePeriodDays: Long = 3

        /** Maximum days without sync before lock */
        var maxOfflineDays: Long = 14
    }

    class TrialConfig {
        /** Default trial duration in days */
        var defaultTrialDays: Int = 14

        /** Allow multiple trials per workspace */
        var allowMultipleTrials: Boolean = false

        /** Trial available plans */
        var availablePlans: List<String> = listOf("STARTER", "PROFESSIONAL")
    }
}

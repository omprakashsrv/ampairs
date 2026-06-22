package com.ampairs.subscription

import com.ampairs.subscription.config.SubscriptionConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SubscriptionConfigTest {

    @Test
    fun `defaults are sensible`() {
        val config = SubscriptionConfig()

        assertEquals("com.ampairs.app", config.googlePlay.packageName)
        assertFalse(config.googlePlay.enabled)

        assertEquals("com.ampairs.app", config.appStore.bundleId)
        assertTrue(config.appStore.sandbox)
        assertFalse(config.appStore.enabled)

        assertFalse(config.razorpay.enabled)
        assertFalse(config.stripe.enabled)

        assertEquals(7L, config.device.tokenValidityDays)
        assertEquals(3L, config.device.gracePeriodDays)
        assertEquals(14L, config.device.maxOfflineDays)

        assertEquals(14, config.trial.defaultTrialDays)
        assertFalse(config.trial.allowMultipleTrials)
        assertEquals(listOf("STARTER", "PROFESSIONAL"), config.trial.availablePlans)
    }

    @Test
    fun `properties are mutable`() {
        val config = SubscriptionConfig()
        config.googlePlay.serviceAccountJson = "{}"
        config.googlePlay.packageName = "com.test"
        config.googlePlay.enabled = true
        config.appStore.keyId = "K1"
        config.appStore.issuerId = "I1"
        config.appStore.privateKey = "PK"
        config.appStore.sharedSecret = "SS"
        config.razorpay.keyId = "rzp"
        config.razorpay.keySecret = "sec"
        config.razorpay.webhookSecret = "wh"
        config.stripe.secretKey = "sk"
        config.stripe.publishableKey = "pk"
        config.stripe.webhookSecret = "whsec"
        config.device.tokenValidityDays = 30
        config.trial.defaultTrialDays = 30
        config.trial.allowMultipleTrials = true
        config.trial.availablePlans = listOf("PRO")

        assertEquals("com.test", config.googlePlay.packageName)
        assertTrue(config.googlePlay.enabled)
        assertEquals("K1", config.appStore.keyId)
        assertEquals("rzp", config.razorpay.keyId)
        assertEquals("sk", config.stripe.secretKey)
        assertEquals(30L, config.device.tokenValidityDays)
        assertEquals(30, config.trial.defaultTrialDays)
        assertTrue(config.trial.allowMultipleTrials)
        assertEquals(listOf("PRO"), config.trial.availablePlans)
    }
}

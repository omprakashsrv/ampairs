package com.ampairs.subscription

import com.ampairs.subscription.controller.WebhookController
import com.ampairs.subscription.domain.model.PaymentProvider
import com.ampairs.subscription.domain.model.WebhookLog
import com.ampairs.subscription.domain.model.WebhookStatus
import com.ampairs.subscription.domain.service.WebhookIdempotencyService
import com.ampairs.subscription.webhook.AppStoreWebhookHandler
import com.ampairs.subscription.webhook.GooglePlayWebhookHandler
import com.ampairs.subscription.webhook.RazorpayWebhookHandler
import com.ampairs.subscription.webhook.StripeWebhookHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.util.Base64

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebhookControllerTest {

    @Mock private lateinit var googlePlay: GooglePlayWebhookHandler
    @Mock private lateinit var appStore: AppStoreWebhookHandler
    @Mock private lateinit var razorpay: RazorpayWebhookHandler
    @Mock private lateinit var stripe: StripeWebhookHandler
    @Mock private lateinit var idempotency: WebhookIdempotencyService

    private val objectMapper = JsonMapper.builder().build()
    private lateinit var controller: WebhookController

    @BeforeEach
    fun setUp() {
        controller = WebhookController(googlePlay, appStore, razorpay, stripe, idempotency, objectMapper)
        whenever(idempotency.logWebhook(any(), any(), org.mockito.kotlin.anyOrNull(), org.mockito.kotlin.anyOrNull()))
            .thenReturn(WebhookLog().apply { uid = "WL-1" })
    }

    @Test
    fun `razorpay webhook processes valid payload`() {
        whenever(razorpay.verifySignature(any(), any(), org.mockito.kotlin.anyOrNull())).thenReturn(true)
        whenever(idempotency.isProcessed(eq(PaymentProvider.RAZORPAY), any())).thenReturn(false)
        val payload = """{"event":"subscription.charged","id":"evt_1","payload":{"subscription":{"entity":{"id":"sub_1"}}}}"""

        val resp = controller.handleRazorpayWebhook(payload, "sig")

        assertEquals(200, resp.statusCode.value())
        verify(razorpay).processEvent(eq("subscription.charged"), any())
        verify(idempotency).markAsProcessed(
            eq(PaymentProvider.RAZORPAY), eq("evt_1"), any(),
            org.mockito.kotlin.anyOrNull(), org.mockito.kotlin.anyOrNull(), org.mockito.kotlin.anyOrNull()
        )
    }

    @Test
    fun `razorpay webhook rejects invalid signature`() {
        whenever(razorpay.verifySignature(any(), any(), org.mockito.kotlin.anyOrNull())).thenReturn(false)
        val resp = controller.handleRazorpayWebhook("""{"event":"x","id":"e"}""", "badsig")

        assertEquals(401, resp.statusCode.value())
        verify(idempotency).updateWebhookStatus(any(), eq(WebhookStatus.SIGNATURE_FAILED), org.mockito.kotlin.anyOrNull())
        verify(razorpay, never()).processEvent(any(), any())
    }

    @Test
    fun `razorpay webhook ignores duplicate`() {
        whenever(razorpay.verifySignature(any(), any(), org.mockito.kotlin.anyOrNull())).thenReturn(true)
        whenever(idempotency.isProcessed(eq(PaymentProvider.RAZORPAY), any())).thenReturn(true)

        val resp = controller.handleRazorpayWebhook("""{"event":"x","id":"e"}""", "sig")

        assertEquals(200, resp.statusCode.value())
        verify(razorpay, never()).processEvent(any(), any())
    }

    @Test
    fun `razorpay webhook returns 200 even on processing error`() {
        whenever(razorpay.verifySignature(any(), any(), org.mockito.kotlin.anyOrNull())).thenReturn(true)
        whenever(idempotency.isProcessed(eq(PaymentProvider.RAZORPAY), any())).thenReturn(false)
        whenever(razorpay.processEvent(any(), any())).thenThrow(RuntimeException("boom"))

        val resp = controller.handleRazorpayWebhook("""{"event":"x","id":"e","payload":{}}""", "sig")

        assertEquals(200, resp.statusCode.value())
        verify(idempotency).updateWebhookStatus(any(), eq(WebhookStatus.FAILED), any())
    }

    @Test
    fun `stripe webhook processes valid payload`() {
        whenever(stripe.verifySignature(any(), any(), org.mockito.kotlin.anyOrNull())).thenReturn(true)
        whenever(idempotency.isProcessed(eq(PaymentProvider.STRIPE), any())).thenReturn(false)
        val payload = """{"type":"invoice.paid","id":"evt_2","data":{"object":{"subscription":"sub_2"}}}"""

        val resp = controller.handleStripeWebhook(payload, "sig")

        assertEquals(200, resp.statusCode.value())
        verify(stripe).processEvent(eq("invoice.paid"), any())
    }

    @Test
    fun `stripe webhook rejects invalid signature`() {
        whenever(stripe.verifySignature(any(), any(), org.mockito.kotlin.anyOrNull())).thenReturn(false)
        val resp = controller.handleStripeWebhook("""{"type":"x","id":"e"}""", "badsig")
        assertEquals(401, resp.statusCode.value())
    }

    @Test
    fun `stripe webhook ignores duplicate`() {
        whenever(stripe.verifySignature(any(), any(), org.mockito.kotlin.anyOrNull())).thenReturn(true)
        whenever(idempotency.isProcessed(eq(PaymentProvider.STRIPE), any())).thenReturn(true)
        val resp = controller.handleStripeWebhook("""{"type":"x","id":"e"}""", "sig")
        assertEquals(200, resp.statusCode.value())
        verify(stripe, never()).processEvent(any(), any())
    }

    @Test
    fun `google play webhook processes valid pubsub message`() {
        whenever(idempotency.isProcessed(eq(PaymentProvider.GOOGLE_PLAY), any())).thenReturn(false)
        val inner = """{"subscriptionNotification":{"subscriptionId":"sub_g","purchaseToken":"tok"}}"""
        val encoded = Base64.getEncoder().encodeToString(inner.toByteArray())
        val payload = """{"message":{"messageId":"m1","data":"$encoded"}}"""

        val resp = controller.handleGooglePlayWebhook(payload)

        assertEquals(200, resp.statusCode.value())
        verify(googlePlay).processEvent(eq("notification"), any())
    }

    @Test
    fun `google play webhook returns 200 on malformed payload`() {
        val resp = controller.handleGooglePlayWebhook("not-json")
        assertEquals(200, resp.statusCode.value())
        verify(idempotency).updateWebhookStatus(any(), eq(WebhookStatus.FAILED), any())
    }

    @Test
    fun `app store webhook returns 200 on malformed payload`() {
        val resp = controller.handleAppStoreWebhook("not-json")
        assertEquals(200, resp.statusCode.value())
    }

    @Test
    fun `test webhook endpoint echoes ok`() {
        val resp = controller.handleTestWebhook("razorpay", "payload")
        assertEquals(200, resp.statusCode.value())
        assertEquals("OK", resp.body)
    }
}

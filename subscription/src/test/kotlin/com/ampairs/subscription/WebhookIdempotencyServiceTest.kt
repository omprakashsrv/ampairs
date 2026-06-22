package com.ampairs.subscription

import com.ampairs.subscription.domain.model.PaymentProvider
import com.ampairs.subscription.domain.model.WebhookEvent
import com.ampairs.subscription.domain.model.WebhookLog
import com.ampairs.subscription.domain.model.WebhookStatus
import com.ampairs.subscription.domain.repository.WebhookEventRepository
import com.ampairs.subscription.domain.repository.WebhookLogRepository
import com.ampairs.subscription.domain.service.WebhookIdempotencyService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.time.Instant

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebhookIdempotencyServiceTest {

    @Mock
    private lateinit var webhookEventRepository: WebhookEventRepository

    @Mock
    private lateinit var webhookLogRepository: WebhookLogRepository

    private lateinit var service: WebhookIdempotencyService

    @BeforeEach
    fun setUp() {
        service = WebhookIdempotencyService(webhookEventRepository, webhookLogRepository)
        whenever(webhookEventRepository.save(any<WebhookEvent>())).thenAnswer { it.arguments[0] }
        whenever(webhookLogRepository.save(any<WebhookLog>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `isProcessed delegates to repository`() {
        whenever(webhookEventRepository.existsByProviderAndEventId(PaymentProvider.STRIPE, "evt_1")).thenReturn(true)

        assertTrue(service.isProcessed(PaymentProvider.STRIPE, "evt_1"))
    }

    @Test
    fun `markAsProcessed returns existing when already present`() {
        val existing = WebhookEvent().apply { provider = PaymentProvider.STRIPE; eventId = "evt_1" }
        whenever(webhookEventRepository.findByProviderAndEventId(PaymentProvider.STRIPE, "evt_1")).thenReturn(existing)

        val result = service.markAsProcessed(PaymentProvider.STRIPE, "evt_1", "payment.succeeded")

        assertSame(existing, result)
    }

    @Test
    fun `markAsProcessed creates new event when not present`() {
        whenever(webhookEventRepository.findByProviderAndEventId(PaymentProvider.RAZORPAY, "evt_new")).thenReturn(null)

        val result = service.markAsProcessed(
            provider = PaymentProvider.RAZORPAY,
            eventId = "evt_new",
            eventType = "subscription.charged",
            payload = "{}",
            externalSubscriptionId = "sub_1",
            workspaceId = "WS-1"
        )

        assertEquals(PaymentProvider.RAZORPAY, result.provider)
        assertEquals("evt_new", result.eventId)
        assertEquals("subscription.charged", result.eventType)
        assertEquals("WS-1", result.workspaceId)
        assertEquals("sub_1", result.externalSubscriptionId)
        assertNotNull(result.processedAt)
    }

    @Test
    fun `logWebhook stores a RECEIVED log`() {
        val log = service.logWebhook(PaymentProvider.STRIPE, "{payload}", "sig", "headers")

        assertEquals(WebhookStatus.RECEIVED, log.status)
        assertEquals("{payload}", log.payload)
        assertEquals("sig", log.signature)
        assertEquals("headers", log.headers)
        assertNotNull(log.receivedAt)
    }

    @Test
    fun `updateWebhookStatus to PROCESSED sets processedAt and clears error`() {
        val log = WebhookLog().apply { status = WebhookStatus.RECEIVED }

        val result = service.updateWebhookStatus(log, WebhookStatus.PROCESSED)

        assertEquals(WebhookStatus.PROCESSED, result.status)
        assertNotNull(result.processedAt)
        assertNull(result.errorMessage)
    }

    @Test
    fun `updateWebhookStatus to FAILED increments retry and schedules next retry`() {
        val log = WebhookLog().apply { status = WebhookStatus.RECEIVED; retryCount = 0 }

        val result = service.updateWebhookStatus(log, WebhookStatus.FAILED, "boom")

        assertEquals(WebhookStatus.FAILED, result.status)
        assertEquals("boom", result.errorMessage)
        assertEquals(1, result.retryCount)
        assertNotNull(result.nextRetryAt)
    }

    @Test
    fun `updateWebhookStatus to RETRY_SCHEDULED schedules next retry`() {
        val log = WebhookLog().apply { retryCount = 4 }

        val result = service.updateWebhookStatus(log, WebhookStatus.RETRY_SCHEDULED)

        assertEquals(5, result.retryCount)
        assertNotNull(result.nextRetryAt)
    }

    @Test
    fun `updateWebhookStatus to other status does not change retry`() {
        val log = WebhookLog().apply { retryCount = 2 }

        val result = service.updateWebhookStatus(log, WebhookStatus.SIGNATURE_FAILED)

        assertEquals(WebhookStatus.SIGNATURE_FAILED, result.status)
        assertEquals(2, result.retryCount)
        assertNull(result.processedAt)
    }

    @Test
    fun `getFailedWebhooksForRetry delegates to repository`() {
        val logs = listOf(WebhookLog())
        whenever(webhookLogRepository.findFailedWebhooksForRetry(org.mockito.kotlin.eq(5), any())).thenReturn(logs)

        assertEquals(1, service.getFailedWebhooksForRetry().size)
    }

    @Test
    fun `cleanupOldWebhooks deletes events and logs before cutoff`() {
        whenever(webhookEventRepository.deleteByProcessedAtBefore(any())).thenReturn(3)
        whenever(webhookLogRepository.deleteByReceivedAtBefore(any())).thenReturn(7)

        // No exception; just exercises the path
        service.cleanupOldWebhooks(30)
    }

    @Test
    fun `getWebhookStats reports counts per status`() {
        whenever(webhookLogRepository.count()).thenReturn(10L)
        whenever(webhookLogRepository.countByStatus(WebhookStatus.PROCESSED)).thenReturn(6L)
        whenever(webhookLogRepository.countByStatus(WebhookStatus.FAILED)).thenReturn(2L)
        whenever(webhookLogRepository.countByStatus(WebhookStatus.RETRY_SCHEDULED)).thenReturn(1L)
        whenever(webhookLogRepository.countByStatus(WebhookStatus.SIGNATURE_FAILED)).thenReturn(1L)

        val stats = service.getWebhookStats()

        assertEquals(10L, stats["total_received"])
        assertEquals(6L, stats["processed"])
        assertEquals(2L, stats["failed"])
        assertEquals(1L, stats["pending_retry"])
        assertEquals(1L, stats["signature_failed"])
    }
}

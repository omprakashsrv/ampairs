package com.ampairs.subscription

import com.ampairs.subscription.domain.model.BillingMode
import com.ampairs.subscription.domain.model.BillingPreferences
import com.ampairs.subscription.domain.model.Invoice
import com.ampairs.subscription.domain.model.InvoiceGenerationLog
import com.ampairs.subscription.domain.model.InvoiceGenerationStatus
import com.ampairs.subscription.domain.model.PaymentProcessingStatus
import com.ampairs.subscription.domain.model.Subscription
import com.ampairs.subscription.domain.model.SubscriptionPlanDefinition
import com.ampairs.subscription.domain.repository.BillingPreferencesRepository
import com.ampairs.subscription.domain.repository.InvoiceGenerationLogRepository
import com.ampairs.subscription.domain.repository.SubscriptionInvoiceRepository
import com.ampairs.subscription.domain.repository.SubscriptionRepository
import com.ampairs.subscription.domain.service.InvoiceGenerationServiceV2
import com.ampairs.subscription.domain.service.InvoicePaymentService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.math.BigDecimal
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvoiceGenerationServiceV2Test {

    @Mock private lateinit var subscriptionRepository: SubscriptionRepository
    @Mock private lateinit var invoiceRepository: SubscriptionInvoiceRepository
    @Mock private lateinit var billingPreferencesRepository: BillingPreferencesRepository
    @Mock private lateinit var logRepository: InvoiceGenerationLogRepository
    @Mock private lateinit var invoicePaymentService: InvoicePaymentService

    private lateinit var service: InvoiceGenerationServiceV2

    @BeforeEach
    fun setUp() {
        service = InvoiceGenerationServiceV2(
            subscriptionRepository, invoiceRepository, billingPreferencesRepository,
            logRepository, invoicePaymentService
        )
        whenever(invoiceRepository.save(any<Invoice>())).thenAnswer { it.arguments[0] }
        whenever(logRepository.save(any<InvoiceGenerationLog>())).thenAnswer { it.arguments[0] }
        whenever(invoiceRepository.count()).thenReturn(0L)
    }

    private fun subscription() = Subscription().apply {
        uid = "SUB-1"; id = 7; workspaceId = "WS-1"; planCode = "PRO"
        nextBillingAmount = BigDecimal("1000")
        plan = SubscriptionPlanDefinition().apply { planCode = "PRO" }
    }

    private fun postpaidPrefs() = BillingPreferences().apply {
        workspaceId = "WS-1"; billingMode = BillingMode.POSTPAID; billingCurrency = "INR"
        billingCountry = "IN"; gracePeriodDays = 15
    }

    @Test
    fun `generateInvoiceForSubscription creates invoice and marks log SUCCESS`() {
        whenever(logRepository.findByWorkspaceIdAndBillingPeriodYearAndBillingPeriodMonth("WS-1", 2025, 1))
            .thenReturn(null)
        whenever(invoiceRepository.findByWorkspaceIdAndBillingPeriod(any(), any(), any())).thenReturn(emptyList())

        service.generateInvoiceForSubscription(subscription(), 2025, 1, postpaidPrefs())

        val captor = org.mockito.ArgumentCaptor.forClass(InvoiceGenerationLog::class.java)
        verify(logRepository, org.mockito.kotlin.atLeastOnce()).save(captor.capture())
        assertEquals(InvoiceGenerationStatus.SUCCESS, captor.allValues.last().status)
    }

    @Test
    fun `generateInvoiceForSubscription marks existing invoice as success`() {
        whenever(logRepository.findByWorkspaceIdAndBillingPeriodYearAndBillingPeriodMonth("WS-1", 2025, 2))
            .thenReturn(null)
        val existing = Invoice().apply { id = 99; invoiceNumber = "INV-EXIST" }
        whenever(invoiceRepository.findByWorkspaceIdAndBillingPeriod(any(), any(), any()))
            .thenReturn(listOf(existing))

        service.generateInvoiceForSubscription(subscription(), 2025, 2, postpaidPrefs())

        verify(invoicePaymentService, org.mockito.kotlin.never()).generatePaymentLink(any())
    }

    @Test
    fun `generateInvoiceForSubscription sends payment link when not auto-pay`() {
        whenever(logRepository.findByWorkspaceIdAndBillingPeriodYearAndBillingPeriodMonth("WS-1", 2025, 3))
            .thenReturn(null)
        whenever(invoiceRepository.findByWorkspaceIdAndBillingPeriod(any(), any(), any())).thenReturn(emptyList())

        service.generateInvoiceForSubscription(subscription(), 2025, 3, postpaidPrefs())

        verify(invoicePaymentService).generatePaymentLink(any())
    }

    @Test
    fun `generateInvoiceForSubscription attempts auto-charge when configured`() {
        val prefs = postpaidPrefs().apply { autoPaymentEnabled = true; defaultPaymentMethodId = 5L }
        whenever(logRepository.findByWorkspaceIdAndBillingPeriodYearAndBillingPeriodMonth("WS-1", 2025, 4))
            .thenReturn(null)
        whenever(invoiceRepository.findByWorkspaceIdAndBillingPeriod(any(), any(), any())).thenReturn(emptyList())

        service.generateInvoiceForSubscription(subscription(), 2025, 4, prefs)

        verify(invoicePaymentService).processAutoPayment(any())
    }

    @Test
    fun `manuallyGenerateInvoice throws when no subscription`() {
        whenever(subscriptionRepository.findByWorkspaceId("WS-X")).thenReturn(null)
        assertThrows(IllegalArgumentException::class.java) {
            service.manuallyGenerateInvoice("WS-X", 2025, 1)
        }
    }

    @Test
    fun `manuallyGenerateInvoice throws when no billing prefs`() {
        whenever(subscriptionRepository.findByWorkspaceId("WS-1")).thenReturn(subscription())
        whenever(billingPreferencesRepository.findByWorkspaceId("WS-1")).thenReturn(null)
        assertThrows(IllegalArgumentException::class.java) {
            service.manuallyGenerateInvoice("WS-1", 2025, 1)
        }
    }

    @Test
    fun `manuallyGenerateInvoice throws when prepaid`() {
        whenever(subscriptionRepository.findByWorkspaceId("WS-1")).thenReturn(subscription())
        whenever(billingPreferencesRepository.findByWorkspaceId("WS-1"))
            .thenReturn(BillingPreferences().apply { billingMode = BillingMode.PREPAID })
        assertThrows(IllegalArgumentException::class.java) {
            service.manuallyGenerateInvoice("WS-1", 2025, 1)
        }
    }

    @Test
    fun `manuallyGenerateInvoice returns generated invoice`() {
        whenever(subscriptionRepository.findByWorkspaceId("WS-1")).thenReturn(subscription())
        whenever(billingPreferencesRepository.findByWorkspaceId("WS-1")).thenReturn(postpaidPrefs())
        whenever(invoiceRepository.findByWorkspaceIdAndBillingPeriod(any(), any(), any())).thenReturn(emptyList())
        // First lookup (in generateInvoicesForMonth path) is via generateInvoiceForSubscription's own lookup
        whenever(logRepository.findByWorkspaceIdAndBillingPeriodYearAndBillingPeriodMonth("WS-1", 2025, 6))
            .thenReturn(null)
            .thenReturn(InvoiceGenerationLog().apply { invoiceId = 42 })
        whenever(invoiceRepository.findById(42L)).thenReturn(Optional.of(Invoice().apply { id = 42; invoiceNumber = "INV-42" }))

        val result = service.manuallyGenerateInvoice("WS-1", 2025, 6)

        assertNotNull(result)
        assertEquals("INV-42", result!!.invoiceNumber)
    }

    @Test
    fun `getGenerationStats aggregates statuses`() {
        val logs = listOf(
            InvoiceGenerationLog().apply { workspaceId = "A"; status = InvoiceGenerationStatus.SUCCESS },
            InvoiceGenerationLog().apply { workspaceId = "B"; status = InvoiceGenerationStatus.FAILED; errorMessage = "boom" },
            InvoiceGenerationLog().apply { workspaceId = "C"; status = InvoiceGenerationStatus.PENDING },
            InvoiceGenerationLog().apply { workspaceId = "D"; status = InvoiceGenerationStatus.IN_PROGRESS }
        )
        whenever(logRepository.findByBillingPeriodYearAndBillingPeriodMonth(2025, 1)).thenReturn(logs)

        val stats = service.getGenerationStats(2025, 1)

        assertEquals(4, stats.totalAttempts)
        assertEquals(1, stats.successCount)
        assertEquals(1, stats.failedCount)
        assertEquals(1, stats.pendingCount)
        assertEquals(1, stats.inProgressCount)
        assertEquals(1, stats.failedWorkspaces.size)
        assertEquals("B" to "boom", stats.failedWorkspaces.first())
    }

    @Test
    fun `dailyInvoiceReconciliation runs end-to-end without active subscriptions`() {
        whenever(subscriptionRepository.findByStatus(any())).thenReturn(emptyList())
        whenever(logRepository.findFailedLogsReadyForRetry(any())).thenReturn(emptyList())
        whenever(logRepository.findLogsWithPendingPaymentProcessing()).thenReturn(emptyList())

        service.dailyInvoiceReconciliation()

        verify(logRepository).findFailedLogsReadyForRetry(any())
        verify(logRepository).findLogsWithPendingPaymentProcessing()
    }

    @Test
    fun `InvoiceGenerationLog mark transitions behave as expected`() {
        val log = InvoiceGenerationLog()
        log.markInProgress()
        assertEquals(InvoiceGenerationStatus.IN_PROGRESS, log.status)
        assertEquals(1, log.attemptCount)

        log.markFailed(RuntimeException("err"), shouldRetryAgain = true)
        assertEquals(InvoiceGenerationStatus.FAILED, log.status)
        assertNotNull(log.nextRetryAt)

        log.markSucceeded(5L, "INV-5")
        assertEquals(InvoiceGenerationStatus.SUCCESS, log.status)
        assertEquals(5L, log.invoiceId)

        log.markPaymentProcessing(PaymentProcessingStatus.LINK_SENT)
        assertEquals(PaymentProcessingStatus.LINK_SENT, log.paymentStatus)
        assertNotNull(log.paymentLinkSentAt)
    }
}

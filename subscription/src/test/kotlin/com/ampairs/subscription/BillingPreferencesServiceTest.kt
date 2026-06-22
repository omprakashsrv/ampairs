package com.ampairs.subscription

import com.ampairs.subscription.domain.dto.BillingAddressDto
import com.ampairs.subscription.domain.dto.UpdateBillingPreferencesRequest
import com.ampairs.subscription.domain.model.BillingMode
import com.ampairs.subscription.domain.model.BillingPreferences
import com.ampairs.subscription.domain.model.PaymentMethod
import com.ampairs.subscription.domain.repository.BillingPreferencesRepository
import com.ampairs.subscription.domain.repository.PaymentMethodRepository
import com.ampairs.subscription.domain.service.BillingPreferencesService
import com.ampairs.subscription.exception.SubscriptionException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingPreferencesServiceTest {

    @Mock
    private lateinit var billingPreferencesRepository: BillingPreferencesRepository

    @Mock
    private lateinit var paymentMethodRepository: PaymentMethodRepository

    private lateinit var service: BillingPreferencesService

    @BeforeEach
    fun setUp() {
        service = BillingPreferencesService(billingPreferencesRepository, paymentMethodRepository)
        whenever(billingPreferencesRepository.save(any<BillingPreferences>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `getBillingPreferences returns the stored preferences`() {
        val prefs = BillingPreferences().apply { workspaceId = "WS-1"; billingEmail = "a@b.com" }
        whenever(billingPreferencesRepository.findByWorkspaceId("WS-1")).thenReturn(prefs)

        val result = service.getBillingPreferences("WS-1")

        assertEquals("WS-1", result.workspaceId)
        assertEquals("a@b.com", result.billingEmail)
    }

    @Test
    fun `getBillingPreferences throws NotFound when missing`() {
        whenever(billingPreferencesRepository.findByWorkspaceId("WS-X")).thenReturn(null)

        assertThrows(SubscriptionException.NotFound::class.java) {
            service.getBillingPreferences("WS-X")
        }
    }

    @Test
    fun `updateBillingPreferences creates new preferences when none exist`() {
        whenever(billingPreferencesRepository.findByWorkspaceId("WS-NEW")).thenReturn(null)

        val request = UpdateBillingPreferencesRequest(
            autoPaymentEnabled = true,
            billingMode = BillingMode.POSTPAID,
            billingEmail = "new@b.com",
            sendPaymentReminders = false,
            gracePeriodDays = 30,
            taxIdentifier = "GST123"
        )

        val result = service.updateBillingPreferences("WS-NEW", request)

        assertEquals("WS-NEW", result.workspaceId)
        assertTrue(result.autoPaymentEnabled)
        assertEquals(BillingMode.POSTPAID, result.billingMode)
        assertEquals("new@b.com", result.billingEmail)
        assertEquals(false, result.sendPaymentReminders)
        assertEquals(30, result.gracePeriodDays)
        assertEquals("GST123", result.taxIdentifier)
    }

    @Test
    fun `updateBillingPreferences updates billing address fields`() {
        val prefs = BillingPreferences().apply { workspaceId = "WS-1" }
        whenever(billingPreferencesRepository.findByWorkspaceId("WS-1")).thenReturn(prefs)

        val request = UpdateBillingPreferencesRequest(
            billingAddress = BillingAddressDto(
                line1 = "12 Main St",
                line2 = "Apt 5",
                city = "Pune",
                state = "MH",
                postalCode = "411001",
                country = "IN"
            )
        )

        val result = service.updateBillingPreferences("WS-1", request)

        assertEquals("12 Main St", result.billingAddressLine1)
        assertEquals("Apt 5", result.billingAddressLine2)
        assertEquals("Pune", result.billingCity)
        assertEquals("MH", result.billingState)
        assertEquals("411001", result.billingPostalCode)
        assertEquals("IN", result.billingCountry)
    }

    @Test
    fun `updateBillingPreferences sets default payment method when it belongs to workspace`() {
        val prefs = BillingPreferences().apply { workspaceId = "WS-1" }
        whenever(billingPreferencesRepository.findByWorkspaceId("WS-1")).thenReturn(prefs)
        val pm = PaymentMethod().apply { workspaceId = "WS-1" }
        whenever(paymentMethodRepository.findById(99L)).thenReturn(Optional.of(pm))

        val result = service.updateBillingPreferences("WS-1", UpdateBillingPreferencesRequest(defaultPaymentMethodId = 99L))

        assertEquals(99L, result.defaultPaymentMethodId)
    }

    @Test
    fun `updateBillingPreferences throws when payment method not found`() {
        val prefs = BillingPreferences().apply { workspaceId = "WS-1" }
        whenever(billingPreferencesRepository.findByWorkspaceId("WS-1")).thenReturn(prefs)
        whenever(paymentMethodRepository.findById(404L)).thenReturn(Optional.empty())

        assertThrows(SubscriptionException.PaymentMethodNotFound::class.java) {
            service.updateBillingPreferences("WS-1", UpdateBillingPreferencesRequest(defaultPaymentMethodId = 404L))
        }
    }

    @Test
    fun `updateBillingPreferences rejects payment method from another workspace`() {
        val prefs = BillingPreferences().apply { workspaceId = "WS-1" }
        whenever(billingPreferencesRepository.findByWorkspaceId("WS-1")).thenReturn(prefs)
        val pm = PaymentMethod().apply { workspaceId = "WS-OTHER" }
        whenever(paymentMethodRepository.findById(5L)).thenReturn(Optional.of(pm))

        assertThrows(SubscriptionException.PaymentFailed::class.java) {
            service.updateBillingPreferences("WS-1", UpdateBillingPreferencesRequest(defaultPaymentMethodId = 5L))
        }
    }
}

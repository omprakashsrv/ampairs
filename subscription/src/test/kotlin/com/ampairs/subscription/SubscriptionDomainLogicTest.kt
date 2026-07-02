package com.ampairs.subscription

import com.ampairs.subscription.domain.dto.asBillingPreferencesResponse
import com.ampairs.subscription.domain.dto.asInvoiceResponse
import com.ampairs.subscription.domain.dto.asInvoiceResponses
import com.ampairs.subscription.domain.model.AddonModuleCode
import com.ampairs.subscription.domain.model.BillingCycle
import com.ampairs.subscription.domain.model.BillingPreferences
import com.ampairs.subscription.domain.model.DeviceRegistration
import com.ampairs.subscription.domain.model.Invoice
import com.ampairs.subscription.domain.model.InvoiceLineItem
import com.ampairs.subscription.domain.model.InvoiceStatus
import com.ampairs.subscription.domain.model.PaymentMethod
import com.ampairs.subscription.domain.model.PaymentMethodType
import com.ampairs.subscription.domain.model.SubscriptionAccessMode
import com.ampairs.subscription.domain.model.SubscriptionPlanDefinition
import com.ampairs.subscription.domain.model.UsageMetric
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class SubscriptionDomainLogicTest {

    // ----- BillingCycle -----

    @Test
    fun `BillingCycle monthly has no discount`() {
        assertEquals(0, BigDecimal("100.00").compareTo(BillingCycle.MONTHLY.calculateDiscountedPrice(BigDecimal("100"))))
    }

    @Test
    fun `BillingCycle annual applies 20 percent discount over 12 months`() {
        // 100 * 12 = 1200, minus 20% = 960.00
        val price = BillingCycle.ANNUAL.calculateDiscountedPrice(BigDecimal("100"))
        assertEquals(0, BigDecimal("960.00").compareTo(price))
    }

    // ----- AddonModuleCode -----

    @Test
    fun `AddonModuleCode price by currency`() {
        assertEquals(0, BigDecimal("299.00").compareTo(AddonModuleCode.TALLY_INTEGRATION.getPrice("INR")))
        assertEquals(0, BigDecimal("4.00").compareTo(AddonModuleCode.TALLY_INTEGRATION.getPrice("USD")))
        // unknown currency falls back to USD
        assertEquals(0, BigDecimal("4.00").compareTo(AddonModuleCode.TALLY_INTEGRATION.getPrice("EUR")))
    }

    // ----- Invoice -----

    @Test
    fun `Invoice remaining balance and full-paid checks`() {
        val inv = Invoice().apply { totalAmount = BigDecimal("100"); paidAmount = BigDecimal("40") }
        assertEquals(0, BigDecimal("60").compareTo(inv.getRemainingBalance()))
        assertFalse(inv.isFullyPaid())

        inv.paidAmount = BigDecimal("100")
        assertTrue(inv.isFullyPaid())
    }

    @Test
    fun `Invoice overdue and days past due`() {
        val inv = Invoice().apply {
            totalAmount = BigDecimal("100")
            paidAmount = BigDecimal.ZERO
            dueDate = Instant.now().minus(5, ChronoUnit.DAYS)
        }
        assertTrue(inv.isOverdue())
        assertEquals(5L, inv.getDaysPastDue())

        val notDue = Invoice().apply {
            totalAmount = BigDecimal("100")
            dueDate = Instant.now().plus(5, ChronoUnit.DAYS)
        }
        assertFalse(notDue.isOverdue())
        assertEquals(0L, notDue.getDaysPastDue())
    }

    @Test
    fun `Invoice recalculateTotals sums line items`() {
        val inv = Invoice().apply { taxAmount = BigDecimal("18"); discountAmount = BigDecimal("10") }
        val li = InvoiceLineItem().apply { description = "Plan"; quantity = 2; unitPrice = BigDecimal("50") }
        li.calculateAmount() // 100
        inv.addLineItem(li)

        assertEquals(0, BigDecimal("100").compareTo(inv.subtotal))
        // total = 100 + 18 - 10 = 108
        assertEquals(0, BigDecimal("108").compareTo(inv.totalAmount))
    }

    // ----- InvoiceLineItem -----

    @Test
    fun `InvoiceLineItem amount and tax calculations`() {
        val li = InvoiceLineItem().apply { quantity = 3; unitPrice = BigDecimal("10"); taxPercent = BigDecimal("18") }
        assertEquals(0, BigDecimal("30").compareTo(li.calculateAmount()))
        // 30 * 18 / 100 = 5.4000
        assertEquals(0, BigDecimal("5.4000").compareTo(li.calculateTaxAmount()))
    }

    // ----- PaymentMethod -----

    @Test
    fun `PaymentMethod display name for card and upi`() {
        val card = PaymentMethod().apply { type = PaymentMethodType.CREDIT_CARD; brand = "VISA"; last4 = "4242" }
        assertEquals("VISA **** 4242", card.getDisplayName())

        val upi = PaymentMethod().apply { type = PaymentMethodType.UPI; upiId = "a@upi" }
        assertEquals("UPI: a@upi", upi.getDisplayName())
    }

    @Test
    fun `PaymentMethod expiry logic`() {
        val expired = PaymentMethod().apply {
            type = PaymentMethodType.CREDIT_CARD
            expYear = YearMonth.now().year - 1
            expMonth = 1
        }
        assertTrue(expired.isExpired())
        assertFalse(expired.isVerified())

        val nonCard = PaymentMethod().apply { type = PaymentMethodType.UPI }
        assertFalse(nonCard.isExpired())
        assertTrue(nonCard.isVerified())
    }

    // ----- UsageMetric -----

    @Test
    fun `UsageMetric storage GB and limit flags`() {
        val m = UsageMetric().apply { storageUsedBytes = 1024L * 1024 * 1024 } // 1 GB
        assertEquals(1.0, m.getStorageUsedGb(), 0.0001)
        assertFalse(m.hasExceededLimits())

        m.productLimitExceeded = true
        assertTrue(m.hasExceededLimits())

        assertEquals(YearMonth.of(m.periodYear, m.periodMonth), m.getPeriod())
    }

    @Test
    fun `UsageMetric forCurrentPeriod factory`() {
        val m = UsageMetric.forCurrentPeriod("WS-1")
        assertEquals("WS-1", m.workspaceId)
        assertEquals(YearMonth.now().year, m.periodYear)
    }

    // ----- DeviceRegistration -----

    @Test
    fun `DeviceRegistration access mode transitions`() {
        val valid = DeviceRegistration().apply {
            isActive = true; tokenExpiresAt = Instant.now().plus(Duration.ofDays(3))
        }
        assertEquals(SubscriptionAccessMode.FULL_ACCESS, valid.getAccessMode())

        val grace = DeviceRegistration().apply {
            isActive = true; tokenExpiresAt = Instant.now().minus(Duration.ofDays(1))
        }
        assertEquals(SubscriptionAccessMode.OFFLINE_GRACE, grace.getAccessMode())

        val locked = DeviceRegistration().apply { isActive = false }
        assertEquals(SubscriptionAccessMode.LOCKED, locked.getAccessMode())

        val readOnly = DeviceRegistration().apply {
            isActive = true; tokenExpiresAt = Instant.now().minus(Duration.ofDays(5))
        }
        assertEquals(SubscriptionAccessMode.READ_ONLY, readOnly.getAccessMode())
    }

    @Test
    fun `DeviceRegistration refresh and deactivate`() {
        val device = DeviceRegistration().apply { isActive = true }
        device.refreshToken()
        assertTrue(device.tokenExpiresAt.isAfter(Instant.now()))
        assertTrue(device.isTokenValid())

        device.deactivate("retired")
        assertFalse(device.isActive)
        assertEquals("retired", device.deactivationReason)
        assertTrue(device.shouldBeLocked())
    }

    // ----- SubscriptionPlanDefinition -----

    @Test
    fun `Plan price by currency and free detection`() {
        val plan = SubscriptionPlanDefinition().apply {
            planCode = "PRO"; monthlyPriceInr = BigDecimal("999"); monthlyPriceUsd = BigDecimal("12")
        }
        assertEquals(0, BigDecimal("999").compareTo(plan.getMonthlyPrice("INR")))
        assertEquals(0, BigDecimal("12").compareTo(plan.getMonthlyPrice("xx")))
        assertFalse(plan.isFree())
        assertTrue(plan.isUnlimited(-1))
        assertFalse(plan.isUnlimited(10))

        val free = SubscriptionPlanDefinition().apply { planCode = "FREE" }
        assertTrue(free.isFree())
    }

    @Test
    fun `Plan multi-workspace discount`() {
        val plan = SubscriptionPlanDefinition().apply {
            monthlyPriceInr = BigDecimal("1000")
            multiWorkspaceMinCount = 3
            multiWorkspaceDiscountPercent = 10
        }
        // below threshold → no discount
        assertEquals(0, BigDecimal("1000").compareTo(plan.getPriceWithDiscount("INR", 2)))
        assertFalse(plan.hasMultiWorkspaceDiscount(2))
        // at threshold → 10% off
        assertEquals(0, BigDecimal("900").compareTo(plan.getPriceWithDiscount("INR", 3)))
        assertTrue(plan.hasMultiWorkspaceDiscount(3))
    }

    @Test
    fun `Plan seasonal discount active window`() {
        val plan = SubscriptionPlanDefinition().apply {
            seasonalDiscountPercent = 15
            seasonalDiscountStartAt = Instant.now().minus(1, ChronoUnit.DAYS)
            seasonalDiscountEndAt = Instant.now().plus(1, ChronoUnit.DAYS)
        }
        assertTrue(plan.hasActiveSeasonalDiscount())
        assertEquals(15, plan.getActiveSeasonalDiscountPercent())

        val inactive = SubscriptionPlanDefinition().apply { seasonalDiscountPercent = 0 }
        assertFalse(inactive.hasActiveSeasonalDiscount())
        assertEquals(0, inactive.getActiveSeasonalDiscountPercent())
    }

    @Test
    fun `Plan pre-launch discount and stacked all-discounts`() {
        val plan = SubscriptionPlanDefinition().apply {
            monthlyPriceInr = BigDecimal("1000")
            preLaunchDiscountPercent = 50
            preLaunchDiscountEndAt = Instant.now().plus(5, ChronoUnit.DAYS)
            preLaunchNewUsersOnly = true
        }
        assertTrue(plan.hasActivePreLaunchDiscount(isNewUser = true))
        assertFalse(plan.hasActivePreLaunchDiscount(isNewUser = false))
        assertEquals(50, plan.getActivePreLaunchDiscountPercent(true))

        // only pre-launch applies → 1000 * 0.5 = 500.00
        assertEquals(0, BigDecimal("500.00").compareTo(plan.getPriceWithAllDiscounts("INR", 1, true)))
        assertEquals(50, plan.getTotalDiscountPercent(1, true))
    }

    @Test
    fun `Plan total discount caps at 100`() {
        val plan = SubscriptionPlanDefinition().apply {
            preLaunchDiscountPercent = 60
            preLaunchDiscountEndAt = Instant.now().plus(5, ChronoUnit.DAYS)
            preLaunchNewUsersOnly = false
            multiWorkspaceMinCount = 1
            multiWorkspaceDiscountPercent = 30
            seasonalDiscountPercent = 30
            seasonalDiscountStartAt = Instant.now().minus(1, ChronoUnit.DAYS)
            seasonalDiscountEndAt = Instant.now().plus(1, ChronoUnit.DAYS)
        }
        assertEquals(100, plan.getTotalDiscountPercent(5, true))
    }

    // ----- BillingPreferences -----

    @Test
    fun `BillingPreferences address and auto-payment config`() {
        val prefs = BillingPreferences().apply {
            workspaceId = "WS-1"
            billingAddressLine1 = "1 St"
            billingCity = "Pune"
        }
        assertEquals("1 St, Pune", prefs.getFullBillingAddress())
        assertFalse(prefs.isAutoPaymentConfigured())

        prefs.autoPaymentEnabled = true
        prefs.defaultPaymentMethodId = 9L
        assertTrue(prefs.isAutoPaymentConfigured())

        val empty = BillingPreferences()
        assertNull(empty.getFullBillingAddress())
    }

    // ----- DTO mappers -----

    @Test
    fun `Invoice maps to response including derived fields and line items`() {
        val inv = Invoice().apply {
            uid = "INV-1"
            invoiceNumber = "INV-2025-001"
            subscriptionId = 7
            status = InvoiceStatus.PENDING
            totalAmount = BigDecimal("100")
            paidAmount = BigDecimal("25")
            dueDate = Instant.now().minus(2, ChronoUnit.DAYS)
            currency = "INR"
        }
        val li = InvoiceLineItem().apply { description = "Plan"; quantity = 1; unitPrice = BigDecimal("100"); amount = BigDecimal("100") }
        li.invoice = inv
        inv.lineItems.add(li)

        val dto = inv.asInvoiceResponse()
        assertEquals("INV-2025-001", dto.invoiceNumber)
        assertEquals(0, BigDecimal("75").compareTo(dto.remainingBalance))
        assertTrue(dto.isOverdue)
        assertEquals(1, dto.lineItems.size)
        assertEquals("Plan", dto.lineItems.first().description)

        assertEquals(1, listOf(inv).asInvoiceResponses().size)
    }

    @Test
    fun `BillingPreferences maps to response`() {
        val prefs = BillingPreferences().apply {
            workspaceId = "WS-1"
            billingEmail = "a@b.com"
            autoPaymentEnabled = true
            defaultPaymentMethodId = 3L
        }
        val dto = prefs.asBillingPreferencesResponse()
        assertEquals("WS-1", dto.workspaceId)
        assertEquals("a@b.com", dto.billingEmail)
        assertTrue(dto.isAutoPaymentConfigured)
    }
}

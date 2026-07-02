package com.ampairs.pricing.service

import com.ampairs.core.domain.enums.SalesChannel
import com.ampairs.pricing.domain.dto.CouponApplyRequest
import com.ampairs.pricing.domain.dto.CouponRedeemRequest
import com.ampairs.pricing.domain.enums.CouponRejectionReason
import com.ampairs.pricing.domain.enums.OfferConditionType
import com.ampairs.pricing.domain.enums.OfferRewardType
import com.ampairs.pricing.domain.enums.OfferStatus
import com.ampairs.pricing.domain.model.CouponRedemption
import com.ampairs.pricing.domain.model.Offer
import com.ampairs.pricing.repository.CouponRedemptionRepository
import com.ampairs.pricing.repository.OfferRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CouponServiceTest {

    private val offerRepository: OfferRepository = mock()
    private val couponRedemptionRepository: CouponRedemptionRepository = mock()
    private val service = CouponServiceImpl(offerRepository, couponRedemptionRepository)

    private fun coupon(uid: String, block: Offer.() -> Unit = {}) = Offer().apply {
        this.uid = uid
        name = "Coupon $uid"
        channel = SalesChannel.RETAIL
        status = OfferStatus.ACTIVE
        active = true
        conditionType = OfferConditionType.COUPON
        couponCode = "SAVE10"
        rewardType = OfferRewardType.PERCENT
        rewardPercent = 10.0
        block()
    }

    private fun stubLookup(vararg offers: Offer) =
        whenever(offerRepository.findByChannelAndStatusAndActiveTrueAndCouponCodeIgnoreCase(any(), any(), any()))
            .thenReturn(offers.toList())

    @Test
    fun `validate accepts a known active coupon (case-insensitive)`() {
        stubLookup(coupon("C1"))
        val res = service.validate(CouponApplyRequest(code = "save10", channel = SalesChannel.RETAIL))
        assertTrue(res.valid)
        assertEquals("C1", res.offerUid)
        assertEquals(OfferRewardType.PERCENT, res.rewardType)
    }

    @Test
    fun `validate rejects an unknown coupon`() {
        stubLookup()
        val res = service.validate(CouponApplyRequest(code = "NOPE"))
        assertFalse(res.valid)
        assertEquals(CouponRejectionReason.NOT_FOUND, res.rejectionReason)
    }

    @Test
    fun `validate rejects when global limit reached`() {
        stubLookup(coupon("C1") { totalLimit = 100 })
        whenever(couponRedemptionRepository.countByCouponOfferUid("C1")).thenReturn(100L)
        val res = service.validate(CouponApplyRequest(code = "SAVE10"))
        assertFalse(res.valid)
        assertEquals(CouponRejectionReason.GLOBAL_LIMIT_REACHED, res.rejectionReason)
    }

    @Test
    fun `validate rejects when per-customer limit reached`() {
        stubLookup(coupon("C1") { perCustomerLimit = 1 })
        whenever(couponRedemptionRepository.countByCouponOfferUidAndCustomerId("C1", "CUST1")).thenReturn(1L)
        val res = service.validate(CouponApplyRequest(code = "SAVE10", customerId = "CUST1"))
        assertFalse(res.valid)
        assertEquals(CouponRejectionReason.PER_CUSTOMER_LIMIT_REACHED, res.rejectionReason)
    }

    @Test
    fun `redeem records a new redemption when not yet redeemed for the order`() {
        stubLookup(coupon("C1"))
        whenever(couponRedemptionRepository.existsByCouponOfferUidAndOrderRef("C1", "ORD1")).thenReturn(false)
        val res = service.redeem(CouponRedeemRequest(code = "SAVE10", customerId = "CUST1", orderRef = "ORD1"))
        assertTrue(res.valid)
        verify(couponRedemptionRepository).save(any<CouponRedemption>())
    }

    @Test
    fun `redeem is idempotent for the same order (no second row)`() {
        stubLookup(coupon("C1"))
        whenever(couponRedemptionRepository.existsByCouponOfferUidAndOrderRef("C1", "ORD1")).thenReturn(true)
        val res = service.redeem(CouponRedeemRequest(code = "SAVE10", orderRef = "ORD1"))
        assertTrue(res.valid)
        verify(couponRedemptionRepository, never()).save(any<CouponRedemption>())
    }

    @Test
    fun `redeem refuses when global limit already reached`() {
        stubLookup(coupon("C1") { totalLimit = 5 })
        whenever(couponRedemptionRepository.countByCouponOfferUid("C1")).thenReturn(5L)
        val res = service.redeem(CouponRedeemRequest(code = "SAVE10", orderRef = "ORD9"))
        assertFalse(res.valid)
        assertEquals(CouponRejectionReason.GLOBAL_LIMIT_REACHED, res.rejectionReason)
        verify(couponRedemptionRepository, never()).save(any<CouponRedemption>())
    }
}

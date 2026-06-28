package com.ampairs.pricing.service

import com.ampairs.core.domain.enums.SalesChannel
import com.ampairs.pricing.domain.dto.CartLineInput
import com.ampairs.pricing.domain.dto.OfferApplicationRequest
import com.ampairs.pricing.domain.enums.OfferConditionType
import com.ampairs.pricing.domain.enums.OfferRewardType
import com.ampairs.pricing.domain.enums.OfferStatus
import com.ampairs.pricing.domain.model.Offer
import com.ampairs.pricing.repository.OfferRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class OfferApplicationServiceTest {

    private val offerRepository: OfferRepository = mock()
    // Default mock returns null from zoneForPincode — no stubbing needed unless geo-targeting is tested.
    private val geoZoneService: GeoZoneService = mock()

    private val service = OfferApplicationServiceImpl(offerRepository, geoZoneService)

    private fun offer(uid: String, block: Offer.() -> Unit = {}) = Offer().apply {
        this.uid = uid
        name = "Offer $uid"
        channel = SalesChannel.RETAIL
        status = OfferStatus.ACTIVE
        active = true
        block()
    }

    private fun line(price: Long, qty: Long, block: CartLineInput.() -> CartLineInput = { this }) =
        CartLineInput(productId = "P", unitPriceMinor = price, quantity = BigDecimal(qty)).block()

    private fun req(vararg lines: CartLineInput, block: OfferApplicationRequest.() -> OfferApplicationRequest = { this }) =
        OfferApplicationRequest(channel = SalesChannel.RETAIL, customerGroupId = "RETAILERS", lines = lines.toList()).block()

    private fun stub(vararg offers: Offer) =
        whenever(offerRepository.findByChannelAndStatusAndActiveTrue(any(), any())).thenReturn(offers.toList())

    @Test
    fun `cart-min percent offer fires above threshold and reduces subtotal`() {
        stub(offer("O1") {
            conditionType = OfferConditionType.CART_MIN; cartMinMinor = 200_000
            rewardType = OfferRewardType.PERCENT; rewardPercent = 10.0
        })
        // 30 x ₹100.00 = ₹3,000.00 (300000 minor) ≥ ₹2,000 threshold → 10% = ₹300.00
        val res = service.apply(req(line(10_000, 30)))
        assertEquals(30_000L, res.totalDiscount.amountMinor)
        assertEquals(1, res.appliedOffers.size)
    }

    @Test
    fun `cart-min offer does not fire below threshold`() {
        stub(offer("O1") {
            conditionType = OfferConditionType.CART_MIN; cartMinMinor = 200_000
            rewardType = OfferRewardType.PERCENT; rewardPercent = 10.0
        })
        // 5 x ₹100 = ₹500 < ₹2,000 → no offer
        val res = service.apply(req(line(10_000, 5)))
        assertEquals(0L, res.totalDiscount.amountMinor)
        assertTrue(res.appliedOffers.isEmpty())
    }

    @Test
    fun `percent discount is capped at rewardCapMinor`() {
        stub(offer("O1") {
            rewardType = OfferRewardType.PERCENT; rewardPercent = 50.0; rewardCapMinor = 10_000
        })
        // 50% of ₹1,000 = ₹500, capped at ₹100.00
        val res = service.apply(req(line(100_000, 1)))
        assertEquals(10_000L, res.totalDiscount.amountMinor)
    }

    @Test
    fun `flat discount clamps to subtotal`() {
        stub(offer("O1") { rewardType = OfferRewardType.FLAT; rewardFlatMinor = 50_000 })
        // ₹300 flat off but subtotal only ₹200 → clamp to ₹200
        val res = service.apply(req(line(20_000, 1)))
        assertEquals(20_000L, res.totalDiscount.amountMinor)
    }

    @Test
    fun `bogo emits free goods by integer sets and no monetary discount`() {
        stub(offer("O1") {
            rewardType = OfferRewardType.BOGO; bogoBuyQty = 2; bogoGetQty = 1
        })
        // buy 5 → floor(5/2)=2 sets → 2 free
        val res = service.apply(req(line(10_000, 5)))
        assertEquals(0L, res.totalDiscount.amountMinor)
        assertEquals(1, res.freeGoods.size)
        assertEquals(BigDecimal(2), res.freeGoods.first().quantity)
        assertEquals("O1", res.freeGoods.first().sourceOfferUid)
    }

    @Test
    fun `offer targeting a different customer group does not apply`() {
        stub(offer("O1") {
            customerGroupId = "WHOLESALERS"
            rewardType = OfferRewardType.FLAT; rewardFlatMinor = 5_000
        })
        val res = service.apply(req(line(100_000, 1)) { copy(customerGroupId = "RETAILERS") })
        assertTrue(res.appliedOffers.isEmpty())
    }

    @Test
    fun `two stackable offers both apply cumulatively`() {
        stub(
            offer("O1") { priority = 10; stackable = true; rewardType = OfferRewardType.PERCENT; rewardPercent = 10.0 },
            offer("O2") { priority = 5; stackable = true; rewardType = OfferRewardType.FLAT; rewardFlatMinor = 5_000 },
        )
        // ₹1,000 → 10% (₹100) + flat ₹50 = ₹150
        val res = service.apply(req(line(100_000, 1)))
        assertEquals(15_000L, res.totalDiscount.amountMinor)
        assertEquals(2, res.appliedOffers.size)
    }

    @Test
    fun `exclusive offer blocks all others`() {
        stub(
            offer("EX") { priority = 1; exclusive = true; rewardType = OfferRewardType.FLAT; rewardFlatMinor = 2_000 },
            offer("O2") { priority = 99; stackable = true; rewardType = OfferRewardType.PERCENT; rewardPercent = 50.0 },
        )
        val res = service.apply(req(line(100_000, 1)))
        assertEquals(1, res.appliedOffers.size)
        assertEquals("EX", res.appliedOffers.first().offerUid)
        assertEquals(2_000L, res.totalDiscount.amountMinor)
    }

    @Test
    fun `only the highest-priority non-stackable offer applies`() {
        stub(
            offer("HI") { priority = 20; stackable = false; rewardType = OfferRewardType.FLAT; rewardFlatMinor = 3_000 },
            offer("LO") { priority = 1; stackable = false; rewardType = OfferRewardType.FLAT; rewardFlatMinor = 1_000 },
        )
        val res = service.apply(req(line(100_000, 1)))
        assertEquals(1, res.appliedOffers.size)
        assertEquals("HI", res.appliedOffers.first().offerUid)
    }

    @Test
    fun `coupon offer fires only when the matching code is entered`() {
        stub(offer("O1") {
            conditionType = OfferConditionType.COUPON; couponCode = "SAVE10"
            rewardType = OfferRewardType.PERCENT; rewardPercent = 10.0
        })
        assertTrue(service.apply(req(line(100_000, 1))).appliedOffers.isEmpty())
        val withCode = service.apply(req(line(100_000, 1)) { copy(couponCode = "save10") })
        assertEquals(10_000L, withCode.totalDiscount.amountMinor)
    }
}

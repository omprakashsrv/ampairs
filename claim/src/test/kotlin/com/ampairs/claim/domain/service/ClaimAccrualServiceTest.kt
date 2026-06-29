package com.ampairs.claim.domain.service

import com.ampairs.claim.domain.enums.ClaimStatus
import com.ampairs.claim.domain.model.SchemeClaim
import com.ampairs.claim.exception.ClaimException
import com.ampairs.claim.repository.ClaimSettlementRepository
import com.ampairs.claim.repository.SchemeClaimRepository
import com.ampairs.dms.domain.service.SnapshotService
import com.ampairs.pricing.domain.dto.OfferResponse
import com.ampairs.pricing.domain.enums.OfferRewardType
import com.ampairs.pricing.domain.enums.OfferStatus
import com.ampairs.pricing.service.OfferService
import com.ampairs.core.domain.enums.SalesChannel
import com.ampairs.pricing.domain.enums.OfferConditionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class ClaimAccrualServiceTest {

    private val snapshotService: SnapshotService = mock()
    private val offerService: OfferService = mock()
    private val claimRepository: SchemeClaimRepository = mock()
    private val settlementRepository: ClaimSettlementRepository = mock()
    private val claimService = ClaimService(claimRepository, settlementRepository)
    private val service = ClaimAccrualService(snapshotService, claimService, offerService)

    init {
        whenever(claimRepository.save(any<SchemeClaim>())).thenAnswer { it.arguments[0] as SchemeClaim }
    }

    private fun offer(rewardType: OfferRewardType, percent: Double?) = OfferResponse(
        uid = "OFFER-1", name = "Scheme", channel = SalesChannel.RETAIL, status = OfferStatus.ACTIVE,
        priority = 0, startsAt = null, endsAt = null, customerGroupId = null, customerType = null,
        brandId = null, categoryId = null, geoZoneId = null, conditionType = OfferConditionType.NONE,
        cartMinMinor = null, quantityMin = null, couponCode = null, couponLimit = null,
        rewardType = rewardType, rewardPercent = percent, rewardFlatMinor = null, rewardCapMinor = null,
        bogoBuyQty = null, bogoGetQty = null, stackable = false, exclusive = false,
        perCustomerLimit = null, totalLimit = null, usedCount = 0, active = true,
        createdAt = null, updatedAt = null,
    )

    @Test
    fun `explicit rate wins and the scheme is not read`() {
        whenever(snapshotService.qualifyingSecondaryValue(eq("BRAND"), eq("DIST"), eq("2026-06")))
            .thenReturn(BigDecimal("10000"))

        val claim = service.accrueFromSecondarySales("OFFER-1", "BRAND", "DIST", "2026-06", BigDecimal("2.5"))

        // 10000 × 2.5 / 100 = 250.0000
        assertEquals(0, BigDecimal("250.0000").compareTo(claim.computedAmount))
        assertEquals(ClaimStatus.DRAFT, claim.status)
        verify(offerService, never()).findByUid(any())
    }

    @Test
    fun `rate is read from the pricing scheme when the request omits it`() {
        whenever(offerService.findByUid("OFFER-1")).thenReturn(offer(OfferRewardType.PERCENT, 3.0))
        whenever(snapshotService.qualifyingSecondaryValue(eq("BRAND"), eq("DIST"), anyOrNull()))
            .thenReturn(BigDecimal("10000"))

        val claim = service.accrueFromSecondarySales("OFFER-1", "BRAND", "DIST", null, ratePercent = null)

        // 10000 × 3 / 100 = 300.0000
        assertEquals(0, BigDecimal("300.0000").compareTo(claim.computedAmount))
        verify(offerService).findByUid("OFFER-1")
    }

    @Test
    fun `a non-percent scheme without an explicit rate is rejected`() {
        whenever(offerService.findByUid("OFFER-1")).thenReturn(offer(OfferRewardType.FLAT, null))
        assertThrows<ClaimException> {
            service.accrueFromSecondarySales("OFFER-1", "BRAND", "DIST", "2026-06", ratePercent = null)
        }
    }

    @Test
    fun `a missing scheme without an explicit rate is rejected`() {
        whenever(offerService.findByUid("OFFER-1")).thenReturn(null)
        assertThrows<ClaimException> {
            service.accrueFromSecondarySales("OFFER-1", "BRAND", "DIST", "2026-06", ratePercent = null)
        }
    }

    @Test
    fun `zero qualifying value yields a zero-amount claim`() {
        whenever(snapshotService.qualifyingSecondaryValue(eq("BRAND"), eq("DIST"), anyOrNull()))
            .thenReturn(BigDecimal.ZERO)
        val claim = service.accrueFromSecondarySales("OFFER-1", "BRAND", "DIST", null, BigDecimal("5"))
        assertEquals(0, BigDecimal.ZERO.compareTo(claim.computedAmount))
    }

    @Test
    fun `a negative explicit rate is rejected`() {
        assertThrows<ClaimException> {
            service.accrueFromSecondarySales("OFFER-1", "BRAND", "DIST", "2026-06", BigDecimal("-1"))
        }
    }
}

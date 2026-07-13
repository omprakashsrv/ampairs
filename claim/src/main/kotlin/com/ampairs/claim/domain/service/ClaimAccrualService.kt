package com.ampairs.claim.domain.service

import com.ampairs.claim.domain.model.SchemeClaim
import com.ampairs.claim.exception.ClaimException
import com.ampairs.dms.domain.service.SnapshotService
import com.ampairs.pricing.domain.enums.OfferRewardType
import com.ampairs.pricing.service.OfferService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Derives a brand-funded scheme claim from the shared secondary-sales data rather than a hand-entered
 * amount. The qualifying value comes from the `dms` module (consent-gated by the trade link); the rate
 * comes from the `pricing`/015 scheme definition (`Offer.rewardPercent` for a PERCENT-reward offer),
 * unless the caller passes an explicit override.
 *
 * `computedAmount = qualifyingSecondaryValue × ratePercent / 100`, then handed to [ClaimService.accrue]
 * which opens the claim in DRAFT.
 */
@Service
class ClaimAccrualService(
    private val snapshotService: SnapshotService,
    private val claimService: ClaimService,
    private val offerService: OfferService,
) {

    fun accrueFromSecondarySales(
        schemeRef: String,
        brandWorkspaceId: String,
        distributorWorkspaceId: String,
        periodKey: String?,
        ratePercent: BigDecimal?,
        linkUid: String? = null,
    ): SchemeClaim {
        val rate = ratePercent ?: resolveSchemeRate(schemeRef)
        if (rate.signum() < 0) throw ClaimException("rate percent cannot be negative")

        val qualifyingValue =
            snapshotService.qualifyingSecondaryValue(brandWorkspaceId, distributorWorkspaceId, periodKey)
        val computedAmount = qualifyingValue
            .multiply(rate)
            .divide(BigDecimal(100), 4, RoundingMode.HALF_UP)

        return claimService.accrue(
            schemeRef = schemeRef,
            brandWorkspaceId = brandWorkspaceId,
            distributorWorkspaceId = distributorWorkspaceId,
            computedAmount = computedAmount,
            linkUid = linkUid,
            periodKey = periodKey,
        )
    }

    /**
     * Reads the brand's pricing/015 scheme [schemeRef]. Only a PERCENT-reward offer maps cleanly onto a
     * value × rate accrual; for other reward types the caller must pass an explicit [ratePercent].
     */
    private fun resolveSchemeRate(schemeRef: String): BigDecimal {
        val offer = offerService.findByUid(schemeRef)
            ?: throw ClaimException("scheme offer $schemeRef not found")
        if (offer.rewardType != OfferRewardType.PERCENT) {
            throw ClaimException(
                "scheme reward type ${offer.rewardType} cannot be accrued automatically — pass an explicit rate",
            )
        }
        val percent = offer.rewardPercent
            ?: throw ClaimException("scheme offer $schemeRef has no reward percent")
        return BigDecimal.valueOf(percent)
    }
}

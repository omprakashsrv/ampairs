package com.ampairs.claim.domain.service

import com.ampairs.claim.domain.model.SchemeClaim
import com.ampairs.claim.exception.ClaimException
import com.ampairs.dms.domain.service.SnapshotService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Derives a brand-funded scheme claim from the shared secondary-sales data rather than a hand-entered
 * amount. The qualifying value comes from the `dms` module (consent-gated by the trade link); the rate
 * is supplied per call for now — sourcing it from the `pricing`/015 scheme definition is a follow-up.
 *
 * `computedAmount = qualifyingSecondaryValue × ratePercent / 100`, then handed to [ClaimService.accrue]
 * which opens the claim in DRAFT.
 */
@Service
class ClaimAccrualService(
    private val snapshotService: SnapshotService,
    private val claimService: ClaimService,
) {

    fun accrueFromSecondarySales(
        schemeRef: String,
        brandWorkspaceId: String,
        distributorWorkspaceId: String,
        periodKey: String?,
        ratePercent: BigDecimal,
        linkUid: String? = null,
    ): SchemeClaim {
        if (ratePercent.signum() < 0) throw ClaimException("rate percent cannot be negative")

        val qualifyingValue =
            snapshotService.qualifyingSecondaryValue(brandWorkspaceId, distributorWorkspaceId, periodKey)
        val computedAmount = qualifyingValue
            .multiply(ratePercent)
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
}

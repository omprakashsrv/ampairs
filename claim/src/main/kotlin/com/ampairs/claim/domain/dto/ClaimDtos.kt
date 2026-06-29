package com.ampairs.claim.domain.dto

import com.ampairs.claim.domain.enums.ClaimStatus
import com.ampairs.claim.domain.model.ClaimSettlement
import com.ampairs.claim.domain.model.SchemeClaim
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant

data class ClaimAccrueRequest(
    @field:NotBlank val schemeRef: String? = null,
    @field:NotBlank val brandWorkspaceId: String? = null,
    @field:NotBlank val distributorWorkspaceId: String? = null,
    val linkUid: String? = null,
    val periodKey: String? = null,
    val computedAmount: BigDecimal? = null,
)

data class ClaimAccrueFromSalesRequest(
    @field:NotBlank val schemeRef: String? = null,
    @field:NotBlank val brandWorkspaceId: String? = null,
    @field:NotBlank val distributorWorkspaceId: String? = null,
    val periodKey: String? = null,
    /** Scheme rate as a percentage of qualifying secondary-sales value (e.g. 2.5 = 2.5%). */
    @field:NotNull val ratePercent: BigDecimal? = null,
    val linkUid: String? = null,
)

data class ClaimRejectRequest(@field:NotBlank val reason: String? = null)

data class ClaimSettleRequest(
    @field:NotBlank val reference: String? = null,
    val settledAmount: BigDecimal? = null,
)

data class SchemeClaimResponse(
    val uid: String,
    val schemeRef: String,
    val brandWorkspaceId: String,
    val distributorWorkspaceId: String,
    val computedAmount: BigDecimal,
    val status: ClaimStatus,
    val rejectionReason: String?,
)

fun SchemeClaim.asResponse(): SchemeClaimResponse =
    SchemeClaimResponse(uid, schemeRef, brandWorkspaceId, distributorWorkspaceId, computedAmount, status, rejectionReason)

data class ClaimSettlementResponse(
    val uid: String,
    val claimUid: String,
    val settledAmount: BigDecimal,
    val reference: String,
    val settledAt: Instant,
)

fun ClaimSettlement.asResponse(): ClaimSettlementResponse =
    ClaimSettlementResponse(uid, claimUid, settledAmount, reference, settledAt)

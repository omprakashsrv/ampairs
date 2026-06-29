package com.ampairs.claim.controller

import com.ampairs.claim.domain.dto.ClaimAccrueFromSalesRequest
import com.ampairs.claim.domain.dto.ClaimAccrueRequest
import com.ampairs.claim.domain.dto.ClaimRejectRequest
import com.ampairs.claim.domain.dto.ClaimSettleRequest
import com.ampairs.claim.domain.dto.ClaimSettlementResponse
import com.ampairs.claim.domain.dto.SchemeClaimResponse
import com.ampairs.claim.domain.dto.asResponse
import com.ampairs.claim.domain.service.ClaimAccrualService
import com.ampairs.claim.domain.service.ClaimService
import jakarta.validation.Valid
import java.math.BigDecimal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import com.ampairs.core.domain.dto.ApiResponse

/**
 * Trade-scheme claim lifecycle (definition reused from `pricing`/015). Distributor submits; brand
 * approves/rejects/settles. Errors bubble to ClaimExceptionHandler (409/422). Base `/claim/v1`.
 */
@RestController
@RequestMapping("/claim/v1")
class ClaimController(
    private val claimService: ClaimService,
    private val claimAccrualService: ClaimAccrualService,
) {

    @PostMapping("/claims")
    fun accrue(@RequestBody @Valid request: ClaimAccrueRequest): ApiResponse<SchemeClaimResponse> =
        ApiResponse.success(
            claimService.accrue(
                request.schemeRef!!, request.brandWorkspaceId!!, request.distributorWorkspaceId!!,
                request.computedAmount ?: BigDecimal.ZERO, request.linkUid, request.periodKey,
            ).asResponse(),
        )

    @PostMapping("/claims/accrue-from-sales")
    fun accrueFromSecondarySales(@RequestBody @Valid request: ClaimAccrueFromSalesRequest): ApiResponse<SchemeClaimResponse> =
        ApiResponse.success(
            claimAccrualService.accrueFromSecondarySales(
                request.schemeRef!!, request.brandWorkspaceId!!, request.distributorWorkspaceId!!,
                request.periodKey, request.ratePercent!!, request.linkUid,
            ).asResponse(),
        )

    @PostMapping("/claims/{uid}/submit")
    fun submit(@PathVariable uid: String): ApiResponse<SchemeClaimResponse> =
        ApiResponse.success(claimService.submit(uid).asResponse())

    @PostMapping("/claims/{uid}/approve")
    fun approve(@PathVariable uid: String): ApiResponse<SchemeClaimResponse> =
        ApiResponse.success(claimService.approve(uid).asResponse())

    @PostMapping("/claims/{uid}/reject")
    fun reject(@PathVariable uid: String, @RequestBody @Valid request: ClaimRejectRequest): ApiResponse<SchemeClaimResponse> =
        ApiResponse.success(claimService.reject(uid, request.reason!!).asResponse())

    @PostMapping("/claims/{uid}/settle")
    fun settle(@PathVariable uid: String, @RequestBody @Valid request: ClaimSettleRequest): ApiResponse<ClaimSettlementResponse> =
        ApiResponse.success(claimService.settle(uid, request.reference!!, request.settledAmount ?: BigDecimal.ZERO).asResponse())
}

package com.ampairs.claim.domain.service

import com.ampairs.claim.domain.enums.ClaimStatus
import com.ampairs.claim.domain.model.ClaimSettlement
import com.ampairs.claim.domain.model.SchemeClaim
import com.ampairs.claim.exception.ClaimException
import com.ampairs.claim.exception.ClaimStateException
import com.ampairs.claim.repository.ClaimSettlementRepository
import com.ampairs.claim.repository.SchemeClaimRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

/**
 * The claim → settlement reimbursement lifecycle: DRAFT → SUBMITTED → APPROVED|REJECTED → SETTLED.
 * Scheme definition is reused from `pricing`/015 (referenced by `schemeRef`); accrual from qualifying
 * secondary sales is a cross-module follow-up — `computedAmount` is supplied at accrue time for now.
 */
@Service
@Transactional
class ClaimService(
    private val claimRepository: SchemeClaimRepository,
    private val settlementRepository: ClaimSettlementRepository,
) {

    fun accrue(
        schemeRef: String,
        brandWorkspaceId: String,
        distributorWorkspaceId: String,
        computedAmount: BigDecimal,
        linkUid: String?,
        periodKey: String?,
    ): SchemeClaim {
        if (schemeRef.isBlank()) throw ClaimException("scheme_ref is required")
        if (computedAmount.signum() < 0) throw ClaimException("computed amount cannot be negative")
        return claimRepository.save(
            SchemeClaim().apply {
                this.schemeRef = schemeRef
                this.brandWorkspaceId = brandWorkspaceId
                this.distributorWorkspaceId = distributorWorkspaceId
                this.computedAmount = computedAmount
                this.linkUid = linkUid
                this.periodKey = periodKey
                this.status = ClaimStatus.DRAFT
            },
        )
    }

    fun submit(uid: String): SchemeClaim = transition(uid, from = ClaimStatus.DRAFT, to = ClaimStatus.SUBMITTED)

    fun approve(uid: String): SchemeClaim = transition(uid, from = ClaimStatus.SUBMITTED, to = ClaimStatus.APPROVED)

    fun reject(uid: String, reason: String): SchemeClaim {
        val claim = require(uid)
        if (claim.status != ClaimStatus.SUBMITTED) {
            throw ClaimStateException("only a SUBMITTED claim can be rejected (was ${claim.status})")
        }
        claim.status = ClaimStatus.REJECTED
        claim.rejectionReason = reason
        return claimRepository.save(claim)
    }

    /** Settle an APPROVED claim, recording a reconcilable reference. */
    fun settle(uid: String, reference: String, settledAmount: BigDecimal): ClaimSettlement {
        val claim = require(uid)
        if (claim.status != ClaimStatus.APPROVED) {
            throw ClaimStateException("only an APPROVED claim can be settled (was ${claim.status})")
        }
        if (reference.isBlank()) throw ClaimException("a settlement reference is required")
        claim.status = ClaimStatus.SETTLED
        claimRepository.save(claim)
        return settlementRepository.save(
            ClaimSettlement().apply {
                claimUid = claim.uid
                this.settledAmount = settledAmount
                this.reference = reference
                this.settledAt = Instant.now()
            },
        )
    }

    private fun transition(uid: String, from: ClaimStatus, to: ClaimStatus): SchemeClaim {
        val claim = require(uid)
        if (claim.status != from) {
            throw ClaimStateException("claim must be $from to move to $to (was ${claim.status})")
        }
        claim.status = to
        return claimRepository.save(claim)
    }

    private fun require(uid: String): SchemeClaim =
        claimRepository.findByUid(uid) ?: throw ClaimException("claim $uid not found")
}

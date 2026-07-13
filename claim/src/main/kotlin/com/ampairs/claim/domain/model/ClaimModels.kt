package com.ampairs.claim.domain.model

import com.ampairs.claim.config.Constants
import com.ampairs.claim.domain.enums.ClaimStatus
import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

/**
 * A reimbursement accrued from qualifying secondary sales under a brand-funded `pricing`/015 scheme.
 * `schemeRef` references the pricing scheme/offer uid — no scheme is defined here.
 */
@Entity
@Table(
    name = "scheme_claims",
    indexes = [
        Index(name = "idx_claim_scheme", columnList = "scheme_ref"),
        Index(name = "idx_claim_brand", columnList = "brand_workspace_id"),
        Index(name = "idx_claim_distributor", columnList = "distributor_workspace_id"),
        Index(name = "idx_claim_status", columnList = "status"),
    ],
)
class SchemeClaim : BaseDomain() {

    /** The `pricing`/015 scheme/offer uid this claim is computed against. */
    @Column(name = "scheme_ref", nullable = false, length = 40)
    var schemeRef: String = ""

    @Column(name = "link_uid", length = 40)
    var linkUid: String? = null

    @Column(name = "brand_workspace_id", nullable = false, length = 40)
    var brandWorkspaceId: String = ""

    @Column(name = "distributor_workspace_id", nullable = false, length = 40)
    var distributorWorkspaceId: String = ""

    @Column(name = "period_key", length = 20)
    var periodKey: String? = null

    /** Computed identically for brand and distributor from the same shared secondary-sales data. */
    @Column(name = "computed_amount", nullable = false, precision = 19, scale = 4)
    var computedAmount: BigDecimal = BigDecimal.ZERO

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: ClaimStatus = ClaimStatus.DRAFT

    @Column(name = "rejection_reason", length = 500)
    var rejectionReason: String? = null

    override fun obtainSeqIdPrefix(): String = Constants.CLAIM_PREFIX
}

/** The settled outcome of a [SchemeClaim], with a reconcilable reference for the distributor's books. */
@Entity
@Table(name = "claim_settlements", indexes = [Index(name = "idx_settlement_claim", columnList = "claim_uid")])
class ClaimSettlement : BaseDomain() {

    @Column(name = "claim_uid", nullable = false, length = 40)
    var claimUid: String = ""

    @Column(name = "settled_amount", nullable = false, precision = 19, scale = 4)
    var settledAmount: BigDecimal = BigDecimal.ZERO

    @Column(name = "reference", nullable = false, length = 120)
    var reference: String = ""

    @Column(name = "settled_at", nullable = false)
    var settledAt: Instant = Instant.EPOCH

    override fun obtainSeqIdPrefix(): String = Constants.SETTLEMENT_PREFIX
}

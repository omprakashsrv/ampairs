package com.ampairs.claim.domain.enums

/**
 * Lifecycle of a trade-scheme reimbursement claim. The distributor owns DRAFT→SUBMITTED; the brand
 * owns APPROVED/REJECTED/SETTLED. Scheme *definition* lives in `pricing` (spec 015) — this is only
 * the reimbursement layer.
 */
enum class ClaimStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED,
    SETTLED,
}

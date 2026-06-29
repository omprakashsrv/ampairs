package com.ampairs.claim.repository

import com.ampairs.claim.domain.model.ClaimSettlement
import com.ampairs.claim.domain.model.SchemeClaim
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SchemeClaimRepository : JpaRepository<SchemeClaim, Long> {
    fun findByUid(uid: String): SchemeClaim?
    fun findByBrandWorkspaceId(brandWorkspaceId: String): List<SchemeClaim>
    fun findByDistributorWorkspaceId(distributorWorkspaceId: String): List<SchemeClaim>
}

@Repository
interface ClaimSettlementRepository : JpaRepository<ClaimSettlement, Long> {
    fun findByClaimUid(claimUid: String): ClaimSettlement?
}

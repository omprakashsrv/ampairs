package com.ampairs.dms.repository

import com.ampairs.dms.domain.model.DistributorStockSnapshot
import com.ampairs.dms.domain.model.SalesTarget
import com.ampairs.dms.domain.model.SecondarySalesSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SecondarySalesSnapshotRepository : JpaRepository<SecondarySalesSnapshot, Long> {
    fun findByAttributedBrandWorkspaceIdAndDistributorWorkspaceId(
        attributedBrandWorkspaceId: String,
        distributorWorkspaceId: String,
    ): List<SecondarySalesSnapshot>

    fun findByAttributedBrandWorkspaceId(attributedBrandWorkspaceId: String): List<SecondarySalesSnapshot>
    fun deleteByDistributorWorkspaceId(distributorWorkspaceId: String)
}

@Repository
interface DistributorStockSnapshotRepository : JpaRepository<DistributorStockSnapshot, Long> {
    fun findByAttributedBrandWorkspaceIdAndDistributorWorkspaceId(
        attributedBrandWorkspaceId: String,
        distributorWorkspaceId: String,
    ): List<DistributorStockSnapshot>

    fun findByAttributedBrandWorkspaceId(attributedBrandWorkspaceId: String): List<DistributorStockSnapshot>
}

@Repository
interface SalesTargetRepository : JpaRepository<SalesTarget, Long> {
    fun findByUid(uid: String): SalesTarget?
    fun findByBrandWorkspaceIdAndPeriodKey(brandWorkspaceId: String, periodKey: String): List<SalesTarget>
    fun findByBrandWorkspaceId(brandWorkspaceId: String): List<SalesTarget>
}

package com.ampairs.dms.domain.service

import com.ampairs.dms.domain.AttributedRow
import com.ampairs.dms.domain.dto.DistributorStockRow
import com.ampairs.dms.domain.dto.SecondarySalesRow
import com.ampairs.dms.domain.dto.asRow
import com.ampairs.dms.domain.enums.SnapshotGrain
import com.ampairs.dms.domain.model.SecondarySalesSnapshot
import com.ampairs.dms.repository.DistributorStockSnapshotRepository
import com.ampairs.dms.repository.SecondarySalesSnapshotRepository
import com.ampairs.trade.domain.enums.DataCategory
import com.ampairs.trade.service.CrossTenantReadGuard
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Owns the secondary-sales / distributor-stock snapshots. Recompute replaces a distributor's rows
 * wholesale (deterministic, backdated-safe). Every brand read passes the trade-module consent gate.
 */
@Service
@Transactional
class SnapshotService(
    private val secondaryRepository: SecondarySalesSnapshotRepository,
    private val stockRepository: DistributorStockSnapshotRepository,
    private val guard: CrossTenantReadGuard,
) {

    /** Wholesale recompute of a distributor's secondary-sales snapshot from attributed rows. */
    fun recomputeSecondarySales(distributorWorkspaceId: String, rows: List<AttributedRow>): Int {
        secondaryRepository.deleteByDistributorWorkspaceId(distributorWorkspaceId)
        val entities = rows.map { r ->
            SecondarySalesSnapshot().apply {
                attributedBrandWorkspaceId = r.brandWorkspaceId
                this.distributorWorkspaceId = distributorWorkspaceId
                grain = SnapshotGrain.SKU_PERIOD
                periodKey = r.periodKey
                areaCode = r.areaCode
                brandProductUid = r.brandProductUid
                brandSkuCode = r.brandSkuCode
                quantity = r.quantity
                valueAmount = r.value
                version = 1
            }
        }
        secondaryRepository.saveAll(entities)
        return entities.size
    }

    @Transactional(readOnly = true)
    fun readSecondarySales(brandWorkspaceId: String, distributorWorkspaceId: String): List<SecondarySalesRow> {
        guard.requireActiveLink(brandWorkspaceId, distributorWorkspaceId, DataCategory.SECONDARY_SALES)
        return secondaryRepository
            .findByAttributedBrandWorkspaceIdAndDistributorWorkspaceId(brandWorkspaceId, distributorWorkspaceId)
            .map { it.asRow() }
    }

    @Transactional(readOnly = true)
    fun readDistributorStock(brandWorkspaceId: String, distributorWorkspaceId: String): List<DistributorStockRow> {
        guard.requireActiveLink(brandWorkspaceId, distributorWorkspaceId, DataCategory.STOCK)
        return stockRepository
            .findByAttributedBrandWorkspaceIdAndDistributorWorkspaceId(brandWorkspaceId, distributorWorkspaceId)
            .map { it.asRow() }
    }
}

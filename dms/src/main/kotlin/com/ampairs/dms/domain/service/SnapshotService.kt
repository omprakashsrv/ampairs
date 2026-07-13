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
import java.math.BigDecimal

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

    /**
     * Consent-gated sum of a distributor's secondary-sales value attributed to this brand, optionally
     * for a single [periodKey]. Drives brand-funded scheme accrual (the `claim` module reads this) — the
     * same SECONDARY_SALES consent gate as [readSecondarySales], so a brand can only accrue where it has
     * an active link. Returns [BigDecimal.ZERO] when there is nothing attributed.
     */
    @Transactional(readOnly = true)
    fun qualifyingSecondaryValue(
        brandWorkspaceId: String,
        distributorWorkspaceId: String,
        periodKey: String?,
    ): BigDecimal {
        guard.requireActiveLink(brandWorkspaceId, distributorWorkspaceId, DataCategory.SECONDARY_SALES)
        return secondaryRepository
            .findByAttributedBrandWorkspaceIdAndDistributorWorkspaceId(brandWorkspaceId, distributorWorkspaceId)
            .filter { periodKey.isNullOrBlank() || it.periodKey == periodKey }
            .fold(BigDecimal.ZERO) { acc, row -> acc.add(row.valueAmount) }
    }
}

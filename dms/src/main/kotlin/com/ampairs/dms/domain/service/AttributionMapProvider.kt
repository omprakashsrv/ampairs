package com.ampairs.dms.domain.service

import com.ampairs.dms.domain.BrandSku
import com.ampairs.trade.domain.enums.DesignationStatus
import com.ampairs.trade.domain.enums.MappingStatus
import com.ampairs.trade.service.NetworkBrandService
import com.ampairs.trade.service.NetworkProductService
import com.ampairs.trade.service.TradeLinkService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Builds the two attribution maps the [com.ampairs.dms.domain.SnapshotAttributionCalculator] needs
 * for a distributor, by reading trade's consent records across all its ACCEPTED links:
 * - **Hop A**: distributor brand-label uid → brand workspace id (ACTIVE designations only).
 * - **Hop B**: distributor product uid → brand SKU (CONFIRMED mappings only).
 * Cross-module reads go through trade's public services (never its repositories).
 */
@Service
@Transactional(readOnly = true)
class AttributionMapProvider(
    private val tradeLinkService: TradeLinkService,
    private val networkBrandService: NetworkBrandService,
    private val networkProductService: NetworkProductService,
) {

    fun hopA(distributorWorkspaceId: String): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        for (link in tradeLinkService.acceptedLinksForDistributor(distributorWorkspaceId)) {
            for (nb in networkBrandService.list(link.uid)) {
                if (nb.status == DesignationStatus.ACTIVE) {
                    map[nb.distributorProductBrandUid] = nb.brandWorkspaceId
                }
            }
        }
        return map
    }

    fun hopB(distributorWorkspaceId: String): Map<String, BrandSku> {
        val map = LinkedHashMap<String, BrandSku>()
        for (link in tradeLinkService.acceptedLinksForDistributor(distributorWorkspaceId)) {
            for (np in networkProductService.list(link.uid)) {
                val brandProductUid = np.brandProductUid
                if (np.status == MappingStatus.CONFIRMED && brandProductUid != null) {
                    map[np.distributorProductUid] = BrandSku(brandProductUid, np.brandSkuCode)
                }
            }
        }
        return map
    }
}

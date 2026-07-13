package com.ampairs.trade.service

import com.ampairs.trade.domain.enums.DataCategory
import com.ampairs.trade.domain.model.TradeLink
import com.ampairs.trade.exception.ConsentRequiredException
import org.springframework.stereotype.Service

/**
 * The single gate every cross-tenant brand read passes through. Requires an ACCEPTED [TradeLink]
 * whose [com.ampairs.trade.domain.model.ConsentScope] permits the requested data category, else
 * throws [ConsentRequiredException] (403). This is the feature's central trust boundary.
 */
@Service
class CrossTenantReadGuard(
    private val tradeLinkService: TradeLinkService,
) {

    fun requireActiveLink(
        brandWorkspaceId: String,
        distributorWorkspaceId: String,
        category: DataCategory,
    ): TradeLink {
        val link = tradeLinkService.findActiveLink(brandWorkspaceId, distributorWorkspaceId)
            ?: throw ConsentRequiredException(
                "No active link between brand $brandWorkspaceId and distributor $distributorWorkspaceId",
            )
        if (!link.consentScope.permits(category)) {
            throw ConsentRequiredException("The link's consent scope does not permit $category")
        }
        return link
    }
}

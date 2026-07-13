package com.ampairs.trade.service

import com.ampairs.trade.domain.enums.DesignationStatus
import com.ampairs.trade.domain.enums.LinkStatus
import com.ampairs.trade.domain.enums.MappingStatus
import com.ampairs.trade.domain.enums.MatchSource
import com.ampairs.trade.domain.enums.PrimaryOrderStatus
import com.ampairs.trade.domain.enums.PublicationStatus
import com.ampairs.trade.domain.model.NetworkBrand
import com.ampairs.trade.domain.model.NetworkProduct
import com.ampairs.trade.domain.model.PrimaryOrderLink
import com.ampairs.trade.domain.model.SchemePublication
import com.ampairs.trade.exception.ConsentRequiredException
import com.ampairs.trade.exception.LinkStateException
import com.ampairs.trade.exception.TradeException
import com.ampairs.trade.repository.NetworkBrandRepository
import com.ampairs.trade.repository.NetworkProductRepository
import com.ampairs.trade.repository.PrimaryOrderLinkRepository
import com.ampairs.trade.repository.SchemePublicationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Hop A — a distributor designates one of its existing brand labels as a linked brand's (brand reads it). */
@Service
@Transactional
class NetworkBrandService(
    private val repository: NetworkBrandRepository,
    private val tradeLinkService: TradeLinkService,
) {
    fun designate(linkUid: String, distributorProductBrandUid: String): NetworkBrand {
        val link = tradeLinkService.require(linkUid)
        if (link.status != LinkStatus.ACCEPTED) throw LinkStateException("designation requires an ACCEPTED link")
        repository.findByLinkUidAndDistributorProductBrandUidAndStatus(
            linkUid, distributorProductBrandUid, DesignationStatus.ACTIVE,
        )?.let { return it }
        return repository.save(
            NetworkBrand().apply {
                this.linkUid = linkUid
                this.distributorProductBrandUid = distributorProductBrandUid
                this.brandWorkspaceId = link.brandWorkspaceId
                this.status = DesignationStatus.ACTIVE
            },
        )
    }

    fun remove(uid: String): NetworkBrand {
        val nb = repository.findByUid(uid) ?: throw TradeException("network brand $uid not found")
        nb.status = DesignationStatus.REMOVED
        return repository.save(nb)
    }

    @Transactional(readOnly = true)
    fun list(linkUid: String): List<NetworkBrand> = repository.findByLinkUid(linkUid)
}

/** Hop B — optional distributor-product ↔ brand-SKU mapping (auto-suggested by barcode/SKU, never HSN). */
@Service
@Transactional
class NetworkProductService(
    private val repository: NetworkProductRepository,
    private val tradeLinkService: TradeLinkService,
) {
    fun upsertMapping(
        linkUid: String,
        distributorProductUid: String,
        brandProductUid: String?,
        brandSkuCode: String?,
        matchSource: MatchSource,
        status: MappingStatus,
    ): NetworkProduct {
        val link = tradeLinkService.require(linkUid)
        if (link.status != LinkStatus.ACCEPTED) throw LinkStateException("mapping requires an ACCEPTED link")
        return repository.save(
            NetworkProduct().apply {
                this.linkUid = linkUid
                this.distributorProductUid = distributorProductUid
                this.brandProductUid = brandProductUid
                this.brandSkuCode = brandSkuCode
                this.matchSource = matchSource
                this.status = status
            },
        )
    }

    fun confirm(uid: String): NetworkProduct {
        val np = repository.findByUid(uid) ?: throw TradeException("network product $uid not found")
        np.status = MappingStatus.CONFIRMED
        return repository.save(np)
    }

    @Transactional(readOnly = true)
    fun list(linkUid: String): List<NetworkProduct> = repository.findByLinkUid(linkUid)
}

/** Publishes a `pricing`/015 scheme down an ACCEPTED link (definition stays in pricing). */
@Service
@Transactional
class SchemePublicationService(
    private val repository: SchemePublicationRepository,
    private val tradeLinkService: TradeLinkService,
) {
    fun publish(linkUid: String, schemeRef: String): SchemePublication {
        val link = tradeLinkService.require(linkUid)
        if (link.status != LinkStatus.ACCEPTED) throw ConsentRequiredException("publish requires an ACCEPTED link")
        return repository.save(
            SchemePublication().apply {
                this.linkUid = linkUid
                this.schemeRef = schemeRef
                this.status = PublicationStatus.PUBLISHED
            },
        )
    }

    fun withdraw(uid: String): SchemePublication {
        val sp = repository.findByUid(uid) ?: throw TradeException("scheme publication $uid not found")
        sp.status = PublicationStatus.WITHDRAWN
        return repository.save(sp)
    }

    @Transactional(readOnly = true)
    fun listPublished(linkUid: String): List<SchemePublication> =
        repository.findByLinkUidAndStatus(linkUid, PublicationStatus.PUBLISHED)
}

/**
 * Brand→distributor primary-order handshake. PLACED requires an active link; the distributor confirms
 * (creating its own order — that cross-module create is a noted follow-up) or rejects. No silent write.
 */
@Service
@Transactional
class PrimaryOrderService(
    private val repository: PrimaryOrderLinkRepository,
    private val tradeLinkService: TradeLinkService,
) {
    fun place(brandWorkspaceId: String, distributorWorkspaceId: String, brandOrderUid: String): PrimaryOrderLink {
        tradeLinkService.findActiveLink(brandWorkspaceId, distributorWorkspaceId)
            ?: throw ConsentRequiredException("primary order requires an ACCEPTED link")
        return repository.save(
            PrimaryOrderLink().apply {
                this.brandWorkspaceId = brandWorkspaceId
                this.distributorWorkspaceId = distributorWorkspaceId
                this.brandOrderUid = brandOrderUid
                this.status = PrimaryOrderStatus.PLACED
            },
        )
    }

    /**
     * Distributor confirms — the resulting `order`-module order uid is supplied by the caller
     * (the actual order creation via OrderService is a cross-module follow-up).
     */
    fun confirm(uid: String, distributorOrderUid: String): PrimaryOrderLink {
        val pol = require(uid)
        if (pol.status != PrimaryOrderStatus.PLACED) throw LinkStateException("only a PLACED primary order can be confirmed")
        pol.status = PrimaryOrderStatus.CONFIRMED
        pol.distributorOrderUid = distributorOrderUid
        return repository.save(pol)
    }

    fun reject(uid: String): PrimaryOrderLink {
        val pol = require(uid)
        if (pol.status != PrimaryOrderStatus.PLACED) throw LinkStateException("only a PLACED primary order can be rejected")
        pol.status = PrimaryOrderStatus.REJECTED
        return repository.save(pol)
    }

    private fun require(uid: String): PrimaryOrderLink =
        repository.findByUid(uid) ?: throw TradeException("primary order $uid not found")
}

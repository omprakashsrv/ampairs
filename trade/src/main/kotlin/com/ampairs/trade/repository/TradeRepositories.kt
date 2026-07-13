package com.ampairs.trade.repository

import com.ampairs.trade.domain.enums.DesignationStatus
import com.ampairs.trade.domain.enums.LinkStatus
import com.ampairs.trade.domain.enums.PublicationStatus
import com.ampairs.trade.domain.model.NetworkBrand
import com.ampairs.trade.domain.model.NetworkProduct
import com.ampairs.trade.domain.model.NetworkRetailer
import com.ampairs.trade.domain.model.PrimaryOrderLink
import com.ampairs.trade.domain.model.SchemePublication
import com.ampairs.trade.domain.model.TradeLink
import com.ampairs.trade.domain.model.TradeNetwork
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TradeNetworkRepository : JpaRepository<TradeNetwork, Long> {
    fun findByBrandWorkspaceId(brandWorkspaceId: String): TradeNetwork?
}

@Repository
interface TradeLinkRepository : JpaRepository<TradeLink, Long> {
    fun findByUid(uid: String): TradeLink?
    fun findByBrandWorkspaceIdAndDistributorWorkspaceIdAndStatus(
        brandWorkspaceId: String,
        distributorWorkspaceId: String,
        status: LinkStatus,
    ): TradeLink?

    fun findByBrandWorkspaceId(brandWorkspaceId: String): List<TradeLink>
    fun findByDistributorWorkspaceId(distributorWorkspaceId: String): List<TradeLink>
    fun existsByBrandWorkspaceIdAndDistributorWorkspaceIdAndStatusNot(
        brandWorkspaceId: String,
        distributorWorkspaceId: String,
        status: LinkStatus,
    ): Boolean
}

@Repository
interface NetworkRetailerRepository : JpaRepository<NetworkRetailer, Long> {
    fun findByUid(uid: String): NetworkRetailer?
    fun findByLinkUid(linkUid: String): List<NetworkRetailer>
}

@Repository
interface NetworkBrandRepository : JpaRepository<NetworkBrand, Long> {
    fun findByUid(uid: String): NetworkBrand?
    fun findByLinkUid(linkUid: String): List<NetworkBrand>
    fun findByLinkUidAndDistributorProductBrandUidAndStatus(
        linkUid: String,
        distributorProductBrandUid: String,
        status: DesignationStatus,
    ): NetworkBrand?
}

@Repository
interface NetworkProductRepository : JpaRepository<NetworkProduct, Long> {
    fun findByUid(uid: String): NetworkProduct?
    fun findByLinkUid(linkUid: String): List<NetworkProduct>
}

@Repository
interface SchemePublicationRepository : JpaRepository<SchemePublication, Long> {
    fun findByUid(uid: String): SchemePublication?
    fun findByLinkUidAndStatus(linkUid: String, status: PublicationStatus): List<SchemePublication>
}

@Repository
interface PrimaryOrderLinkRepository : JpaRepository<PrimaryOrderLink, Long> {
    fun findByUid(uid: String): PrimaryOrderLink?
}

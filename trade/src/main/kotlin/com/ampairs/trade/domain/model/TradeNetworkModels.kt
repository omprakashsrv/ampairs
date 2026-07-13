package com.ampairs.trade.domain.model

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.trade.config.Constants
import com.ampairs.trade.domain.enums.DataCategory
import com.ampairs.trade.domain.enums.LinkStatus
import com.ampairs.trade.domain.enums.RetailerVisibility
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table

/*
 * The cross-tenant network/consent records extend BaseDomain (NOT OwnableBaseDomain): they are the
 * trust edge BETWEEN two workspaces, so they carry explicit brand/distributor workspace ids and are
 * never @TenantId-auto-filtered to one side. All reads go through CrossTenantReadGuard / explicit ids.
 */

/** A brand's trade network — the container for its linked distributors. */
@Entity
@Table(name = "trade_networks", indexes = [Index(name = "idx_trade_network_brand", columnList = "brand_workspace_id")])
class TradeNetwork : BaseDomain() {

    @Column(name = "brand_workspace_id", nullable = false, length = 40)
    var brandWorkspaceId: String = ""

    @Column(name = "name", length = 150)
    var name: String? = null

    override fun obtainSeqIdPrefix(): String = Constants.NETWORK_PREFIX
}

/** The agreed sharing scope on a link. Default: coded outlets, all aggregate categories shared. */
@Embeddable
class ConsentScope {

    @Enumerated(EnumType.STRING)
    @Column(name = "retailer_visibility", nullable = false, length = 20)
    var retailerVisibility: RetailerVisibility = RetailerVisibility.CODED

    @Column(name = "share_secondary_sales", nullable = false)
    var shareSecondarySales: Boolean = true

    @Column(name = "share_stock", nullable = false)
    var shareStock: Boolean = true

    @Column(name = "share_targets", nullable = false)
    var shareTargets: Boolean = true

    fun permits(category: DataCategory): Boolean = when (category) {
        DataCategory.SECONDARY_SALES -> shareSecondarySales
        DataCategory.STOCK -> shareStock
        DataCategory.TARGETS -> shareTargets
    }
}

/** The single consented edge between one brand and one distributor. Data flows only while ACCEPTED. */
@Entity
@Table(
    name = "trade_links",
    indexes = [
        Index(name = "idx_trade_link_brand", columnList = "brand_workspace_id"),
        Index(name = "idx_trade_link_distributor", columnList = "distributor_workspace_id"),
        Index(name = "idx_trade_link_status", columnList = "status"),
    ],
)
class TradeLink : BaseDomain() {

    @Column(name = "brand_workspace_id", nullable = false, length = 40)
    var brandWorkspaceId: String = ""

    @Column(name = "distributor_workspace_id", nullable = false, length = 40)
    var distributorWorkspaceId: String = ""

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: LinkStatus = LinkStatus.INVITED

    @Embedded
    var consentScope: ConsentScope = ConsentScope()

    override fun obtainSeqIdPrefix(): String = Constants.LINK_PREFIX
}

/** A distributor outlet (a customer) surfaced to a brand — coded by default, identified on opt-in. */
@Entity
@Table(
    name = "network_retailers",
    indexes = [
        Index(name = "idx_network_retailer_link", columnList = "link_uid"),
        Index(name = "idx_network_retailer_customer", columnList = "customer_uid"),
    ],
)
class NetworkRetailer : BaseDomain() {

    @Column(name = "link_uid", nullable = false, length = 40)
    var linkUid: String = ""

    @Column(name = "customer_uid", nullable = false, length = 40)
    var customerUid: String = ""

    @Column(name = "outlet_code", length = 60)
    var outletCode: String? = null

    /** Present only when the link scope opts into IDENTIFIED visibility (never full contact PII). */
    @Column(name = "identified_name", length = 200)
    var identifiedName: String? = null

    @Column(name = "pincode", length = 12)
    var pincode: String? = null

    override fun obtainSeqIdPrefix(): String = Constants.NETWORK_RETAILER_PREFIX
}

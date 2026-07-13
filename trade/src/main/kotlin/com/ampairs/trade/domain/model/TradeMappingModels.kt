package com.ampairs.trade.domain.model

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.trade.config.Constants
import com.ampairs.trade.domain.enums.DesignationStatus
import com.ampairs.trade.domain.enums.MappingStatus
import com.ampairs.trade.domain.enums.MatchSource
import com.ampairs.trade.domain.enums.PrimaryOrderStatus
import com.ampairs.trade.domain.enums.PublicationStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table

/** Hop A — a distributor designates one of its existing `product_brand` labels as a linked brand's. */
@Entity
@Table(
    name = "network_brands",
    indexes = [
        Index(name = "idx_network_brand_link", columnList = "link_uid"),
        Index(name = "idx_network_brand_label", columnList = "distributor_product_brand_uid"),
    ],
)
class NetworkBrand : BaseDomain() {

    @Column(name = "link_uid", nullable = false, length = 40)
    var linkUid: String = ""

    /** The distributor's existing `product_brand` label uid. */
    @Column(name = "distributor_product_brand_uid", nullable = false, length = 40)
    var distributorProductBrandUid: String = ""

    @Column(name = "brand_workspace_id", nullable = false, length = 40)
    var brandWorkspaceId: String = ""

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: DesignationStatus = DesignationStatus.ACTIVE

    override fun obtainSeqIdPrefix(): String = Constants.NETWORK_BRAND_PREFIX
}

/** Hop B — an optional, finer mapping of a distributor product to the brand's specific SKU. */
@Entity
@Table(
    name = "network_products",
    indexes = [
        Index(name = "idx_network_product_link", columnList = "link_uid"),
        Index(name = "idx_network_product_distributor", columnList = "distributor_product_uid"),
        Index(name = "idx_network_product_brand", columnList = "brand_product_uid"),
    ],
)
class NetworkProduct : BaseDomain() {

    @Column(name = "link_uid", nullable = false, length = 40)
    var linkUid: String = ""

    @Column(name = "distributor_product_uid", nullable = false, length = 40)
    var distributorProductUid: String = ""

    @Column(name = "brand_product_uid", length = 40)
    var brandProductUid: String? = null

    @Column(name = "brand_sku_code", length = 80)
    var brandSkuCode: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "match_source", nullable = false, length = 20)
    var matchSource: MatchSource = MatchSource.MANUAL

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: MappingStatus = MappingStatus.SUGGESTED

    override fun obtainSeqIdPrefix(): String = Constants.NETWORK_PRODUCT_PREFIX
}

/**
 * The consented publish edge for a brand-funded scheme. The scheme *definition* lives in `pricing`
 * (spec 015); this only references its uid and gates visibility to the link. Revoked with the link.
 */
@Entity
@Table(
    name = "scheme_publications",
    indexes = [
        Index(name = "idx_scheme_publication_link", columnList = "link_uid"),
        Index(name = "idx_scheme_publication_scheme", columnList = "scheme_ref"),
    ],
)
class SchemePublication : BaseDomain() {

    @Column(name = "link_uid", nullable = false, length = 40)
    var linkUid: String = ""

    /** The `pricing`/015 scheme/offer uid being published. */
    @Column(name = "scheme_ref", nullable = false, length = 40)
    var schemeRef: String = ""

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: PublicationStatus = PublicationStatus.PUBLISHED

    override fun obtainSeqIdPrefix(): String = Constants.SCHEME_PUBLICATION_PREFIX
}

/** Brand→distributor primary order handshake: PLACED in the brand tenant, CONFIRMED into the distributor's. */
@Entity
@Table(
    name = "primary_order_links",
    indexes = [
        Index(name = "idx_primary_order_brand", columnList = "brand_workspace_id"),
        Index(name = "idx_primary_order_distributor", columnList = "distributor_workspace_id"),
    ],
)
class PrimaryOrderLink : BaseDomain() {

    @Column(name = "brand_workspace_id", nullable = false, length = 40)
    var brandWorkspaceId: String = ""

    @Column(name = "distributor_workspace_id", nullable = false, length = 40)
    var distributorWorkspaceId: String = ""

    /** The order recorded in the brand's own tenant. */
    @Column(name = "brand_order_uid", nullable = false, length = 40)
    var brandOrderUid: String = ""

    /** The order created in the distributor's tenant on confirmation (null until then). */
    @Column(name = "distributor_order_uid", length = 40)
    var distributorOrderUid: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: PrimaryOrderStatus = PrimaryOrderStatus.PLACED

    override fun obtainSeqIdPrefix(): String = Constants.PRIMARY_ORDER_PREFIX
}

package com.ampairs.ecom.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.ecom.domain.enums.TaxonomyType
import jakarta.persistence.*

@Entity
@Table(
    name = "ecom_taxonomy_image",
    indexes = [Index(name = "idx_ecom_taxonomy_storefront", columnList = "storefront_id, taxonomy_type")],
    uniqueConstraints = [
        UniqueConstraint(name = "uq_ecom_taxonomy", columnNames = ["storefront_id", "taxonomy_type", "name"])
    ]
)
class EcomTaxonomyImage : OwnableBaseDomain() {

    @Column(name = "storefront_id", nullable = false, length = 200)
    var storefrontId: String = ""

    @Column(name = "taxonomy_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var taxonomyType: TaxonomyType = TaxonomyType.CATEGORY

    @Column(name = "name", nullable = false, length = 255)
    var name: String = ""

    @Column(name = "image_url", nullable = false, length = 500)
    var imageUrl: String = ""

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0

    override fun obtainSeqIdPrefix(): String = "TXI"
}

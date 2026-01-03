package com.ampairs.product.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.*

@Entity
@Table(
    name = "variant_attribute",
    indexes = [
        Index(name = "idx_variant_attribute_owner_id", columnList = "owner_id"),
        Index(name = "idx_variant_attribute_workspace_id", columnList = "workspace_id"),
        Index(name = "idx_variant_attribute_product_id", columnList = "product_id"),
        Index(name = "idx_variant_attribute_name", columnList = "attribute_name"),
        Index(name = "idx_variant_attribute_value", columnList = "attribute_value")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_variant_attribute",
            columnNames = ["owner_id", "product_id", "attribute_name", "attribute_value"]
        )
    ]
)
class VariantAttribute : OwnableBaseDomain() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "uid", insertable = false, updatable = false)
    var product: Product? = null

    @Column(name = "product_id", nullable = false)
    var productId: String = ""

    @Column(name = "attribute_name", nullable = false, length = 100)
    var attributeName: String = ""

    @Column(name = "attribute_value", nullable = false)
    var attributeValue: String = ""

    override fun obtainSeqIdPrefix(): String {
        return "VAT" // Variant Attribute
    }
}

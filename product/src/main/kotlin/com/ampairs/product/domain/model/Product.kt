package com.ampairs.product.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.product.config.Constants
import jakarta.persistence.*

@Entity(name = "product")
class Product : OwnableBaseDomain() {

    @Column(name = "name", nullable = false, length = 255)
    var name: String = ""

    @Column(name = "tax_code", length = 10)
    var taxCode: String = ""

    @Column(name = "group_id", length = 200)
    var groupId: String = ""

    @Column(name = "category_id", length = 200)
    var categoryId: String = ""

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    @Column(name = "mrp", nullable = false)
    var mrp: Double = 0.0

    @Column(name = "selling_price", nullable = false)
    var sellingPrice: Double = 0.0

    @OneToOne()
    @JoinColumn(name = "group_id", referencedColumnName = "id", updatable = false, insertable = false)
    var group: ProductGroup? = null

    @OneToOne()
    @JoinColumn(name = "group_id", referencedColumnName = "id", updatable = false, insertable = false)
    var category: ProductCategory? = null

    @OneToMany()
    @JoinColumn(
        name = "code", referencedColumnName = "tax_code", insertable = false, updatable = false, nullable = false
    )
    var taxCodes: MutableList<TaxCode> = mutableListOf()

    override fun obtainIdPrefix(): String {
        return Constants.PRODUCT_PREFIX
    }
}
package com.ampairs.product.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.product.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne

@Entity(name = "product_price")
class ProductPrice : OwnableBaseDomain() {

    @Column(name = "product_id", length = 200)
    var productId: String = ""

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    @Column(name = "mrp", nullable = false)
    var mrp: Double = 0.0

    @Column(name = "dp", nullable = false)
    var dp: Double = 0.0

    @Column(name = "selling_price", nullable = false)
    var sellingPrice: Double = 0.0

    @OneToOne()
    @JoinColumn(name = "product_id", referencedColumnName = "id", updatable = false, insertable = false)
    var product: Product? = null

    override fun obtainIdPrefix(): String {
        return Constants.PRODUCT_PREFIX
    }
}
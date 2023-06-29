package com.ampairs.product.domain.model

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.product.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity

@Entity(name = "product_category")
class ProductCategory : BaseDomain() {

    @Column(name = "name", nullable = false, length = 255)
    var name: String = ""

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainIdPrefix(): String {
        return Constants.PRODUCT_CATEGORY_PREFIX
    }
}
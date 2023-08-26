package com.ampairs.product.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.product.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity

@Entity(name = "product_group")
class ProductSubCategory : OwnableBaseDomain() {

    @Column(name = "name", nullable = false, length = 255)
    var name: String = ""

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainIdPrefix(): String {
        return Constants.PRODUCT_SUB_CATEGORY_PREFIX
    }
}
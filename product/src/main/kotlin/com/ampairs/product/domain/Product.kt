package com.ampairs.product.domain

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.product.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne

@Entity(name = "product")
class Product : BaseDomain() {

    @Column(name = "name", nullable = false, length = 255)
    var name: String = ""

    @Column(name = "hsn_id", length = 200)
    var hsnId: String = ""

    @Column(name = "group_id", length = 200)
    var groupId: String = ""

    @Column(name = "category_id", length = 200)
    var categoryId: String = ""

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    @OneToOne()
    @JoinColumn(name = "group_id", referencedColumnName = "id", updatable = false, insertable = false)
    var group: ProductGroup? = null

    @OneToOne()
    @JoinColumn(name = "group_id", referencedColumnName = "id", updatable = false, insertable = false)
    var category: ProductCategory? = null

    override fun obtainIdPrefix(): String {
        return Constants.PRODUCT_PREFIX
    }
}
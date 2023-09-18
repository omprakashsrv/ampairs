package com.ampairs.product.domain.model

import com.ampairs.core.domain.model.File
import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.product.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne

@Entity(name = "product_brand")
class ProductBrand : OwnableBaseDomain() {

    @Column(name = "name", nullable = false, length = 255)
    var name: String = ""

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    @Column(name = "image_id", length = 200)
    var imageId: String? = null

    @OneToOne
    @JoinColumn(name = "image_id", referencedColumnName = "id", updatable = false, insertable = false)
    var image: File? = null

    override fun obtainIdPrefix(): String {
        return Constants.PRODUCT_BRAND_PREFIX
    }
}
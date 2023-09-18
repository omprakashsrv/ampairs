package com.ampairs.product.domain.model

import com.ampairs.core.domain.model.File
import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.product.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne


@Entity(name = "product_image")
class ProductImage : OwnableBaseDomain() {

    @Column(name = "image_id", length = 50, nullable = false)
    var imageId: String = ""

    @Column(name = "product_id", length = 50, nullable = false)
    var productId: String = ""

    @Column(name = "active")
    var active: Boolean = true

    @OneToOne
    @JoinColumn(name = "image_id", referencedColumnName = "id", updatable = false, insertable = false)
    var image: File? = null

    override fun obtainIdPrefix(): String {
        return Constants.PRODUCT_IMAGE_PREFIX
    }
}
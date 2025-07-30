package com.ampairs.product.domain.model.group

import com.ampairs.core.domain.model.File
import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.product.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne

@Entity(name = "product_sub_category")
class ProductSubCategory : OwnableBaseDomain() {

    @Column(name = "name", nullable = false, length = 255)
    var name: String = ""

    @Column(name = "image_id", length = 200)
    var imageId: String? = null

    @Column(name = "index_no", nullable = false)
    var index: Int = 0

    @OneToOne
    @JoinColumn(name = "image_id", referencedColumnName = "id", updatable = false, insertable = false)
    var image: File? = null

    override fun obtainSeqIdPrefix(): String {
        return Constants.PRODUCT_SUB_CATEGORY_PREFIX
    }
}
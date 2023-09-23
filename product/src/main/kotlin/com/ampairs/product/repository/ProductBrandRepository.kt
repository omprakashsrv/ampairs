package com.ampairs.product.repository

import com.ampairs.product.domain.model.group.ProductBrand
import org.springframework.data.repository.CrudRepository

interface ProductBrandRepository : CrudRepository<ProductBrand, String> {
    fun findByRefId(refId: String?): ProductBrand?

}
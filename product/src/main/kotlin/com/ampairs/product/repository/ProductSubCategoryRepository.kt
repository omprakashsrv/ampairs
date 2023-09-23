package com.ampairs.product.repository

import com.ampairs.product.domain.model.group.ProductSubCategory
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.*

interface ProductSubCategoryRepository : CrudRepository<ProductSubCategory, String> {
    fun findByRefId(refId: String?): ProductSubCategory?

    @Query("SELECT psc FROM product_sub_category psc WHERE psc.id = :id")
    override fun findById(id: String): Optional<ProductSubCategory>
}
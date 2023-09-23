package com.ampairs.product.repository

import com.ampairs.product.domain.model.ProductGroup
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.*

interface ProductGroupRepository : CrudRepository<ProductGroup, String> {
    fun findByRefId(refId: String?): ProductGroup?

    @Query("SELECT pg FROM product_group pg WHERE pg.id = :id")
    override fun findById(id: String): Optional<ProductGroup>

}
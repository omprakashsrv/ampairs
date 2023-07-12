package com.ampairs.product.repository

import com.ampairs.product.domain.model.Product
import org.springframework.data.repository.CrudRepository

interface ProductRepository : CrudRepository<Product, String> {
    fun findByRefId(refId: String?): Product?
}
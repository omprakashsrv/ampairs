package com.ampairs.product.repository

import com.ampairs.product.domain.model.Product
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface ProductRepository : CrudRepository<Product, String> {
    fun findByRefId(refId: String?): Product?


    @Query("SELECT p FROM product p WHERE p.groupId IN (:ids)")
    fun getProduct(ids: String): List<Product>
}
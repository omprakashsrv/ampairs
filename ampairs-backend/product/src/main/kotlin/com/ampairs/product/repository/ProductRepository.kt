package com.ampairs.product.repository

import com.ampairs.product.domain.model.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import java.util.*

interface ProductRepository : CrudRepository<Product, Long>, PagingAndSortingRepository<Product, Long> {
    fun findByUid(uid: String?): Product?
    fun findByRefId(refId: String?): Product?
    fun findBySku(sku: String): Optional<Product>
    fun findByStatus(status: String): List<Product>
    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<Product>

    @Query("SELECT p FROM product p WHERE p.groupId IN (:ids)")
    fun getProduct(ids: String): List<Product>

    @Query("SELECT p FROM product p WHERE p.name ILIKE %:searchTerm% OR p.sku ILIKE %:searchTerm% OR p.description ILIKE %:searchTerm%")
    fun searchProducts(searchTerm: String, pageable: Pageable): Page<Product>

    @Query("SELECT p FROM product p WHERE p.categoryId = :categoryId AND p.status = 'ACTIVE'")
    fun findActiveProductsByCategory(categoryId: String, pageable: Pageable): Page<Product>

    @Query("SELECT p FROM product p WHERE p.brandId = :brandId AND p.status = 'ACTIVE'")
    fun findActiveProductsByBrand(brandId: String, pageable: Pageable): Page<Product>

    @Query("SELECT p FROM product p WHERE p.basePrice BETWEEN :minPrice AND :maxPrice AND p.status = 'ACTIVE'")
    fun findActiveProductsByPriceRange(minPrice: Double, maxPrice: Double, pageable: Pageable): Page<Product>
}
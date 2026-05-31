package com.ampairs.ecom.repository

import com.ampairs.ecom.domain.model.EcomListedProduct
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.Instant
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository

@Repository
interface EcomListedProductRepository :
    CrudRepository<EcomListedProduct, Long>,
    PagingAndSortingRepository<EcomListedProduct, Long> {

    fun findByStorefrontIdAndManagementProductId(storefrontId: String, managementProductId: String): EcomListedProduct?

    fun findByStorefrontIdAndIsVisible(storefrontId: String, isVisible: Boolean): List<EcomListedProduct>

    fun findByStorefrontIdAndIsVisibleTrue(storefrontId: String, pageable: Pageable): Page<EcomListedProduct>

    fun findByUid(uid: String): EcomListedProduct?

    @Query(
        value = """
            SELECT * FROM ecom_listed_product
            WHERE storefront_id = :storefrontId
              AND is_visible = true
              AND search_vector @@ plainto_tsquery('english', :query)
            ORDER BY ts_rank(search_vector, plainto_tsquery('english', :query)) DESC
        """,
        countQuery = """
            SELECT COUNT(*) FROM ecom_listed_product
            WHERE storefront_id = :storefrontId
              AND is_visible = true
              AND search_vector @@ plainto_tsquery('english', :query)
        """,
        nativeQuery = true
    )
    fun searchByText(storefrontId: String, query: String, pageable: Pageable): Page<EcomListedProduct>

    @Query(
        "SELECT p FROM EcomListedProduct p WHERE p.storefrontId = :storefrontId AND p.isVisible = true " +
        "AND (:category IS NULL OR p.category = :category) " +
        "AND (:brand IS NULL OR p.brand = :brand) " +
        "AND (:subcategory IS NULL OR p.subcategory = :subcategory)"
    )
    fun findByFilters(
        storefrontId: String,
        category: String?,
        brand: String?,
        subcategory: String?,
        pageable: Pageable,
    ): Page<EcomListedProduct>

    @Query(
        "SELECT p FROM EcomListedProduct p WHERE p.storefrontId = :storefrontId " +
        "AND p.updatedAt > :since ORDER BY p.updatedAt ASC"
    )
    fun findChangedSince(storefrontId: String, since: Instant, pageable: Pageable): Page<EcomListedProduct>

    @Query(
        value = """
            SELECT category, subcategory, COUNT(*) as cnt
            FROM ecom_listed_product
            WHERE storefront_id = :storefrontId AND is_visible = true AND category IS NOT NULL
            GROUP BY category, subcategory
        """,
        nativeQuery = true
    )
    fun countByCategoryAndSubcategory(storefrontId: String): List<Array<Any>>

    @Query(
        value = """
            SELECT brand, COUNT(*) as cnt
            FROM ecom_listed_product
            WHERE storefront_id = :storefrontId AND is_visible = true AND brand IS NOT NULL
            GROUP BY brand
            ORDER BY cnt DESC
        """,
        nativeQuery = true
    )
    fun countByBrand(storefrontId: String): List<Array<Any>>
}

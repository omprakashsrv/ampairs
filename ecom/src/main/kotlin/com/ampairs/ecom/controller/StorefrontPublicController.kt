package com.ampairs.ecom.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.ecom.domain.dto.BrandMeta
import com.ampairs.ecom.domain.dto.CategoryMeta
import com.ampairs.ecom.domain.dto.ListedProductResponse
import com.ampairs.ecom.domain.dto.StorefrontCatalogMetaResponse
import com.ampairs.ecom.domain.dto.StorefrontResponse
import com.ampairs.ecom.domain.dto.SubcategoryMeta
import com.ampairs.ecom.domain.dto.asListedProductResponse
import com.ampairs.ecom.domain.dto.asStorefrontResponse
import com.ampairs.ecom.exception.EcomOrderNotFoundException
import com.ampairs.ecom.repository.EcomListedProductRepository
import com.ampairs.ecom.service.StorefrontService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/store/{slug}")
class StorefrontPublicController(
    private val storefrontService: StorefrontService,
    private val listedProductRepository: EcomListedProductRepository,
) {

    @GetMapping
    fun getStorefront(@PathVariable slug: String): ApiResponse<StorefrontResponse> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(slug)
        return ApiResponse.success(storefront.asStorefrontResponse())
    }

    @GetMapping("/products")
    fun getProducts(
        @PathVariable slug: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam category: String? = null,
        @RequestParam brand: String? = null,
        @RequestParam subcategory: String? = null,
    ): ApiResponse<PageResponse<ListedProductResponse>> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(slug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
            val result = if (category != null || brand != null || subcategory != null) {
                listedProductRepository.findByFilters(storefront.uid, category, brand, subcategory, pageable)
            } else {
                listedProductRepository.findByStorefrontIdAndIsVisibleTrue(storefront.uid, pageable)
            }
            return ApiResponse.success(PageResponse.from(result.map { it.asListedProductResponse() }))
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @GetMapping("/products/search")
    fun searchProducts(
        @PathVariable slug: String,
        @RequestParam q: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<ListedProductResponse>> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(slug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            val pageable = PageRequest.of(page, size)
            val result = listedProductRepository.searchByText(storefront.uid, q, pageable)
            return ApiResponse.success(PageResponse.from(result.map { it.asListedProductResponse() }))
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @GetMapping("/catalog-meta")
    fun getCatalogMeta(@PathVariable slug: String): ApiResponse<StorefrontCatalogMetaResponse> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(slug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            val catRows = listedProductRepository.countByCategoryAndSubcategory(storefront.uid)
            val categories = catRows
                .groupBy { it[0] as String }
                .map { (cat, rows) ->
                    val total = rows.sumOf { (it[2] as Number).toInt() }
                    val subs = rows
                        .mapNotNull { row -> (row[1] as? String)?.let { SubcategoryMeta(it, (row[2] as Number).toInt()) } }
                        .sortedByDescending { it.productCount }
                    CategoryMeta(name = cat, productCount = total, subcategories = subs)
                }
                .sortedByDescending { it.productCount }

            val brands = listedProductRepository.countByBrand(storefront.uid)
                .map { BrandMeta(name = it[0] as String, productCount = (it[1] as Number).toInt()) }

            return ApiResponse.success(StorefrontCatalogMetaResponse(categories = categories, brands = brands))
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @GetMapping("/products/{productId}")
    fun getProduct(
        @PathVariable slug: String,
        @PathVariable productId: String,
    ): ApiResponse<ListedProductResponse> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(slug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            val product = listedProductRepository.findByUid(productId)
                ?.takeIf { it.storefrontId == storefront.uid && it.isVisible }
                ?: throw EcomOrderNotFoundException("Product not found")
            return ApiResponse.success(product.asListedProductResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }
}

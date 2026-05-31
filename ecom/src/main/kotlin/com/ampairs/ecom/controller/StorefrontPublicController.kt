package com.ampairs.ecom.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.ecom.domain.dto.BrandMeta
import com.ampairs.ecom.domain.dto.CategoryMeta
import com.ampairs.ecom.domain.dto.ListedProductResponse
import com.ampairs.ecom.domain.dto.ProductSyncItem
import com.ampairs.ecom.domain.dto.StorefrontCatalogMetaResponse
import com.ampairs.ecom.domain.dto.StorefrontResponse
import com.ampairs.ecom.domain.dto.SubcategoryMeta
import com.ampairs.ecom.domain.dto.SyncPage
import com.ampairs.ecom.domain.dto.asListedProductResponse
import com.ampairs.ecom.domain.dto.asProductSyncItem
import com.ampairs.ecom.domain.dto.asStorefrontResponse
import com.ampairs.ecom.domain.enums.TaxonomyType
import com.ampairs.ecom.exception.EcomOrderNotFoundException
import com.ampairs.ecom.repository.EcomListedProductRepository
import com.ampairs.ecom.repository.EcomTaxonomyImageRepository
import com.ampairs.ecom.service.StorefrontService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/v1/store/{slug}")
class StorefrontPublicController(
    private val storefrontService: StorefrontService,
    private val listedProductRepository: EcomListedProductRepository,
    private val taxonomyImageRepository: EcomTaxonomyImageRepository,
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

    @GetMapping("/products/sync")
    fun syncProducts(
        @PathVariable slug: String,
        @RequestParam since: Instant,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "100") size: Int,
    ): ApiResponse<SyncPage<ProductSyncItem>> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(slug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            // Capture server time BEFORE the query so no updates fall between the cracks.
            val nextSince = Instant.now()
            val pageable = PageRequest.of(page, size, Sort.by("updatedAt").ascending())
            val result = listedProductRepository.findChangedSince(storefront.uid, since, pageable)
            return ApiResponse.success(
                SyncPage(
                    items = result.content.map { it.asProductSyncItem() },
                    totalChanges = result.totalElements,
                    page = result.number,
                    size = result.size,
                    hasMore = result.hasNext(),
                    nextSince = nextSince,
                )
            )
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @GetMapping("/catalog-meta")
    fun getCatalogMeta(@PathVariable slug: String): ApiResponse<StorefrontCatalogMetaResponse> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(slug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            val taxonomyImages = taxonomyImageRepository
                .findByStorefrontIdOrderBySortOrderAsc(storefront.uid)
                .groupBy { it.taxonomyType }

            val catImages = taxonomyImages[TaxonomyType.CATEGORY]?.associateBy { it.name } ?: emptyMap()
            val subImages = taxonomyImages[TaxonomyType.SUBCATEGORY]?.associateBy { it.name } ?: emptyMap()
            val brandImages = taxonomyImages[TaxonomyType.BRAND]?.associateBy { it.name } ?: emptyMap()

            val catRows = listedProductRepository.countByCategoryAndSubcategory(storefront.uid)
            val categories = catRows
                .groupBy { it[0] as String }
                .map { (cat, rows) ->
                    val total = rows.sumOf { (it[2] as Number).toInt() }
                    val subs = rows
                        .mapNotNull { row ->
                            (row[1] as? String)?.let { sub ->
                                SubcategoryMeta(
                                    name = sub,
                                    productCount = (row[2] as Number).toInt(),
                                    imageUrl = subImages[sub]?.imageUrl,
                                    sortOrder = subImages[sub]?.sortOrder ?: 0,
                                )
                            }
                        }
                        .sortedWith(compareBy({ subImages[it.name]?.sortOrder ?: Int.MAX_VALUE }, { -it.productCount }))
                    CategoryMeta(
                        name = cat,
                        productCount = total,
                        imageUrl = catImages[cat]?.imageUrl,
                        sortOrder = catImages[cat]?.sortOrder ?: 0,
                        subcategories = subs,
                    )
                }
                .sortedWith(compareBy({ catImages[it.name]?.sortOrder ?: Int.MAX_VALUE }, { -it.productCount }))

            val brands = listedProductRepository.countByBrand(storefront.uid)
                .map { row ->
                    val name = row[0] as String
                    BrandMeta(
                        name = name,
                        productCount = (row[1] as Number).toInt(),
                        imageUrl = brandImages[name]?.imageUrl,
                        sortOrder = brandImages[name]?.sortOrder ?: 0,
                    )
                }
                .sortedWith(compareBy({ brandImages[it.name]?.sortOrder ?: Int.MAX_VALUE }, { -it.productCount }))

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

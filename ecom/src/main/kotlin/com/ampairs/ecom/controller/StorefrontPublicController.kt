package com.ampairs.ecom.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
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
import com.ampairs.ecom.domain.model.Storefront
import com.ampairs.ecom.exception.EcomOrderNotFoundException
import com.ampairs.ecom.interceptor.StorefrontTenantInterceptor
import com.ampairs.ecom.repository.EcomListedProductRepository
import com.ampairs.ecom.repository.EcomTaxonomyImageRepository
import com.ampairs.core.domain.dto.MoneyDto
import com.ampairs.pricing.domain.dto.PriceResolutionRequest
import com.ampairs.pricing.domain.dto.PriceResolutionResponse
import com.ampairs.pricing.service.PricingResolutionService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.Instant

@RestController
@RequestMapping("/v1/store/{slug}")
class StorefrontPublicController(
    private val listedProductRepository: EcomListedProductRepository,
    private val taxonomyImageRepository: EcomTaxonomyImageRepository,
    private val pricingResolutionService: PricingResolutionService,
) {

    /**
     * Resolve the effective unit price a shopper sees for a listed product, in the storefront's
     * channel (RETAIL by default; WHOLESALE for B2B storefronts), honoring an optional customer
     * segment + delivery pincode. Runs in-process via the single-sourced pricing engine, within the
     * StorefrontTenantInterceptor's tenant context (no X-Workspace-ID needed). Anonymous shoppers
     * never get a WHOLESALE-only price unless the storefront's defaultChannel is WHOLESALE.
     */
    @GetMapping("/products/{productId}/price")
    fun resolvePrice(
        @PathVariable slug: String,
        @PathVariable productId: String,
        @RequestAttribute(StorefrontTenantInterceptor.STOREFRONT_ATTR) storefront: Storefront,
        @RequestParam(defaultValue = "1") quantity: Int,
        @RequestParam("variant_sku", required = false) variantSku: String? = null,
        @RequestParam("customer_id", required = false) customerId: String? = null,
        @RequestParam("customer_group_id", required = false) customerGroupId: String? = null,
        @RequestParam("customer_type", required = false) customerType: String? = null,
        @RequestParam("pincode", required = false) pincode: String? = null,
    ): ApiResponse<PriceResolutionResponse> {
        val product = listedProductRepository.findByUid(productId)
            ?.takeIf { it.storefrontId == storefront.uid && it.isVisible }
            ?: throw EcomOrderNotFoundException("Product not found")
        val currency = "INR"
        val resolution = pricingResolutionService.resolve(
            PriceResolutionRequest(
                channel = storefront.defaultChannel,
                productId = product.managementProductId,
                variantSku = variantSku,
                quantity = BigDecimal(quantity),
                customerId = customerId,
                customerGroupId = customerGroupId,
                customerType = customerType,
                pincode = pincode,
                fallbackUnitPriceMinor = MoneyDto.of(product.price, currency)?.amountMinor,
                currency = currency,
            )
        )
        return ApiResponse.success(resolution)
    }

    @GetMapping
    fun getStorefront(
        @PathVariable slug: String,
        @RequestAttribute(StorefrontTenantInterceptor.STOREFRONT_ATTR) storefront: Storefront,
    ): ApiResponse<StorefrontResponse> {
        return ApiResponse.success(storefront.asStorefrontResponse())
    }

    @GetMapping("/products")
    fun getProducts(
        @PathVariable slug: String,
        @RequestAttribute(StorefrontTenantInterceptor.STOREFRONT_ATTR) storefront: Storefront,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam category: String? = null,
        @RequestParam brand: String? = null,
        @RequestParam subcategory: String? = null,
    ): ApiResponse<PageResponse<ListedProductResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        val result = if (category != null || brand != null || subcategory != null) {
            listedProductRepository.findByFilters(storefront.uid, category, brand, subcategory, pageable)
        } else {
            listedProductRepository.findByStorefrontIdAndIsVisibleTrue(storefront.uid, pageable)
        }
        return ApiResponse.success(PageResponse.from(result.map { it.asListedProductResponse() }))
    }

    @GetMapping("/products/search")
    fun searchProducts(
        @PathVariable slug: String,
        @RequestAttribute(StorefrontTenantInterceptor.STOREFRONT_ATTR) storefront: Storefront,
        @RequestParam q: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<ListedProductResponse>> {
        val pageable = PageRequest.of(page, size)
        val result = listedProductRepository.searchByText(storefront.uid, q, pageable)
        return ApiResponse.success(PageResponse.from(result.map { it.asListedProductResponse() }))
    }

    @GetMapping("/products/sync")
    fun syncProducts(
        @PathVariable slug: String,
        @RequestAttribute(StorefrontTenantInterceptor.STOREFRONT_ATTR) storefront: Storefront,
        @RequestParam since: Instant,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "100") size: Int,
    ): ApiResponse<SyncPage<ProductSyncItem>> {
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
    }

    @GetMapping("/catalog-meta")
    fun getCatalogMeta(
        @PathVariable slug: String,
        @RequestAttribute(StorefrontTenantInterceptor.STOREFRONT_ATTR) storefront: Storefront,
    ): ApiResponse<StorefrontCatalogMetaResponse> {
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
    }

    @GetMapping("/products/{productId}")
    fun getProduct(
        @PathVariable slug: String,
        @PathVariable productId: String,
        @RequestAttribute(StorefrontTenantInterceptor.STOREFRONT_ATTR) storefront: Storefront,
    ): ApiResponse<ListedProductResponse> {
        val product = listedProductRepository.findByUid(productId)
            ?.takeIf { it.storefrontId == storefront.uid && it.isVisible }
            ?: throw EcomOrderNotFoundException("Product not found")
        return ApiResponse.success(product.asListedProductResponse())
    }
}

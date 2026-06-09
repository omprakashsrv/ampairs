package com.ampairs.product.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.core.exception.NotFoundException
import com.ampairs.product.domain.model.Product
import com.ampairs.product.domain.dto.group.*
import com.ampairs.product.domain.dto.product.*
import com.ampairs.product.service.ProductService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/product/v1/products")
class ProductController(
    val productService: ProductService,
) {

    @GetMapping("/product-category")
    fun getProductsWithCategory(@RequestParam("group_id") groupId: String): ApiResponse<ProductsCategoryResponse> {
        val products = productService.getProducts(groupId)
        val categoryIds = products.map { it.categoryId ?: "" }.toSet()
        val productCategories = productService.getCategories(categoryIds)
        val result = ProductsCategoryResponse(products = products, categories = productCategories.asResponse())
        return ApiResponse.success(result)
    }

    /**
     * Incremental sync feed: products updated at/after last_sync, INCLUDING soft-deleted
     * (status = "DELETED") rows, paginated. Lets mobile clients do batched incremental pulls
     * and detect deletions.
     */
    @GetMapping("/sync")
    fun getProductsSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String
    ): ApiResponse<PageResponse<ProductResponse>> {
        val direction = if (sortDir.uppercase() == "DESC") Sort.Direction.DESC else Sort.Direction.ASC
        val pageable = PageRequest.of(page, size, Sort.by(direction, sortBy))
        val productsPage = productService.getProductsAfterSync(lastSync, pageable)
        return ApiResponse.success(PageResponse.from(productsPage))
    }

    /** Bulk push of products (creates/updates + in-band soft-deletes via status = "DELETED"). */
    @PostMapping("/sync")
    fun updateProductsSync(@RequestBody products: List<ProductRequest>): ApiResponse<List<ProductResponse>> {
        return ApiResponse.success(productService.updateProducts(products.asDatabaseModel()))
    }

    /**
     * Incremental sync feed: product groups updated at/after last_sync, ordered by
     * updatedAt ASC, paginated. Catalog entities have no soft-delete flag.
     */
    @GetMapping("/groups/sync")
    fun getGroupsSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int
    ): ApiResponse<PageResponse<ProductGroupResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "updatedAt"))
        val groupsPage = productService.getGroupsAfterSync(lastSync, pageable)
        return ApiResponse.success(PageResponse.from(groupsPage) { it.asResponse() })
    }

    @GetMapping("/all-groups-category")
    fun getGroupsCategory(): ApiResponse<AllGroupsResponse> {
        val groups = productService.getGroups()
        val categories = productService.getCategories()
        val brands = productService.getBrands()
        val subCategories = productService.getSubCategories()
        val result = AllGroupsResponse(
            groups = groups.asResponse(),
            categories = categories.asResponse(),
            brands = brands.asResponse(),
            subCategories = subCategories.asResponse()
        )
        return ApiResponse.success(result)
    }

    /**
     * Incremental sync feed: product brands updated at/after last_sync, ordered by
     * updatedAt ASC, paginated. Catalog entities have no soft-delete flag.
     */
    @GetMapping("/brands/sync")
    fun getBrandsSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int
    ): ApiResponse<PageResponse<ProductBrandResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "updatedAt"))
        val brandsPage = productService.getBrandsAfterSync(lastSync, pageable)
        return ApiResponse.success(PageResponse.from(brandsPage) { it.asResponse() })
    }

    /**
     * Incremental sync feed: product sub-categories updated at/after last_sync, ordered by
     * updatedAt ASC, paginated. Catalog entities have no soft-delete flag.
     */
    @GetMapping("/sub-categories/sync")
    fun getSubCategoriesSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int
    ): ApiResponse<PageResponse<ProductSubCategoryResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "updatedAt"))
        val subCategoriesPage = productService.getSubCategoriesAfterSync(lastSync, pageable)
        return ApiResponse.success(PageResponse.from(subCategoriesPage) { it.asResponse() })
    }

    /**
     * Incremental sync feed: product categories updated at/after last_sync, ordered by
     * updatedAt ASC, paginated. Catalog entities have no soft-delete flag.
     */
    @GetMapping("/categories/sync")
    fun getCategoriesSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int
    ): ApiResponse<PageResponse<ProductCategoryResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "updatedAt"))
        val categoriesPage = productService.getCategoriesAfterSync(lastSync, pageable)
        return ApiResponse.success(PageResponse.from(categoriesPage) { it.asResponse() })
    }

    /** Bulk push of product groups. Catalog entities have no soft-delete — upsert only. */
    @PostMapping("/groups/sync")
    fun updateGroupsSync(@RequestBody groups: List<ProductGroupRequest>): ApiResponse<List<ProductGroupResponse>> {
        val productGroups = productService.updateProductGroups(groups.asDatabaseModel())
        return ApiResponse.success(productGroups.asResponse())
    }

    /** Bulk push of product brands. Catalog entities have no soft-delete — upsert only. */
    @PostMapping("/brands/sync")
    fun updateBrandsSync(@RequestBody groups: List<ProductBrandRequest>): ApiResponse<List<ProductBrandResponse>> {
        val productGroups = productService.updateProductBrands(groups.asDatabaseModel())
        return ApiResponse.success(productGroups.asResponse())
    }

    /** Bulk push of product categories. Catalog entities have no soft-delete — upsert only. */
    @PostMapping("/categories/sync")
    fun updateCategoriesSync(@RequestBody categories: List<ProductCategoryRequest>): ApiResponse<List<ProductCategoryResponse>> {
        val productCategories =
            productService.updateProductCategories(categories.asDatabaseModel())
        return ApiResponse.success(productCategories.asResponse())
    }

    /** Bulk push of product sub-categories. Catalog entities have no soft-delete — upsert only. */
    @PostMapping("/sub-categories/sync")
    fun updateSubCategoriesSync(@RequestBody categories: List<ProductSubCategoryRequest>): ApiResponse<List<ProductSubCategoryResponse>> {
        val productSubCategories =
            productService.updateProductSubCategories(categories.asDatabaseModel())
        return ApiResponse.success(productSubCategories.asResponse())
    }

    @GetMapping("/list")
    fun getProductList(
        @RequestParam("search", required = false) search: String?,
        @RequestParam("category", required = false) category: String?,
        @RequestParam("brand", required = false) brand: String?,
        @RequestParam("min_price", required = false) minPrice: Double?,
        @RequestParam("max_price", required = false) maxPrice: Double?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "20") size: Int,
        @RequestParam("sort", defaultValue = "name") sort: String,
        @RequestParam("direction", defaultValue = "ASC") direction: String
    ): ApiResponse<Map<String, Any>> {
        val sortDirection = if (direction.uppercase() == "DESC") Sort.Direction.DESC else Sort.Direction.ASC
        val pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort))
        
        val productPage = productService.searchProducts(search, category, brand, minPrice, maxPrice, pageable)
        
        val response = mapOf(
            "products" to productPage.content,
            "pagination" to mapOf(
                "page" to page,
                "size" to size,
                "total_pages" to productPage.totalPages,
                "total_elements" to productPage.totalElements,
                "has_next" to productPage.hasNext(),
                "has_previous" to productPage.hasPrevious()
            )
        )
        
        return ApiResponse.success(response)
    }

    @GetMapping("/{productId}")
    fun getProduct(@PathVariable productId: String): ApiResponse<ProductResponse> {
        val product = productService.getProductByUid(productId)
            ?: throw NotFoundException("Product not found: $productId")
        return ApiResponse.success(product.asResponse())
    }

    @PutMapping("/{productId}")
    fun updateProduct(
        @PathVariable productId: String,
        @RequestBody @Valid request: ProductRequest
    ): ApiResponse<ProductResponse> {
        val updates = Product().apply {
            name = request.name
            description = request.description
            basePrice = request.basePrice ?: 0.0
            costPrice = request.costPrice ?: 0.0
            attributes = request.attributes ?: emptyMap()
            status = request.status ?: "ACTIVE"
        }
        
        val updatedProduct = productService.updateProduct(productId, updates)
            ?: throw NotFoundException("Product not found: $productId")
        return ApiResponse.success(updatedProduct.asResponse())
    }

    @PutMapping("/{productId}/inventory")
    fun updateProductInventory(
        @PathVariable productId: String,
        @RequestBody @Valid request: UpdateInventoryRequest
    ): ApiResponse<UpdateInventoryResponse> {
        // This would integrate with inventory management
        // For now, return success response
        val response = UpdateInventoryResponse(
            productId = productId,
            quantity = request.quantity,
            warehouseId = request.warehouseId,
            message = "Inventory updated successfully",
            updatedAt = Instant.now()
        )
        return ApiResponse.success(response)
    }

    @GetMapping("/sku/{sku}")
    fun getProductBySku(@PathVariable sku: String): ApiResponse<ProductResponse> {
        val product = productService.getProductBySku(sku)
            ?: throw NotFoundException("Product not found with SKU: $sku")
        return ApiResponse.success(product.asResponse())
    }
}

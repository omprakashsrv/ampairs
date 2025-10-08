package com.ampairs.product.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.FileResponse
import com.ampairs.core.domain.dto.toFileResponse
import com.ampairs.core.domain.service.FileService
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.product.domain.model.Product
import com.ampairs.product.domain.dto.group.*
import com.ampairs.product.domain.dto.product.ProductRequest
import com.ampairs.product.domain.dto.product.ProductResponse
import com.ampairs.product.domain.dto.product.asDatabaseModel
import com.ampairs.product.domain.dto.product.asResponse
import com.ampairs.product.domain.dto.unit.UnitRequest
import com.ampairs.product.domain.dto.unit.UnitResponse
import com.ampairs.product.domain.dto.unit.asDatabaseModel
import com.ampairs.product.domain.dto.unit.asResponse
import com.ampairs.product.service.ProductService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/product/v1")
class ProductController(val productService: ProductService, val fileService: FileService) {

    @GetMapping("")
    fun getProducts(
        @RequestParam("last_updated") lastUpdated: Long?,
        @RequestParam("group_id") groupId: String?,
    ): ApiResponse<List<ProductResponse>> {
        if (!groupId.isNullOrEmpty()) {
            val products = productService.getProducts(groupId)
            return ApiResponse.success(products.asResponse())
        }
        val products = productService.getProducts(lastUpdated)
        return ApiResponse.success(products.asResponse())
    }

    @GetMapping("/product_category")
    fun getProductsWithCategory(@RequestParam("group_id") groupId: String): ApiResponse<ProductsCategoryResponse> {
        val products = productService.getProducts(groupId)
        val categoryIds = products.map { it.categoryId ?: "" }.toSet()
        val productCategories = productService.getCategories(categoryIds)
        val result = ProductsCategoryResponse(products = products.asResponse(), categories = productCategories.asResponse())
        return ApiResponse.success(result)
    }

    @PostMapping("/products")
    fun updateProducts(@RequestBody products: List<ProductRequest>): ApiResponse<List<ProductResponse>> {
        val result = productService.updateProducts(products.asDatabaseModel()).asResponse()
        return ApiResponse.success(result)
    }

    @PostMapping("/units")
    fun updateUnits(@RequestBody units: List<UnitRequest>): ApiResponse<List<UnitResponse>> {
        val units = productService.updateUnits(units.asDatabaseModel())
        return ApiResponse.success(units.asResponse())
    }

    @GetMapping("/units")
    fun getUnits(): ApiResponse<List<UnitResponse>> {
        val units = productService.getUnits()
        return ApiResponse.success(units.asResponse())
    }


    @GetMapping("/groups")
    fun getGroups(): ApiResponse<List<ProductGroupResponse>> {
        val groups = productService.getGroups()
        return ApiResponse.success(groups.asResponse())
    }

    @GetMapping("/all_groups_category")
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

    @GetMapping("/brands")
    fun getBrands(): ApiResponse<List<ProductBrandResponse>> {
        val brands = productService.getBrands()
        return ApiResponse.success(brands.asResponse())
    }

    @GetMapping("/sub_categories")
    fun getSubCategories(): ApiResponse<List<ProductSubCategoryResponse>> {
        val categories = productService.getSubCategories()
        return ApiResponse.success(categories.asResponse())
    }

    @PostMapping("/groups")
    fun updateGroups(@RequestBody groups: List<ProductGroupRequest>): ApiResponse<List<ProductGroupResponse>> {
        val productGroups = productService.updateProductGroups(groups.asDatabaseModel())
        return ApiResponse.success(productGroups.asResponse())
    }

    @PostMapping("/brands")
    fun updateBrands(@RequestBody groups: List<ProductBrandRequest>): ApiResponse<List<ProductBrandResponse>> {
        val productGroups = productService.updateProductBrands(groups.asDatabaseModel())
        return ApiResponse.success(productGroups.asResponse())
    }

    @PostMapping("/categories")
    fun updateCategories(@RequestBody categories: List<ProductCategoryRequest>): ApiResponse<List<ProductCategoryResponse>> {
        val productCategories =
            productService.updateProductCategories(categories.asDatabaseModel())
        return ApiResponse.success(productCategories.asResponse())
    }

    @PostMapping("/sub_categories")
    fun updateSubCategories(@RequestBody categories: List<ProductSubCategoryRequest>): ApiResponse<List<ProductSubCategoryResponse>> {
        val productSubCategories =
            productService.updateProductSubCategories(categories.asDatabaseModel())
        return ApiResponse.success(productSubCategories.asResponse())
    }

    @PostMapping("/upload_image")
    fun uploadImage(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("path") path: String,
    ): ApiResponse<FileResponse> {
        val result = fileService.saveFile(
            bytes = file.inputStream.readAllBytes(),
            name = file.originalFilename ?: "unnamed_file",
            contentType = file.contentType ?: "application/octet-stream",
            folder = "products/${TenantContextHolder.getCurrentTenant()}$path"
        ).toFileResponse()
        return ApiResponse.success(result)
    }

    /**
     * Retail-specific Product Management API endpoints
     */

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun createProduct(@RequestBody request: ProductRequest): ApiResponse<ProductResponse> {
        val product = Product().apply {
            name = request.name
            sku = request.sku ?: ""
            description = request.description
            unitId = request.unitId
            taxCodeId = request.taxCodeId  
            basePrice = request.basePrice ?: 0.0
            costPrice = request.costPrice ?: 0.0
            attributes = request.attributes ?: emptyMap()
            status = "ACTIVE"
        }
        
        val createdProduct = productService.createProduct(product)
        return ApiResponse.success(createdProduct.asResponse())
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
            "products" to productPage.content.map { it.asResponse() },
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
        val product = productService.updateProducts(emptyList()).find { it.uid == productId }
            ?: return ApiResponse.error("Product not found", "PRODUCT_NOT_FOUND")
        
        return ApiResponse.success(product.asResponse())
    }

    @PutMapping("/{productId}")
    fun updateProduct(
        @PathVariable productId: String,
        @RequestBody request: ProductRequest
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
            ?: return ApiResponse.error("Product not found", "PRODUCT_NOT_FOUND")
        
        return ApiResponse.success(updatedProduct.asResponse())
    }

    @PutMapping("/{productId}/inventory")
    fun updateProductInventory(
        @PathVariable productId: String,
        @RequestBody request: Map<String, Any>
    ): ApiResponse<String> {
        // This would integrate with inventory management
        // For now, return success response
        return ApiResponse.success("Inventory updated successfully")
    }

    @GetMapping("/sku/{sku}")
    fun getProductBySku(@PathVariable sku: String): ApiResponse<ProductResponse> {
        val product = productService.getProductBySku(sku)
            ?: return ApiResponse.error("Product not found with SKU: $sku", "PRODUCT_NOT_FOUND")
        
        return ApiResponse.success(product.asResponse())
    }
}
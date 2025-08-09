package com.ampairs.product.controller

import com.ampairs.core.domain.dto.FileResponse
import com.ampairs.core.domain.dto.toFileResponse
import com.ampairs.core.domain.service.FileService
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.product.domain.dto.group.*
import com.ampairs.product.domain.dto.product.ProductRequest
import com.ampairs.product.domain.dto.product.ProductResponse
import com.ampairs.product.domain.dto.product.asDatabaseModel
import com.ampairs.product.domain.dto.product.asResponse
import com.ampairs.product.domain.dto.tax.TaxCodeRequest
import com.ampairs.product.domain.dto.tax.TaxCodeResponse
import com.ampairs.product.domain.dto.tax.asDatabaseModel
import com.ampairs.product.domain.dto.tax.asResponse
import com.ampairs.product.domain.dto.unit.UnitRequest
import com.ampairs.product.domain.dto.unit.UnitResponse
import com.ampairs.product.domain.dto.unit.asDatabaseModel
import com.ampairs.product.domain.dto.unit.asResponse
import com.ampairs.product.service.ProductService
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/product/v1")
class ProductController(val productService: ProductService, val fileService: FileService) {

    @GetMapping("")
    fun getProducts(
        @RequestParam("last_updated") lastUpdated: Long?,
        @RequestParam("group_id") groupId: String?,
    ): List<ProductResponse> {
        if (!groupId.isNullOrEmpty()) {
            val products = productService.getProducts(groupId)
            return products.asResponse()
        }
        val products = productService.getProducts(lastUpdated)
        return products.asResponse()
    }

    @GetMapping("/product_category")
    fun getProductsWithCategory(@RequestParam("group_id") groupId: String): ProductsCategoryResponse {
        val products = productService.getProducts(groupId)
        val categoryIds = products.map { it.categoryId ?: "" }.toSet()
        val productCategories = productService.getCategories(categoryIds)
        return ProductsCategoryResponse(products = products.asResponse(), categories = productCategories.asResponse())
    }

    @PostMapping("/products")
    fun updateProducts(@RequestBody products: List<ProductRequest>): List<ProductResponse> {
        return productService.updateProducts(products.asDatabaseModel()).asResponse()
    }

    @PostMapping("/units")
    fun updateUnits(@RequestBody units: List<UnitRequest>): List<UnitResponse> {
        val units = productService.updateUnits(units.asDatabaseModel())
        return units.asResponse()
    }

    @GetMapping("/units")
    fun updateTaxCodes(): List<UnitResponse> {
        val units = productService.getUnits()
        return units.asResponse()
    }


    @GetMapping("/groups")
    fun getGroups(): List<ProductGroupResponse> {
        val groups = productService.getGroups()
        return groups.asResponse()
    }

    @GetMapping("/all_groups_category")
    fun getGroupsCategory(): AllGroupsResponse {
        val groups = productService.getGroups()
        val categories = productService.getCategories()
        val brands = productService.getBrands()
        val subCategories = productService.getSubCategories()
        return AllGroupsResponse(
            groups = groups.asResponse(),
            categories = categories.asResponse(),
            brands = brands.asResponse(),
            subCategories = subCategories.asResponse()
        )
    }

    @GetMapping("/brands")
    fun getBrands(): List<ProductBrandResponse> {
        val brands = productService.getBrands()
        return brands.asResponse()
    }

    @GetMapping("/sub_categories")
    fun getSubCategories(): List<ProductSubCategoryResponse> {
        val categories = productService.getSubCategories()
        return categories.asResponse()
    }

    @PostMapping("/groups")
    fun updateGroups(@RequestBody groups: List<ProductGroupRequest>): List<ProductGroupResponse> {
        val productGroups = productService.updateProductGroups(groups.asDatabaseModel())
        return productGroups.asResponse()
    }

    @PostMapping("/brands")
    fun updateBrands(@RequestBody groups: List<ProductBrandRequest>): List<ProductBrandResponse> {
        val productGroups = productService.updateProductBrands(groups.asDatabaseModel())
        return productGroups.asResponse()
    }

    @PostMapping("/categories")
    fun updateCategories(@RequestBody categories: List<ProductCategoryRequest>): List<ProductCategoryResponse> {
        val productCategories =
            productService.updateProductCategories(categories.asDatabaseModel())
        return productCategories.asResponse()
    }

    @PostMapping("/sub_categories")
    fun updateSubCategories(@RequestBody categories: List<ProductSubCategoryRequest>): List<ProductSubCategoryResponse> {
        val productSubCategories =
            productService.updateProductSubCategories(categories.asDatabaseModel())
        return productSubCategories.asResponse()
    }

    @PostMapping("/tax_codes")
    fun updateTaxCodes(@RequestBody codes: List<TaxCodeRequest>): List<TaxCodeResponse> {
        val taxCodes = productService.updateTaxCodes(codes.asDatabaseModel())
        return taxCodes.asResponse()
    }

    @PostMapping("/upload_image")
    fun uploadImage(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("path") path: String,
    ): FileResponse {
        return fileService.saveFile(
            bytes = file.inputStream.readAllBytes(),
            name = file.originalFilename ?: "unnamed_file",
            contentType = file.contentType ?: "application/octet-stream",
            folder = "products/${TenantContextHolder.getCurrentTenant()}$path"
        ).toFileResponse()
    }
}
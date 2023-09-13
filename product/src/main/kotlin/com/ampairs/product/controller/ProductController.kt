package com.ampairs.product.controller

import com.ampairs.core.user.model.SessionUser
import com.ampairs.product.domain.dto.*
import com.ampairs.product.service.ProductService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/product/v1")
class ProductController(val productService: ProductService) {

    @GetMapping("")
    fun getProducts(@RequestParam("last_updated") lastUpdated: Long?): List<ProductResponse> {
        val products = productService.getProducts(lastUpdated)
        return products.asResponse()
    }

    @GetMapping("/product_category")
    fun getProducts(@RequestParam("group_id") groupId: String): ProductsCategoryResponse {
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

    @GetMapping("/groups")
    fun getGroups(): List<ProductGroupResponse> {
        val groups = productService.getGroups()
        return groups.asResponse()
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

    @PostMapping("/product_groups")
    fun updateGroups(@RequestBody groups: List<ProductGroupRequest>): List<ProductGroupResponse> {
        val sessionUser: SessionUser = SecurityContextHolder.getContext().authentication.principal as SessionUser
        val productGroups = productService.updateProductGroups(sessionUser.company.id, groups.asDatabaseModel())
        return productGroups.asResponse()
    }

    @PostMapping("/product_categories")
    fun updateCategories(@RequestBody categories: List<ProductCategoryRequest>): List<ProductCategoryResponse> {
        val sessionUser: SessionUser = SecurityContextHolder.getContext().authentication.principal as SessionUser
        val productCategories =
            productService.updateProductCategories(sessionUser.company.id, categories.asDatabaseModel())
        return productCategories.asResponse()
    }

    @PostMapping("/tax_codes")
    fun updateTaxCodes(@RequestBody codes: List<TaxCodeRequest>): List<TaxCodeResponse> {
        val taxCodes = productService.updateTaxCodes(codes.asDatabaseModel())
        return taxCodes.asResponse()
    }
}
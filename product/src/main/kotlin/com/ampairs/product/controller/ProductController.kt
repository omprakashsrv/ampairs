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
        val sessionUser: SessionUser = SecurityContextHolder.getContext().authentication.principal as SessionUser
        val products = productService.getProducts(sessionUser.company.id, lastUpdated)
        return products.asResponse()
    }

    @PostMapping("/units")
    fun updateUnits(units: List<UnitRequest>): List<UnitResponse> {
        val sessionUser: SessionUser = SecurityContextHolder.getContext().authentication.principal as SessionUser
        val units = productService.updateUnits(sessionUser.company.id, units.asDatabaseModel())
        return units.asResponse()
    }

    @PostMapping("/product_groups")
    fun updateGroups(groups: List<ProductGroupRequest>): List<ProductGroupResponse> {
        val sessionUser: SessionUser = SecurityContextHolder.getContext().authentication.principal as SessionUser
        val productGroups = productService.updateProductGroups(sessionUser.company.id, groups.asDatabaseModel())
        return productGroups.asResponse()
    }

    @PostMapping("/product_categories")
    fun updateCategories(categories: List<ProductCategoryRequest>): List<ProductCategoryResponse> {
        val sessionUser: SessionUser = SecurityContextHolder.getContext().authentication.principal as SessionUser
        val productCategories =
            productService.updateProductCategories(sessionUser.company.id, categories.asDatabaseModel())
        return productCategories.asResponse()
    }

    @PostMapping("/tax_codes")
    fun updateTaxCodes(codes: List<TaxCodeRequest>): List<TaxCodeResponse> {
        val sessionUser: SessionUser = SecurityContextHolder.getContext().authentication.principal as SessionUser
        val taxCodes = productService.updateTaxCodes(codes.asDatabaseModel())
        return taxCodes.asResponse()
    }
}
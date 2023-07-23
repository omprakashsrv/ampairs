package com.ampairs.product.controller

import com.ampairs.core.user.model.SessionUser
import com.ampairs.product.domain.dto.*
import com.ampairs.product.service.ProductService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/product/v1")
class ProductController constructor(val productService: ProductService) {

    @GetMapping("")
    fun getProducts(@RequestParam("last_updated") lastUpdated: Long?): List<ProductResponse> {
        val sessionUser: SessionUser = SecurityContextHolder.getContext().authentication.principal as SessionUser
        val products = productService.getProducts(sessionUser.company.id, lastUpdated)
        return products.asProductResponse()
    }

    @PostMapping("/unit")
    fun importMasters(units: List<UnitRequest>): List<UnitResponse> {
        val sessionUser: SessionUser = SecurityContextHolder.getContext().authentication.principal as SessionUser
        val units = productService.updateUnits(sessionUser.company.id, units.asDatabaseModel())
        return units.asUnitResponse()
    }
}
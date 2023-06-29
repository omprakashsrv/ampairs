package com.ampairs.product.service

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/product/v1")
class ProductService {

    @GetMapping("")
    fun getProducts(@RequestParam("last_updated") lastUpdated: Long?) {
        TODO()
    }
}
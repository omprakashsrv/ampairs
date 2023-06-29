package com.ampairs.product.service

import com.ampairs.product.domain.model.Product
import com.ampairs.product.repository.ProductPagingRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service


@Service
class ProductService(
    val productPagingRepository: ProductPagingRepository
) {

    fun getProducts(ownerId: String, lastUpdated: Long?): List<Product> {
        return productPagingRepository.findAllByOwnerIdAndLastUpdatedGreaterThanEqual(
            ownerId,
            lastUpdated ?: 0,
            Sort.by("lastUpdated").ascending()
        )
    }
}
package com.ampairs.product.repository

import com.ampairs.product.domain.Product
import org.springframework.data.repository.CrudRepository

interface StockItemRepository : CrudRepository<Product, String> {

}
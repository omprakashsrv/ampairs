package com.ampairs.product.repository

import com.ampairs.product.domain.ProductCategory
import org.springframework.data.repository.CrudRepository

interface StockCategoryRepository : CrudRepository<ProductCategory, String>{

}
package com.ampairs.product.repository

import com.ampairs.product.domain.ProductGroup
import org.springframework.data.repository.CrudRepository

interface StockGroupRepository : CrudRepository<ProductGroup, String>{

}
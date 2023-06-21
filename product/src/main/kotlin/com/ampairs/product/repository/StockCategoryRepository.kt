package com.ampairs.product.repository

import com.ampairs.product.domain.StockCategory
import org.springframework.data.repository.CrudRepository

interface StockCategoryRepository : CrudRepository<StockCategory, String>
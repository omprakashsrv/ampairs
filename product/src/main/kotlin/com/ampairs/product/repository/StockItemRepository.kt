package com.ampairs.product.repository

import com.ampairs.product.domain.StockItem
import org.springframework.data.repository.CrudRepository

interface StockItemRepository : CrudRepository<StockItem, String>
package com.ampairs.product.repository

import com.ampairs.product.domain.StockGroup
import org.springframework.data.repository.CrudRepository

interface StockGroupRepository : CrudRepository<StockGroup, String>
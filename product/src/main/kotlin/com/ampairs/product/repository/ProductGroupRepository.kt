package com.ampairs.product.repository

import com.ampairs.product.domain.model.ProductGroup
import org.springframework.data.repository.CrudRepository

interface ProductGroupRepository : CrudRepository<ProductGroup, String>{

}
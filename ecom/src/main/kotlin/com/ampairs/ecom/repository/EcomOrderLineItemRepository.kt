package com.ampairs.ecom.repository

import com.ampairs.ecom.domain.model.EcomOrderLineItem
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface EcomOrderLineItemRepository : CrudRepository<EcomOrderLineItem, Long> {
    fun findByEcomOrderId(ecomOrderId: String): List<EcomOrderLineItem>
}

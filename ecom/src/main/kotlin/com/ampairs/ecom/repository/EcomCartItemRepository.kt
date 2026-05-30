package com.ampairs.ecom.repository

import com.ampairs.ecom.domain.model.EcomCartItem
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface EcomCartItemRepository : CrudRepository<EcomCartItem, Long> {
    fun findByCartId(cartId: String): List<EcomCartItem>
    fun findByCartIdAndListedProductId(cartId: String, listedProductId: String): EcomCartItem?
    fun deleteByCartId(cartId: String)
}

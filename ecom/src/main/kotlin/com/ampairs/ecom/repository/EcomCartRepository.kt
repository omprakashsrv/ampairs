package com.ampairs.ecom.repository

import com.ampairs.ecom.domain.enums.CartStatus
import com.ampairs.ecom.domain.model.EcomCart
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface EcomCartRepository : CrudRepository<EcomCart, Long> {
    @EntityGraph("EcomCart.withItems")
    fun findBySessionToken(sessionToken: String): EcomCart?

    fun findByCustomerIdAndStorefrontIdAndStatus(
        customerId: String,
        storefrontId: String,
        status: CartStatus,
    ): EcomCart?

    fun findByExpiresAtBefore(expiresAt: Instant): List<EcomCart>
}

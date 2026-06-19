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

    /**
     * Write-path lookup WITHOUT the items entity graph. Mutating cart items while the cart's
     * read-only `@OneToMany` collection is initialized triggers a TransientPropertyValueException
     * on autoflush. The write methods load the cart through this, mutate items via the item
     * repository, flush+clear, then re-read through [findBySessionToken] for the response.
     */
    fun findFirstBySessionToken(sessionToken: String): EcomCart?

    fun findByCustomerIdAndStorefrontIdAndStatus(
        customerId: String,
        storefrontId: String,
        status: CartStatus,
    ): EcomCart?

    fun findByExpiresAtBefore(expiresAt: Instant): List<EcomCart>
}

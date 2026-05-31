package com.ampairs.ecom.domain.model

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.ecom.domain.enums.CartStatus
import jakarta.persistence.*
import java.time.Instant

@NamedEntityGraph(
    name = "EcomCart.withItems",
    attributeNodes = [NamedAttributeNode("cartItems")]
)
@Entity
@Table(
    name = "ecom_cart",
    indexes = [
        Index(name = "idx_ecom_cart_session",    columnList = "session_token"),
        Index(name = "idx_ecom_cart_customer",   columnList = "customer_id"),
        Index(name = "idx_ecom_cart_storefront", columnList = "storefront_id"),
        Index(name = "idx_ecom_cart_expires",    columnList = "expires_at"),
    ]
)
class EcomCart : BaseDomain() {

    @Column(name = "storefront_id", nullable = false, length = 200)
    var storefrontId: String = ""

    @Column(name = "customer_id", length = 200)
    var customerId: String? = null

    @Column(name = "session_token", nullable = false, length = 200, unique = true)
    var sessionToken: String = ""

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.now()

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: CartStatus = CartStatus.ACTIVE

    @OneToMany
    @JoinColumn(name = "cart_id", referencedColumnName = "uid", insertable = false, updatable = false)
    var cartItems: MutableList<EcomCartItem> = mutableListOf()

    override fun obtainSeqIdPrefix(): String = "CRT"
}

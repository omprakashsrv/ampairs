package com.ampairs.customer.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity

/**
 * A person linked to a CRM [Customer] account, optionally tied to an ecom storefront login
 * ([ecomUserId]). Models many users → one customer (owner/manager/worker of one account) and one
 * user → many customers (a buyer who orders for several accounts), so a storefront order can resolve
 * to — or let the buyer select — the right CRM account. [ecomUserId] is null for contacts that have
 * no app login (recorded from the CRM side).
 */
@Entity(name = "customer_contact")
class CustomerContact : OwnableBaseDomain() {

    @Column(name = "customer_id", nullable = false, length = 200)
    var customerId: String = ""

    @Column(name = "ecom_user_id", length = 200)
    var ecomUserId: String? = null

    @Column(name = "name", nullable = false, length = 255)
    var name: String = ""

    @Column(name = "phone", length = 20)
    var phone: String? = null

    @Column(name = "email", length = 255)
    var email: String? = null

    @Column(name = "role", nullable = false, length = 40)
    var role: String = "OWNER"

    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false

    @Column(name = "status", nullable = false, length = 20)
    var status: String = "ACTIVE"

    override fun obtainSeqIdPrefix(): String = "CCT"
}

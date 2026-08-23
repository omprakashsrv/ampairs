package com.ampairs.ecom.repository

import com.ampairs.ecom.domain.enums.EcomOrderStatus
import com.ampairs.ecom.domain.model.EcomOrder
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository

@Repository
interface EcomOrderRepository :
    CrudRepository<EcomOrder, Long>,
    PagingAndSortingRepository<EcomOrder, Long> {

    @EntityGraph("EcomOrder.withItems")
    fun findByEcomOrderRef(ecomOrderRef: String): EcomOrder?

    @EntityGraph("EcomOrder.withItems")
    fun findBySourceCartToken(sourceCartToken: String): EcomOrder?

    fun findByCustomerIdAndStorefrontId(customerId: String, storefrontId: String, pageable: Pageable): Page<EcomOrder>

    /**
     * Spec 029 — invoice→order reverse link: map a workspace order uid (`Invoice.orderRefId`) back to
     * the ecom order so the buyer-facing storefront ref can be surfaced. @TenantId-filtered.
     */
    fun findByManagementOrderRef(managementOrderRef: String): EcomOrder?

    /** Batched form of [findByManagementOrderRef] — one query for a page of invoices (avoids N+1). @TenantId-filtered. */
    fun findByManagementOrderRefIn(managementOrderRefs: Collection<String>): List<EcomOrder>

    fun findByWorkspaceId(workspaceId: String, pageable: Pageable): Page<EcomOrder>

    fun findByWorkspaceIdAndStatus(workspaceId: String, status: EcomOrderStatus, pageable: Pageable): Page<EcomOrder>
}

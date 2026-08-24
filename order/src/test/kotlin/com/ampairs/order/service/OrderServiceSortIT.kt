package com.ampairs.order.service

import com.ampairs.AmpairsApplication
import com.ampairs.core.multitenancy.TenantContextHolder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * Regression for the `Sort.by("lastUpdated")` typo in [OrderService.getOrders] — the twin of the
 * invoice bug. `Order` has no `lastUpdated` property (timestamp is `updatedAt`), so the derived query
 * threw `PropertyReferenceException` at query-build time and 500-ed the owner order pull.
 *
 * Like the invoice case, a mock can't catch it — the Sort path resolves only against the real JPA
 * metamodel. Boots the context and builds+runs the derived query (empty DB is enough).
 */
@SpringBootTest(classes = [AmpairsApplication::class])
@ActiveProfiles("test")
class OrderServiceSortIT {

    @Autowired private lateinit var orderService: OrderService

    @AfterEach
    fun cleanup() = TenantContextHolder.clearTenantContext()

    @Test
    fun `getOrders builds and runs its derived query with a valid Sort property`() {
        TenantContextHolder.setCurrentTenant("tenant-sort-it")
        assertNotNull(orderService.getOrders(null))
    }
}

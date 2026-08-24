package com.ampairs.invoice.service

import com.ampairs.AmpairsApplication
import com.ampairs.core.multitenancy.TenantContextHolder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * Regression for the `Sort.by("lastUpdated")` typo in [InvoiceService.getInvoices]. `Invoice` has no
 * `lastUpdated` property — its timestamp is `updatedAt` — so the derived query threw
 * `PropertyReferenceException` at query-build time and 500-ed every caller (the owner invoice pull and
 * the ecom buyer open-bills/outstanding path via `OutstandingService`).
 *
 * A Mockito unit test can't catch this: the Sort property path is resolved only by Spring Data against
 * the real JPA metamodel. This boots the context and actually builds+runs the derived query — an empty
 * DB is enough, since the failure was at query BUILD time, before any row is read.
 */
@SpringBootTest(classes = [AmpairsApplication::class])
@ActiveProfiles("test")
class InvoiceServiceSortIT {

    @Autowired private lateinit var invoiceService: InvoiceService

    @AfterEach
    fun cleanup() = TenantContextHolder.clearTenantContext()

    @Test
    fun `getInvoices builds and runs its derived query with a valid Sort property`() {
        TenantContextHolder.setCurrentTenant("tenant-sort-it")
        assertNotNull(invoiceService.getInvoices(null))
    }
}

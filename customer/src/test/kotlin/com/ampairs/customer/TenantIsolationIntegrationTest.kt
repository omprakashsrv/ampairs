package com.ampairs.customer

import com.ampairs.AmpairsApplication
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.repository.CustomerRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * Proves Hibernate `@TenantId` (OwnableBaseDomain.ownerId) actually isolates tenants at the
 * persistence layer — the single most important invariant in a multi-tenant SaaS, and one that
 * was previously untested. Uses the real CustomerRepository against H2 so the tenant predicate
 * Hibernate appends to every query is exercised end-to-end (not mocked).
 *
 * Deliberately NOT @Transactional: Hibernate binds the tenant identifier to the Session when it
 * opens. A test-managed transaction opens its Session before the test body sets the tenant, so it
 * would bind to "default" and every save would fail the "assigned tenant id differs" check. Letting
 * each repository call run in its own transaction binds the tenant set immediately before it —
 * which is also exactly how a real per-request flow behaves. Rows are cleaned up explicitly.
 */
@SpringBootTest(classes = [AmpairsApplication::class])
@ActiveProfiles("test")
class TenantIsolationIntegrationTest {

    @Autowired
    private lateinit var customerRepository: CustomerRepository

    private val createdRows = mutableListOf<Pair<String, String>>() // tenant -> uid

    @AfterEach
    fun cleanup() {
        createdRows.forEach { (tenant, uid) ->
            TenantContextHolder.setCurrentTenant(tenant)
            customerRepository.findByUid(uid)?.let { customerRepository.delete(it) }
        }
        createdRows.clear()
        TenantContextHolder.clearTenantContext()
    }

    @Test
    fun `repository queries only return rows owned by the current tenant`() {
        val alpha = saveAs(TENANT_A, "alpha")
        val bravo = saveAs(TENANT_B, "bravo")

        // The @TenantId column is stamped from the resolver on persist.
        assertEquals(TENANT_A, alpha.ownerId)
        assertEquals(TENANT_B, bravo.ownerId)

        // As tenant B: every visible row is B's, A's row is present in DB but invisible here.
        TenantContextHolder.setCurrentTenant(TENANT_B)
        val seenByB = customerRepository.findAll().toList()
        assertTrue(seenByB.isNotEmpty() && seenByB.all { it.ownerId == TENANT_B },
            "tenant B must only ever see its own rows")
        assertTrue(seenByB.any { it.uid == bravo.uid })
        assertNull(customerRepository.findByUid(alpha.uid),
            "tenant B must NOT resolve tenant A's customer by uid")

        // As tenant A: symmetric.
        TenantContextHolder.setCurrentTenant(TENANT_A)
        val seenByA = customerRepository.findAll().toList()
        assertTrue(seenByA.isNotEmpty() && seenByA.all { it.ownerId == TENANT_A },
            "tenant A must only ever see its own rows")
        assertNull(customerRepository.findByUid(bravo.uid),
            "tenant A must NOT resolve tenant B's customer by uid")
    }

    @Test
    fun `a tenant cannot reach another tenants row to mutate it`() {
        val alpha = saveAs(TENANT_A, "alpha")

        // Tenant B cannot even load A's row, so it can never update or delete it.
        TenantContextHolder.setCurrentTenant(TENANT_B)
        assertNull(customerRepository.findByUid(alpha.uid))

        // B's own write stays in B's partition.
        val bravo = saveAs(TENANT_B, "bravo")
        TenantContextHolder.setCurrentTenant(TENANT_A)
        assertNull(customerRepository.findByUid(bravo.uid))
        assertTrue(customerRepository.findAll().all { it.ownerId == TENANT_A })
    }

    private fun saveAs(tenant: String, name: String): Customer {
        TenantContextHolder.setCurrentTenant(tenant)
        val saved = customerRepository.save(newCustomer(name))
        createdRows += tenant to saved.uid
        return saved
    }

    private fun newCustomer(name: String): Customer = Customer().apply {
        countryCode = 91
        this.name = name
        customerType = "RETAIL"
        customerGroup = "DEFAULT"
        phone = "9999999999"
        landline = "0800000000"
        email = "$name@example.com"
        creditLimit = 0.0
        creditDays = 0
        outstandingAmount = 0.0
        address = "123 Test Street"
        street = "Test Street"
        street2 = "Test Street 2"
        city = "Bengaluru"
        pincode = "560001"
        state = "KA"
        country = "India"
    }

    private companion object {
        const val TENANT_A = "tenant-A"
        const val TENANT_B = "tenant-B"
    }
}

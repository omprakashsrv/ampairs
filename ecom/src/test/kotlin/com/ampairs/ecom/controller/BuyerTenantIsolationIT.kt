package com.ampairs.ecom.controller

import com.ampairs.AmpairsApplication
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.service.InvoiceEcomService
import com.ampairs.invoice.domain.enums.InvoiceStatus
import com.ampairs.invoice.domain.model.Invoice
import com.ampairs.invoice.repository.InvoiceRepository
import com.ampairs.payment.domain.model.PartyBalance
import com.ampairs.payment.repository.PartyBalanceRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.Instant

/**
 * Spec 029 (T036) — proves the buyer read services isolate by tenant end-to-end. The same buyer
 * (`PARTY1`) is a party in two workspaces; each workspace has its own finalized invoice and its own
 * ledger balance for that party. Under tenant A, the buyer must see only A's invoice + balance, and
 * never B's — and symmetrically. This exercises the real `@TenantId` predicate Hibernate appends
 * (OwnableBaseDomain.ownerId on Invoice / PartyBalance): the invoice side through the actual
 * `InvoiceEcomServiceImpl`, the ledger side at the `PartyBalanceRepository` the ecom read service
 * keys by partyUid — not mocks.
 *
 * Mirrors `customer/.../TenantIsolationIntegrationTest`: deliberately NOT `@Transactional`. Hibernate
 * binds the tenant to the Session when it opens; a test-managed transaction would open its Session
 * before the body sets the tenant and bind "default", failing the assigned-tenant-id check. Letting
 * each repository/service call run in its own transaction binds the tenant set immediately before it —
 * exactly how a real per-request flow behaves. Rows are cleaned up explicitly.
 */
@SpringBootTest(classes = [AmpairsApplication::class])
@ActiveProfiles("test")
class BuyerTenantIsolationIT {

    @Autowired private lateinit var invoiceRepository: InvoiceRepository
    @Autowired private lateinit var partyBalanceRepository: PartyBalanceRepository
    @Autowired private lateinit var invoiceEcomService: InvoiceEcomService

    private val invoiceRows = mutableListOf<Pair<String, String>>()  // tenant -> invoice uid
    private val balanceRows = mutableListOf<Pair<String, String>>()  // tenant -> party uid

    @AfterEach
    fun cleanup() {
        invoiceRows.forEach { (tenant, uid) ->
            TenantContextHolder.setCurrentTenant(tenant)
            invoiceRepository.findByUid(uid)?.let { invoiceRepository.delete(it) }
        }
        balanceRows.forEach { (tenant, party) ->
            TenantContextHolder.setCurrentTenant(tenant)
            partyBalanceRepository.findByPartyUid(party)?.let { partyBalanceRepository.delete(it) }
        }
        invoiceRows.clear()
        balanceRows.clear()
        TenantContextHolder.clearTenantContext()
    }

    @Test
    fun `each workspace returns only its own invoices for the shared buyer`() {
        val invA = seedInvoice(TENANT_A, orderRef = "ORD-A")
        val invB = seedInvoice(TENANT_B, orderRef = "ORD-B")

        // Tenant A: the list, the single-invoice read, and the per-order read all see only A's row.
        TenantContextHolder.setCurrentTenant(TENANT_A)
        val listedByA = invoiceEcomService.listBuyerInvoices(PARTY1, PageRequest.of(0, 50)).content
        assertTrue(listedByA.any { it.invoiceUid == invA.uid }, "tenant A must see its own invoice")
        assertTrue(listedByA.none { it.invoiceUid == invB.uid }, "tenant A must NOT see tenant B's invoice")
        assertNull(invoiceEcomService.getBuyerInvoice(invB.uid, PARTY1),
            "tenant A must NOT resolve tenant B's invoice by uid")
        assertTrue(invoiceEcomService.listInvoicesForOrder("ORD-B", PARTY1).isEmpty(),
            "tenant A must NOT see B's order->invoice link")
        assertEquals(listOf(invA.uid), invoiceEcomService.listInvoicesForOrder("ORD-A", PARTY1).map { it.invoiceUid })

        // Tenant B: symmetric.
        TenantContextHolder.setCurrentTenant(TENANT_B)
        val listedByB = invoiceEcomService.listBuyerInvoices(PARTY1, PageRequest.of(0, 50)).content
        assertTrue(listedByB.any { it.invoiceUid == invB.uid })
        assertTrue(listedByB.none { it.invoiceUid == invA.uid })
        assertNull(invoiceEcomService.getBuyerInvoice(invA.uid, PARTY1))
        assertTrue(invoiceEcomService.listInvoicesForOrder("ORD-A", PARTY1).isEmpty())
    }

    @Test
    fun `each workspace returns only its own ledger balance for the shared buyer`() {
        // The buyer's money position is read from the party ledger's balance store, which the ecom
        // read service keys by partyUid. Proving @TenantId on PartyBalance (the store the service
        // reads) proves the isolation without depending on the aging-bucket settings query, which
        // uses a `store_setting."value"` column H2 can't parse (Postgres-only; validated in CI).
        val balA = seedBalance(TENANT_A, BigDecimal("1000.00"))
        val balB = seedBalance(TENANT_B, BigDecimal("2000.00"))
        assertEquals(TENANT_A, balA.ownerId)
        assertEquals(TENANT_B, balB.ownerId)

        // Tenant A resolves only its own balance for the shared partyUid — never tenant B's.
        TenantContextHolder.setCurrentTenant(TENANT_A)
        val seenByA = partyBalanceRepository.findByPartyUid(PARTY1)
        assertEquals(0, BigDecimal("1000.00").compareTo(seenByA!!.cachedClosingBalance),
            "tenant A must resolve only its own ledger balance")
        assertEquals(TENANT_A, seenByA.ownerId)

        // Tenant B: symmetric — its own balance, never A's.
        TenantContextHolder.setCurrentTenant(TENANT_B)
        val seenByB = partyBalanceRepository.findByPartyUid(PARTY1)
        assertEquals(0, BigDecimal("2000.00").compareTo(seenByB!!.cachedClosingBalance),
            "tenant B must resolve only its own ledger balance")
        assertEquals(TENANT_B, seenByB.ownerId)
    }

    private fun seedInvoice(tenant: String, orderRef: String): Invoice {
        TenantContextHolder.setCurrentTenant(tenant)
        val saved = invoiceRepository.save(
            Invoice().apply {
                invoiceNumber = "INV-$tenant"
                invoiceDate = Instant.parse("2026-08-15T10:00:00Z")
                status = InvoiceStatus.INVOICED
                customerId = PARTY1
                orderRefId = orderRef
                basePrice = 9000.0
                totalTax = 207.5
                totalCost = 9207.5
            },
        )
        invoiceRows += tenant to saved.uid
        return saved
    }

    private fun seedBalance(tenant: String, closing: BigDecimal): PartyBalance {
        TenantContextHolder.setCurrentTenant(tenant)
        val saved = partyBalanceRepository.save(
            PartyBalance().apply {
                partyUid = PARTY1
                cachedClosingBalance = closing
            },
        )
        balanceRows += tenant to PARTY1
        return saved
    }

    private companion object {
        const val TENANT_A = "tenant-A"
        const val TENANT_B = "tenant-B"
        const val PARTY1 = "PARTY1"
    }
}

package com.ampairs.invoice.repository

import com.ampairs.invoice.domain.enums.InvoiceStatus
import com.ampairs.invoice.domain.model.Invoice
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface InvoiceRepository : CrudRepository<Invoice, Long> {

    @EntityGraph("Invoice.withItems")
    fun findByUid(uid: String): Invoice?

    /**
     * Buyer invoice list (spec 029): a party's finalized invoices, paginated. @TenantId scopes this to
     * the current workspace; the caller passes the finalized status set and a newest-first sort.
     */
    fun findByCustomerIdAndStatusIn(
        customerId: String,
        statuses: Collection<InvoiceStatus>,
        pageable: Pageable,
    ): Page<Invoice>

    /**
     * Order↔invoice link (spec 029): finalized invoices raised from a workspace order
     * (`orderRefId == Order.uid`). @TenantId-filtered. Items eager-loaded is unnecessary here (summary only).
     */
    fun findByOrderRefIdAndStatusIn(
        orderRefId: String,
        statuses: Collection<InvoiceStatus>,
    ): List<Invoice>

    /** Numbering collision check (spec 010 C5/FR-B09). @TenantId scopes this to the current workspace. */
    fun findBySeriesAndSequenceNumber(series: String, sequenceNumber: Long): Invoice?

    @Query("SELECT MAX(CAST(co.invoiceNumber AS INTEGER)) FROM invoice co")
    fun findMaxInvoiceNumber(): Optional<String>

    /** Sync checkpoint: max updatedAt for the current workspace (null when empty). @TenantId-filtered. */
    @Query("SELECT MAX(i.updatedAt) FROM invoice i")
    fun findMaxUpdatedAt(): Instant?

    /**
     * Finalized invoices whose invoiceDate falls in [fromInclusive, toExclusive). Backs the analytics
     * module's KPI rebuild. @TenantId scopes this to the current workspace.
     */
    @EntityGraph("Invoice.withItems")
    fun findByStatusAndInvoiceDateGreaterThanEqualAndInvoiceDateLessThan(
        status: InvoiceStatus,
        fromInclusive: Instant,
        toExclusive: Instant,
    ): List<Invoice>
}
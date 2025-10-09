package com.ampairs.invoice.repository

import com.ampairs.invoice.domain.model.Invoice
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface InvoiceRepository : CrudRepository<Invoice, Long> {

    fun findByUid(uid: String): Invoice?

    @Query("SELECT MAX(CAST(co.invoiceNumber AS INTEGER)) FROM invoice co")
    fun findMaxInvoiceNumber(): Optional<String>
}
package com.ampairs.invoice.repository

import com.ampairs.invoice.domain.model.Invoice
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface InvoiceRepository : CrudRepository<Invoice, String> {

    @Query("SELECT co FROM invoice co WHERE co.id = :id")
    override fun findById(id: String): Optional<Invoice>

    @Query("SELECT MAX(CAST(co.invoiceNumber AS INTEGER)) FROM invoice co")
    fun findMaxInvoiceNumber(): Optional<String>
}
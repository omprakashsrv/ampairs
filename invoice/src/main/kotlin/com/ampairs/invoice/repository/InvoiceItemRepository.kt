package com.ampairs.invoice.repository

import com.ampairs.invoice.domain.model.InvoiceItem
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface InvoiceItemRepository : CrudRepository<InvoiceItem, String> {

    @Query("SELECT oi FROM invoice_item oi WHERE oi.id = :id")
    override fun findById(id: String): Optional<InvoiceItem>

}
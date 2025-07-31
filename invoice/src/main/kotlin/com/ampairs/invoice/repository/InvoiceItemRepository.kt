package com.ampairs.invoice.repository

import com.ampairs.invoice.domain.model.InvoiceItem
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface InvoiceItemRepository : CrudRepository<InvoiceItem, Long> {

    @Query("SELECT oi FROM invoice_item oi WHERE oi.seqId = :seqId")
    fun findBySeqId(seqId: String): InvoiceItem?

}
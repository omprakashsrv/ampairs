package com.ampairs.invoice.repository

import com.ampairs.invoice.domain.model.InvoiceItem
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface InvoiceItemRepository : CrudRepository<InvoiceItem, Long> {

    fun findByUid(uid: String): InvoiceItem?

}
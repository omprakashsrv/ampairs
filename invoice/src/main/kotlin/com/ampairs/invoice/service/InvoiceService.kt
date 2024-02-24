package com.ampairs.invoice.service

import com.ampairs.invoice.domain.dto.InvoiceResponse
import com.ampairs.invoice.domain.dto.toResponse
import com.ampairs.invoice.domain.model.Invoice
import com.ampairs.invoice.domain.model.InvoiceItem
import com.ampairs.invoice.repository.InvoiceItemRepository
import com.ampairs.invoice.repository.InvoicePagingRepository
import com.ampairs.invoice.repository.InvoiceRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrNull

@Service
class InvoiceService @Autowired constructor(
    val invoiceRepository: InvoiceRepository,
    val invoiceItemRepository: InvoiceItemRepository,
    val invoicePagingRepository: InvoicePagingRepository,
) {
    @Transactional
    fun updateInvoice(invoice: Invoice, invoiceItems: List<InvoiceItem>): InvoiceResponse {
        val existingInvoice = invoiceRepository.findById(invoice.id).getOrNull()
        invoice.seqId = existingInvoice?.seqId
        invoice.invoiceNumber = existingInvoice?.invoiceNumber ?: ""
        if (invoice.invoiceNumber.isEmpty()) {
            val invoiceNumber = invoiceRepository.findMaxInvoiceNumber().getOrDefault("0").toIntOrNull() ?: 0
            invoice.invoiceNumber = (invoiceNumber + 1).toString()
        }
        invoiceRepository.save(invoice)
        invoiceItems.forEach { invoiceItem ->
            if (invoiceItem.id.isNotEmpty()) {
                val existingInvoiceItem = invoiceItemRepository.findById(invoiceItem.id).getOrNull()
                invoiceItem.seqId = existingInvoiceItem?.seqId
            }
            invoiceItemRepository.save(invoiceItem)
        }
        return invoice.toResponse(invoiceItems)
    }

    fun getInvoices(lastUpdated: Long): List<Invoice> {
        val invoices =
            invoicePagingRepository.findAllByLastUpdatedGreaterThanEqual(
                lastUpdated, PageRequest.of(0, 50, Sort.by("lastUpdated").ascending())
            )
        return invoices
    }


}
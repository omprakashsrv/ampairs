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

@Service
class InvoiceService @Autowired constructor(
    val invoiceRepository: InvoiceRepository,
    val invoiceItemRepository: InvoiceItemRepository,
    val invoicePagingRepository: InvoicePagingRepository,
) {
    @Transactional
    fun updateInvoice(invoice: Invoice, invoiceItems: List<InvoiceItem>): InvoiceResponse {
        val existingInvoice = invoiceRepository.findByUid(invoice.uid)
        invoice.uid = existingInvoice?.uid ?: invoice.uid
        invoice.invoiceNumber = existingInvoice?.invoiceNumber ?: ""
        if (invoice.invoiceNumber.isEmpty()) {
            val invoiceNumber = invoiceRepository.findMaxInvoiceNumber().getOrDefault("0").toIntOrNull() ?: 0
            invoice.invoiceNumber = (invoiceNumber + 1).toString()
        }
        val updatedInvoice = invoiceRepository.save(invoice)
        invoice.uid = updatedInvoice.uid
        invoiceItems.forEach { invoiceItem ->
            if (invoiceItem.uid.isNotEmpty()) {
                val existingInvoiceItem = invoiceItemRepository.findByUid(invoiceItem.uid)
                invoiceItem.id = existingInvoiceItem?.id ?: 0
            }
            invoiceItem.invoiceId = invoice.uid
            invoiceItemRepository.save(invoiceItem)
        }
        return invoice.toResponse(invoiceItems)
    }

    @Transactional(readOnly = true)
    fun getInvoices(lastUpdated: Long): List<Invoice> {
        val invoices =
            invoicePagingRepository.findAllByLastUpdatedGreaterThanEqual(
                lastUpdated, PageRequest.of(0, 50, Sort.by("lastUpdated").ascending())
            )
        return invoices
    }

    fun getInvoice(uid: String): Invoice? {
        return invoiceRepository.findByUid(uid = uid)
    }


}
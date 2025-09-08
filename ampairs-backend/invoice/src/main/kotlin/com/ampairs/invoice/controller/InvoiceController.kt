package com.ampairs.invoice.controller

import com.ampairs.invoice.domain.dto.*
import com.ampairs.invoice.service.InvoiceService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/invoice/v1")
class InvoiceController @Autowired constructor(
    private val invoiceService: InvoiceService,
) {

    @PostMapping("")
    fun updateInvoice(@RequestBody @Valid invoiceUpdateRequest: InvoiceUpdateRequest): InvoiceResponse {
        val invoice = invoiceUpdateRequest.toInvoice()
        val invoiceItems = invoiceUpdateRequest.invoiceItems.toInvoiceItems()
        return invoiceService.updateInvoice(invoice, invoiceItems)
    }

    @GetMapping("")
    fun getInvoices(@RequestParam("last_updated") lastUpdated: Long?): List<InvoiceResponse> {
        return invoiceService.getInvoices(lastUpdated ?: 0).toResponse()
    }

}
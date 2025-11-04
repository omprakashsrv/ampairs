package com.ampairs.invoice.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.invoice.domain.dto.*
import com.ampairs.invoice.service.InvoiceService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/invoice/v1")
class InvoiceController @Autowired constructor(
    private val invoiceService: InvoiceService,
) {

    @PostMapping("")
    fun updateInvoice(@RequestBody @Valid invoiceUpdateRequest: InvoiceUpdateRequest): ApiResponse<InvoiceResponse> {
        val invoice = invoiceUpdateRequest.toInvoice()
        val invoiceItems = invoiceUpdateRequest.invoiceItems.toInvoiceItems()
        val result = invoiceService.updateInvoice(invoice, invoiceItems)
        return ApiResponse.success(result)
    }

    @GetMapping("")
    fun getInvoices(@RequestParam("last_updated") lastUpdated: Instant?): ApiResponse<List<InvoiceResponse>> {
        val result = invoiceService.getInvoices(lastUpdated).toResponse()
        return ApiResponse.success(result)
    }

}
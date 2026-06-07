package com.ampairs.invoice.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.invoice.domain.dto.*
import com.ampairs.invoice.service.InvoiceService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/invoice/v1/invoices")
class InvoiceController(
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

    /**
     * Offline-sync bulk upsert (spec 010). Client UID-keyed; carries series + sequenceNumber.
     * Numbering collisions are skipped (server never renumbers, C5/FR-B09) — the client detects the
     * missing uid in the response and keeps that invoice unsynced.
     */
    @PostMapping("/sync")
    fun syncInvoices(@RequestBody @Valid requests: List<InvoiceUpdateRequest>): ApiResponse<List<InvoiceResponse>> {
        return ApiResponse.success(invoiceService.bulkUpsertInvoices(requests))
    }

    /**
     * Incremental sync feed: invoices updated at/after last_sync (ISO-8601), INCLUDING cancelled rows,
     * ordered by updatedAt ASC, paginated.
     */
    @GetMapping("/sync")
    fun getInvoicesSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int
    ): ApiResponse<PageResponse<InvoiceResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "updatedAt"))
        val invoicesPage = invoiceService.getInvoicesAfterSync(lastSync, pageable)
        return ApiResponse.success(PageResponse.from(invoicesPage) { it.toResponse(it.invoiceItems) })
    }

}

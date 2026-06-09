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

    /**
     * Legacy incremental list pull (last_updated). Superseded by GET /sync; retained only because the
     * mobile app's InvoiceRepository.getInvoiceResource() still calls it. Remove once the app migrates
     * its list pull to the /sync contract.
     */
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
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String
    ): ApiResponse<PageResponse<InvoiceResponse>> {
        val direction = Sort.Direction.fromString(sortDir)
        val pageable = PageRequest.of(page, size, Sort.by(direction, sortBy))
        val invoicesPage = invoiceService.getInvoicesAfterSync(lastSync, pageable)
        return ApiResponse.success(PageResponse.from(invoicesPage) { it.toResponse(it.invoiceItems) })
    }

}

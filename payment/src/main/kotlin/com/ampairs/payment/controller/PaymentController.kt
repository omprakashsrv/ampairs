package com.ampairs.payment.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.payment.domain.dto.AdjustmentVoucherRequest
import com.ampairs.payment.domain.dto.AdjustmentVoucherResponse
import com.ampairs.payment.domain.dto.AgingSummaryResponse
import com.ampairs.payment.domain.dto.BounceRequest
import com.ampairs.payment.domain.dto.LedgerEntryRequest
import com.ampairs.payment.domain.dto.LedgerEntryResponse
import com.ampairs.payment.domain.dto.OpenBillResponse
import com.ampairs.payment.domain.dto.PartyBalanceRequest
import com.ampairs.payment.domain.dto.PartyBalanceResponse
import com.ampairs.payment.domain.dto.PartyStatementResponse
import com.ampairs.payment.domain.dto.PaymentAllocationRequest
import com.ampairs.payment.domain.dto.PaymentAllocationResponse
import com.ampairs.payment.domain.dto.PaymentVoucherRequest
import com.ampairs.payment.domain.dto.PaymentVoucherResponse
import com.ampairs.payment.domain.dto.RecomputeResponse
import com.ampairs.payment.domain.dto.asResponse
import com.ampairs.payment.service.AdjustmentService
import com.ampairs.payment.service.AgingService
import com.ampairs.payment.service.BalanceService
import com.ampairs.payment.service.LedgerEntryService
import com.ampairs.payment.service.OutstandingService
import com.ampairs.payment.service.PartyBalanceService
import com.ampairs.payment.service.PaymentAllocationService
import com.ampairs.payment.service.PaymentVoucherService
import com.ampairs.payment.service.StatementService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

/**
 * Payment & Collection API (spec 013). Five canonical `/sync` resources + action endpoints.
 * Tenant context is set by `SessionUserFilter` (X-Workspace-ID); all reads/writes are
 * workspace-scoped. Errors bubble to [PaymentExceptionHandler].
 */
@RestController
@RequestMapping("/payment/v1")
class PaymentController(
    private val ledgerEntryService: LedgerEntryService,
    private val partyBalanceService: PartyBalanceService,
    private val voucherService: PaymentVoucherService,
    private val allocationService: PaymentAllocationService,
    private val adjustmentService: AdjustmentService,
    private val balanceService: BalanceService,
    private val statementService: StatementService,
    private val outstandingService: OutstandingService,
    private val agingService: AgingService,
) {

    private fun pageable(page: Int, size: Int, sortBy: String, sortDir: String) =
        PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), sortBy))

    // ── Ledger entries (sync §3) ────────────────────────────────────────────────

    @PostMapping("/ledger-entries/sync")
    fun syncLedgerEntries(
        @RequestBody @Valid requests: List<LedgerEntryRequest>,
    ): ApiResponse<List<LedgerEntryResponse>> =
        ApiResponse.success(ledgerEntryService.bulkUpsert(requests))

    @GetMapping("/ledger-entries/sync")
    fun getLedgerEntriesSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<LedgerEntryResponse>> {
        val result = ledgerEntryService.getAfterSync(lastSync, pageable(page, size, sortBy, sortDir))
        return ApiResponse.success(PageResponse.from(result) { it.asResponse() })
    }

    // ── Party balances (sync §4) ────────────────────────────────────────────────

    @PostMapping("/party-balances/sync")
    fun syncPartyBalances(
        @RequestBody @Valid requests: List<PartyBalanceRequest>,
    ): ApiResponse<List<PartyBalanceResponse>> =
        ApiResponse.success(partyBalanceService.bulkUpsert(requests))

    @GetMapping("/party-balances/sync")
    fun getPartyBalancesSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<PartyBalanceResponse>> {
        val result = partyBalanceService.getAfterSync(lastSync, pageable(page, size, sortBy, sortDir))
        return ApiResponse.success(PageResponse.from(result) { it.asResponse() })
    }

    // ── Payment vouchers (sync §1) ──────────────────────────────────────────────

    @PostMapping("/vouchers/sync")
    fun syncVouchers(
        @RequestBody @Valid requests: List<PaymentVoucherRequest>,
    ): ApiResponse<List<PaymentVoucherResponse>> =
        ApiResponse.success(voucherService.bulkUpsert(requests))

    @GetMapping("/vouchers/sync")
    fun getVouchersSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<PaymentVoucherResponse>> {
        val result = voucherService.getAfterSync(lastSync, pageable(page, size, sortBy, sortDir))
        return ApiResponse.success(PageResponse.from(result) { it.asResponse() })
    }

    // ── Payment allocations (sync §2) ───────────────────────────────────────────

    @PostMapping("/allocations/sync")
    fun syncAllocations(
        @RequestBody @Valid requests: List<PaymentAllocationRequest>,
    ): ApiResponse<List<PaymentAllocationResponse>> =
        ApiResponse.success(allocationService.bulkUpsert(requests))

    @GetMapping("/allocations/sync")
    fun getAllocationsSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<PaymentAllocationResponse>> {
        val result = allocationService.getAfterSync(lastSync, pageable(page, size, sortBy, sortDir))
        return ApiResponse.success(PageResponse.from(result) { it.asResponse() })
    }

    // ── Adjustments (sync §5) ───────────────────────────────────────────────────

    @PostMapping("/adjustments/sync")
    fun syncAdjustments(
        @RequestBody @Valid requests: List<AdjustmentVoucherRequest>,
    ): ApiResponse<List<AdjustmentVoucherResponse>> =
        ApiResponse.success(adjustmentService.bulkUpsert(requests))

    @GetMapping("/adjustments/sync")
    fun getAdjustmentsSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<AdjustmentVoucherResponse>> {
        val result = adjustmentService.getAfterSync(lastSync, pageable(page, size, sortBy, sortDir))
        return ApiResponse.success(PageResponse.from(result) { it.asResponse() })
    }

    // ── Action endpoints (payment-actions.md) ───────────────────────────────────

    @GetMapping("/parties/{partyUid}/statement")
    fun statement(
        @PathVariable partyUid: String,
        @RequestParam("from", required = false) from: String?,
        @RequestParam("to", required = false) to: String?,
    ): ApiResponse<PartyStatementResponse> =
        ApiResponse.success(statementService.buildStatement(partyUid, parseInstant(from), parseInstant(to)))

    @GetMapping("/parties/{partyUid}/open-bills")
    fun openBills(@PathVariable partyUid: String): ApiResponse<List<OpenBillResponse>> =
        ApiResponse.success(outstandingService.openBills(partyUid))

    @GetMapping("/aging")
    fun aging(@RequestParam("as_of", required = false) asOf: String?): ApiResponse<AgingSummaryResponse> =
        ApiResponse.success(agingService.summary(parseInstant(asOf) ?: Instant.now()))

    @PostMapping("/parties/{partyUid}/recompute-balance")
    fun recompute(@PathVariable partyUid: String): ApiResponse<RecomputeResponse> =
        ApiResponse.success(balanceService.recomputeWithTieOut(partyUid))

    @PostMapping("/vouchers/{voucherUid}/bounce")
    fun bounce(
        @PathVariable voucherUid: String,
        @RequestBody(required = false) request: BounceRequest?,
    ): ApiResponse<PaymentVoucherResponse> =
        ApiResponse.success(voucherService.bounce(voucherUid, request?.narration))

    @PostMapping("/vouchers/{voucherUid}/clear")
    fun clear(@PathVariable voucherUid: String): ApiResponse<PaymentVoucherResponse> =
        ApiResponse.success(voucherService.clear(voucherUid))

    private fun parseInstant(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        return try {
            Instant.parse(URLDecoder.decode(raw, StandardCharsets.UTF_8))
        } catch (e: Exception) {
            null
        }
    }
}

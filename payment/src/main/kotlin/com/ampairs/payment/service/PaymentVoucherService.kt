package com.ampairs.payment.service

import com.ampairs.payment.config.Constants
import com.ampairs.payment.domain.dto.PaymentVoucherRequest
import com.ampairs.payment.domain.dto.PaymentVoucherResponse
import com.ampairs.payment.domain.dto.asResponse
import com.ampairs.payment.domain.dto.toEntity
import com.ampairs.payment.domain.enums.ClearanceStatus
import com.ampairs.payment.domain.enums.EntryType
import com.ampairs.payment.domain.enums.PaymentDirection
import com.ampairs.payment.domain.enums.SourceType
import com.ampairs.payment.domain.model.PaymentVoucher
import com.ampairs.payment.exception.InvalidClearanceTransitionException
import com.ampairs.payment.exception.PaymentVoucherNotFoundException
import com.ampairs.payment.repository.PaymentAllocationRepository
import com.ampairs.payment.repository.PaymentVoucherRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * Payment-voucher `/sync` + lifecycle service (US1 / US6, FR-007..012, FR-020/021).
 *
 * On upsert: assigns a voucher number (series RCP/PAY), defaults the clearance status, posts/updates
 * the deterministic `LDG_<voucher.uid>` ledger entry (or reverses it on soft-delete), and recomputes
 * the party balance. Allocations never affect the balance.
 */
@Service
@Transactional(readOnly = true)
class PaymentVoucherService(
    private val voucherRepository: PaymentVoucherRepository,
    private val allocationRepository: PaymentAllocationRepository,
    private val balanceService: BalanceService,
    private val voucherNumberService: VoucherNumberService,
) {

    @Transactional
    fun bulkUpsert(requests: List<PaymentVoucherRequest>): List<PaymentVoucherResponse> =
        requests.map { upsert(it) }

    private fun upsert(request: PaymentVoucherRequest): PaymentVoucherResponse {
        val incoming = request.toEntity()
        val existing = if (incoming.uid.isNotBlank()) voucherRepository.findByUid(incoming.uid) else null

        val voucher: PaymentVoucher = existing ?: incoming
        if (existing != null) {
            existing.partyUid = incoming.partyUid
            existing.voucherDate = incoming.voucherDate
            existing.direction = incoming.direction
            existing.totalAmount = incoming.totalAmount
            existing.paymentMode = incoming.paymentMode
            existing.referenceNumber = incoming.referenceNumber
            existing.instrumentDate = incoming.instrumentDate
            existing.bankName = incoming.bankName
            existing.narration = incoming.narration
            existing.active = incoming.active
            // Keep clearance as-is unless the client explicitly sent one.
            if (request.clearanceStatus != null) existing.clearanceStatus = request.clearanceStatus
            if (!incoming.voucherNo.isNullOrBlank()) existing.voucherNo = incoming.voucherNo
        }

        // Default clearance for a new voucher (only when the client didn't send one).
        if (existing == null && request.clearanceStatus == null) {
            voucher.clearanceStatus =
                if (voucher.paymentMode.isInstant()) ClearanceStatus.CLEARED else ClearanceStatus.PENDING
        }

        // Voucher numbering (series by direction).
        if (voucher.voucherNo.isNullOrBlank()) {
            val series = if (voucher.direction == PaymentDirection.RECEIVED)
                Constants.SERIES_RECEIPT else Constants.SERIES_PAYMENT_OUT
            voucher.voucherNo = voucherNumberService.resolve(series, voucher.voucherNo)
        }

        val saved = voucherRepository.save(voucher)
        recomputeUnallocated(saved)
        postOrReverseLedger(saved)
        return voucherRepository.findByUid(saved.uid)?.asResponse() ?: saved.asResponse()
    }

    /**
     * Posts the voucher's ledger entry, or reverses it when the voucher is inactive (soft-deleted /
     * cancelled) or in a non-posting clearance state (BOUNCED / CANCELLED keep their own reversal).
     */
    private fun postOrReverseLedger(voucher: PaymentVoucher) {
        val shouldPost = voucher.active &&
            voucher.clearanceStatus != ClearanceStatus.CANCELLED
        if (!shouldPost) {
            balanceService.reverseSourceEntry(voucher.uid)
            return
        }
        val entryType = if (voucher.direction == PaymentDirection.RECEIVED)
            EntryType.PAYMENT_IN else EntryType.PAYMENT_OUT
        balanceService.postSourceEntry(
            partyUid = voucher.partyUid,
            entryType = entryType,
            amount = voucher.totalAmount,
            sourceType = SourceType.PAYMENT,
            sourceUid = voucher.uid,
            voucherNo = voucher.voucherNo,
            entryDate = voucher.voucherDate,
            narration = voucher.narration,
        )
    }

    /** Recomputes unallocatedAmount = totalAmount − Σ active allocations. */
    @Transactional
    fun recomputeUnallocated(voucher: PaymentVoucher) {
        val allocated = allocationRepository.allocatedTotalForVoucher(voucher.uid)
        voucher.unallocatedAmount = voucher.totalAmount.subtract(allocated).max(BigDecimal.ZERO)
        voucherRepository.save(voucher)
    }

    // ── Clearance actions (US6) ─────────────────────────────────────────────────

    /** Mark a pending payment cleared (realised). No-op on already-CLEARED; rejected if terminal. */
    @Transactional
    fun clear(voucherUid: String): PaymentVoucherResponse {
        val voucher = voucherRepository.findByUid(voucherUid)
            ?: throw PaymentVoucherNotFoundException("Voucher not found: $voucherUid")
        if (voucher.clearanceStatus.isTerminal()) {
            throw InvalidClearanceTransitionException(
                "Cannot clear a ${voucher.clearanceStatus} voucher"
            )
        }
        voucher.clearanceStatus = ClearanceStatus.CLEARED
        voucherRepository.save(voucher)
        return voucher.asResponse()
    }

    /**
     * Mark a payment bounced — posts a contra ledger entry that restores the party's outstanding,
     * keeping both original and reversal visible (FR-021). No-op if already terminal.
     */
    @Transactional
    fun bounce(voucherUid: String, narration: String?): PaymentVoucherResponse {
        val voucher = voucherRepository.findByUid(voucherUid)
            ?: throw PaymentVoucherNotFoundException("Voucher not found: $voucherUid")
        if (voucher.clearanceStatus.isTerminal()) {
            throw InvalidClearanceTransitionException(
                "Cannot bounce a ${voucher.clearanceStatus} voucher"
            )
        }
        voucher.clearanceStatus = ClearanceStatus.BOUNCED
        voucherRepository.save(voucher)
        balanceService.postContraReversal(voucher.uid, narration)
        return voucher.asResponse()
    }

    fun getAfterSync(lastSync: String?, pageable: Pageable): Page<PaymentVoucher> {
        val cursor = SyncSupport.parseLastSync(lastSync)
        return if (cursor == null) voucherRepository.findAllByOrderByUpdatedAtAsc(pageable)
        else voucherRepository.findByUpdatedAtGreaterThanEqual(cursor, pageable)
    }
}

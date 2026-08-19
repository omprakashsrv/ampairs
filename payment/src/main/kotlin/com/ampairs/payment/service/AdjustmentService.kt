package com.ampairs.payment.service

import com.ampairs.payment.config.Constants
import com.ampairs.payment.domain.dto.AdjustmentVoucherRequest
import com.ampairs.payment.domain.dto.AdjustmentVoucherResponse
import com.ampairs.payment.domain.dto.asResponse
import com.ampairs.payment.domain.dto.toEntity
import com.ampairs.payment.domain.enums.AdjustmentType
import com.ampairs.payment.domain.enums.SourceType
import com.ampairs.payment.domain.model.AdjustmentVoucher
import com.ampairs.payment.repository.AdjustmentVoucherRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Adjustment `/sync` service (US5, FR-015). Maps `adjustmentType → EntryType + Direction` and posts
 * the deterministic `LDG_<adjustment.uid>` ledger entry via `BalanceService`; soft-delete reverses it.
 * Voucher numbers come from the appropriate series (CRN / DBN / ADJ).
 */
@Service
@Transactional(readOnly = true)
class AdjustmentService(
    private val adjustmentRepository: AdjustmentVoucherRepository,
    private val balanceService: BalanceService,
    private val voucherNumberService: VoucherNumberService,
    private val entityChangePublisher: com.ampairs.core.sync.EntityChangePublisher,
) {

    @Transactional
    fun bulkUpsert(requests: List<AdjustmentVoucherRequest>): List<AdjustmentVoucherResponse> =
        requests.map { upsert(it) }

    private fun upsert(request: AdjustmentVoucherRequest): AdjustmentVoucherResponse {
        val incoming = request.toEntity()
        val existing = if (incoming.uid.isNotBlank()) adjustmentRepository.findByUid(incoming.uid) else null
        val voucher: AdjustmentVoucher = existing ?: incoming
        if (existing != null) {
            existing.partyUid = incoming.partyUid
            existing.voucherDate = incoming.voucherDate
            existing.adjustmentType = incoming.adjustmentType
            existing.amount = incoming.amount
            existing.narration = incoming.narration
            existing.sourceRef = incoming.sourceRef
            existing.active = incoming.active
            if (!incoming.voucherNo.isNullOrBlank()) existing.voucherNo = incoming.voucherNo
        }

        if (voucher.voucherNo.isNullOrBlank()) {
            voucher.voucherNo = voucherNumberService.resolve(seriesFor(voucher.adjustmentType), voucher.voucherNo)
        }

        val saved = adjustmentRepository.save(voucher)
        if (existing == null) entityChangePublisher.created("adjustment", saved.uid)
        else entityChangePublisher.updated("adjustment", saved.uid)
        if (saved.active) {
            balanceService.postSourceEntry(
                partyUid = saved.partyUid,
                entryType = saved.adjustmentType.toEntryType(),
                amount = saved.amount,
                sourceType = SourceType.ADJUSTMENT,
                sourceUid = saved.uid,
                voucherNo = saved.voucherNo,
                entryDate = saved.voucherDate,
                narration = saved.narration,
                direction = saved.adjustmentType.direction(),
            )
        } else {
            balanceService.reverseSourceEntry(saved.uid)
        }
        return saved.asResponse()
    }

    private fun seriesFor(type: AdjustmentType): String = when (type) {
        AdjustmentType.SALES_RETURN, AdjustmentType.CREDIT_NOTE, AdjustmentType.PURCHASE_BILL,
        AdjustmentType.WRITE_OFF, AdjustmentType.DISCOUNT_ALLOWED -> Constants.SERIES_CREDIT_NOTE
        AdjustmentType.PURCHASE_RETURN, AdjustmentType.DEBIT_NOTE -> Constants.SERIES_DEBIT_NOTE
        AdjustmentType.ADJUSTMENT -> Constants.SERIES_ADJUSTMENT
    }

    fun getAfterSync(lastSync: String?, pageable: Pageable): Page<AdjustmentVoucher> {
        val cursor = SyncSupport.parseLastSync(lastSync)
        return if (cursor == null) adjustmentRepository.findAllByOrderByUpdatedAtAsc(pageable)
        else adjustmentRepository.findByUpdatedAtGreaterThanEqual(cursor, pageable)
    }
}

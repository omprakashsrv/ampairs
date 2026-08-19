package com.ampairs.payment.service

import com.ampairs.payment.domain.dto.PaymentAllocationRequest
import com.ampairs.payment.domain.dto.PaymentAllocationResponse
import com.ampairs.payment.domain.dto.asResponse
import com.ampairs.payment.domain.dto.toEntity
import com.ampairs.payment.domain.model.PaymentAllocation
import com.ampairs.payment.exception.AllocationExceedsTotalException
import com.ampairs.payment.exception.PaymentVoucherNotFoundException
import com.ampairs.payment.repository.PaymentAllocationRepository
import com.ampairs.payment.repository.PaymentVoucherRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * Allocation `/sync` service (US1, FR-010/011). Validates Σ allocations ≤ voucher total and
 * recomputes the voucher's `unallocatedAmount`. Allocations NEVER touch the party balance.
 */
@Service
@Transactional(readOnly = true)
class PaymentAllocationService(
    private val allocationRepository: PaymentAllocationRepository,
    private val voucherRepository: PaymentVoucherRepository,
    private val voucherService: PaymentVoucherService,
    private val entityChangePublisher: com.ampairs.core.sync.EntityChangePublisher,
) {

    @Transactional
    fun bulkUpsert(requests: List<PaymentAllocationRequest>): List<PaymentAllocationResponse> {
        val results = mutableListOf<PaymentAllocationResponse>()
        val affectedVouchers = linkedSetOf<String>()
        for (request in requests) {
            val incoming = request.toEntity()
            val existing = if (incoming.uid.isNotBlank()) allocationRepository.findByUid(incoming.uid) else null
            val toSave: PaymentAllocation = existing ?: incoming
            if (existing != null) {
                existing.paymentVoucherUid = incoming.paymentVoucherUid
                existing.targetType = incoming.targetType
                existing.targetUid = incoming.targetUid
                existing.amount = incoming.amount
                existing.active = incoming.active
            }
            validateNotExceeding(toSave)
            val saved = allocationRepository.save(toSave)
            if (existing == null) entityChangePublisher.created("payment_allocation", saved.uid)
            else entityChangePublisher.updated("payment_allocation", saved.uid)
            affectedVouchers.add(saved.paymentVoucherUid)
            results.add(saved.asResponse())
        }
        // Recompute unallocated for each touched voucher.
        affectedVouchers.forEach { uid ->
            voucherRepository.findByUid(uid)?.let { voucherService.recomputeUnallocated(it) }
        }
        return results
    }

    /** Ensures Σ active allocations (with this row applied) ≤ voucher.totalAmount. */
    private fun validateNotExceeding(candidate: PaymentAllocation) {
        if (!candidate.active) return
        val voucher = voucherRepository.findByUid(candidate.paymentVoucherUid)
            ?: throw PaymentVoucherNotFoundException("Voucher not found: ${candidate.paymentVoucherUid}")
        val others = allocationRepository.findByPaymentVoucherUidAndActiveTrue(candidate.paymentVoucherUid)
            .filter { it.uid != candidate.uid }
            .fold(BigDecimal.ZERO) { acc, a -> acc.add(a.amount) }
        val total = others.add(candidate.amount)
        if (total > voucher.totalAmount) {
            throw AllocationExceedsTotalException(
                "Allocated amount exceeds voucher total",
                validationErrors = mapOf("amount" to "sum $total > total ${voucher.totalAmount}"),
            )
        }
    }

    fun getAfterSync(lastSync: String?, pageable: Pageable): Page<PaymentAllocation> {
        val cursor = SyncSupport.parseLastSync(lastSync)
        return if (cursor == null) allocationRepository.findAllByOrderByUpdatedAtAsc(pageable)
        else allocationRepository.findByUpdatedAtGreaterThanEqual(cursor, pageable)
    }
}

package com.ampairs.payment.repository

import com.ampairs.payment.domain.enums.AllocationTarget
import com.ampairs.payment.domain.model.PaymentAllocation
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant

@Repository
interface PaymentAllocationRepository : JpaRepository<PaymentAllocation, Long> {

    fun findByUid(uid: String): PaymentAllocation?

    fun findByPaymentVoucherUidAndActiveTrue(paymentVoucherUid: String): List<PaymentAllocation>

    fun findByTargetTypeAndTargetUidAndActiveTrue(
        targetType: AllocationTarget,
        targetUid: String,
    ): List<PaymentAllocation>

    /** Σ active allocations for a voucher (used to recompute unallocatedAmount). */
    @Query(
        """
        SELECT COALESCE(SUM(a.amount), 0) FROM payment_allocation a
        WHERE a.paymentVoucherUid = :voucherUid AND a.active = true
        """
    )
    fun allocatedTotalForVoucher(@Param("voucherUid") voucherUid: String): BigDecimal

    /** Σ active allocations against a specific bill (used for open-bills outstanding). */
    @Query(
        """
        SELECT COALESCE(SUM(a.amount), 0) FROM payment_allocation a
        WHERE a.targetType = :targetType AND a.targetUid = :targetUid AND a.active = true
        """
    )
    fun allocatedTotalForTarget(
        @Param("targetType") targetType: AllocationTarget,
        @Param("targetUid") targetUid: String,
    ): BigDecimal

    // ── Sync feed (includes soft-deleted rows) ──────────────────────────────────
    fun findAllByOrderByUpdatedAtAsc(pageable: Pageable): Page<PaymentAllocation>

    fun findByUpdatedAtGreaterThanEqual(lastSync: Instant, pageable: Pageable): Page<PaymentAllocation>

    /** Sync checkpoint: max updatedAt for the current workspace (null when empty). @TenantId-filtered. */
    @Query("SELECT MAX(a.updatedAt) FROM payment_allocation a")
    fun findMaxUpdatedAt(): Instant?
}

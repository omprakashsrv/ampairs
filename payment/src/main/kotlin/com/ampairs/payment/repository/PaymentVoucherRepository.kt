package com.ampairs.payment.repository

import com.ampairs.payment.domain.enums.PaymentDirection
import com.ampairs.payment.domain.model.PaymentVoucher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant

@Repository
interface PaymentVoucherRepository : JpaRepository<PaymentVoucher, Long> {

    @EntityGraph("PaymentVoucher.withAllocations")
    fun findByUid(uid: String): PaymentVoucher?

    fun findByPartyUidAndActiveTrue(partyUid: String): List<PaymentVoucher>

    /**
     * Sum of active vouchers of a direction whose voucherDate is in [fromInclusive, toExclusive).
     * Backs the analytics "collected this period" KPI. @TenantId scopes to the current workspace.
     */
    @Query(
        "SELECT COALESCE(SUM(v.totalAmount), 0) FROM payment_voucher v " +
            "WHERE v.direction = :direction AND v.active = true " +
            "AND v.voucherDate >= :fromInclusive AND v.voucherDate < :toExclusive",
    )
    fun sumActiveByDirectionInWindow(
        @Param("direction") direction: PaymentDirection,
        @Param("fromInclusive") fromInclusive: Instant,
        @Param("toExclusive") toExclusive: Instant,
    ): BigDecimal

    // ── Sync feed (includes soft-deleted rows) ──────────────────────────────────
    @EntityGraph("PaymentVoucher.withAllocations")
    fun findAllByOrderByUpdatedAtAsc(pageable: Pageable): Page<PaymentVoucher>

    @EntityGraph("PaymentVoucher.withAllocations")
    fun findByUpdatedAtGreaterThanEqual(lastSync: Instant, pageable: Pageable): Page<PaymentVoucher>
}

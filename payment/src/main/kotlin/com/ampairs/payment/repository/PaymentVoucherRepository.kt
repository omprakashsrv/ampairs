package com.ampairs.payment.repository

import com.ampairs.payment.domain.model.PaymentVoucher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface PaymentVoucherRepository : JpaRepository<PaymentVoucher, Long> {

    @EntityGraph("PaymentVoucher.withAllocations")
    fun findByUid(uid: String): PaymentVoucher?

    fun findByPartyUidAndActiveTrue(partyUid: String): List<PaymentVoucher>

    // ── Sync feed (includes soft-deleted rows) ──────────────────────────────────
    @EntityGraph("PaymentVoucher.withAllocations")
    fun findAllByOrderByUpdatedAtAsc(pageable: Pageable): Page<PaymentVoucher>

    @EntityGraph("PaymentVoucher.withAllocations")
    fun findByUpdatedAtGreaterThanEqual(lastSync: Instant, pageable: Pageable): Page<PaymentVoucher>
}

package com.ampairs.payment.repository

import com.ampairs.payment.domain.model.AdjustmentVoucher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface AdjustmentVoucherRepository : JpaRepository<AdjustmentVoucher, Long> {

    fun findByUid(uid: String): AdjustmentVoucher?

    fun findByPartyUidAndActiveTrue(partyUid: String): List<AdjustmentVoucher>

    // ── Sync feed (includes soft-deleted rows) ──────────────────────────────────
    fun findAllByOrderByUpdatedAtAsc(pageable: Pageable): Page<AdjustmentVoucher>

    fun findByUpdatedAtGreaterThanEqual(lastSync: Instant, pageable: Pageable): Page<AdjustmentVoucher>
}

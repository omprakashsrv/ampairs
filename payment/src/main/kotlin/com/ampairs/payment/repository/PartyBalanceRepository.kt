package com.ampairs.payment.repository

import com.ampairs.payment.domain.model.PartyBalance
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface PartyBalanceRepository : JpaRepository<PartyBalance, Long> {

    fun findByUid(uid: String): PartyBalance?

    fun findByPartyUid(partyUid: String): PartyBalance?

    fun findByActiveTrue(): List<PartyBalance>

    // ── Sync feed (includes soft-deleted rows) ──────────────────────────────────
    fun findAllByOrderByUpdatedAtAsc(pageable: Pageable): Page<PartyBalance>

    fun findByUpdatedAtGreaterThanEqual(lastSync: Instant, pageable: Pageable): Page<PartyBalance>

    /** Sync checkpoint: max updatedAt for the current workspace (null when empty). @TenantId-filtered. */
    @Query("SELECT MAX(b.updatedAt) FROM party_balance b")
    fun findMaxUpdatedAt(): Instant?
}

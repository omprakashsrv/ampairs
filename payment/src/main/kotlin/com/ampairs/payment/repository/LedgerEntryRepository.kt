package com.ampairs.payment.repository

import com.ampairs.payment.domain.model.LedgerEntry
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant

@Repository
interface LedgerEntryRepository : JpaRepository<LedgerEntry, Long> {

    fun findByUid(uid: String): LedgerEntry?

    /** All active entries for a party (statement / recompute). @TenantId-filtered. */
    fun findByPartyUidAndActiveTrueOrderByEntryDateAsc(partyUid: String): List<LedgerEntry>

    /** Active entries for a party within a date range, ordered for statement rendering. */
    fun findByPartyUidAndActiveTrueAndEntryDateBetweenOrderByEntryDateAsc(
        partyUid: String,
        from: Instant,
        to: Instant,
    ): List<LedgerEntry>

    fun findByReversalOf(reversalOf: String): LedgerEntry?

    /** Signed sum (Σ DR amount − Σ CR amount) over active entries for a party. */
    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN e.direction = com.ampairs.payment.domain.enums.Direction.DR
                                 THEN e.amount ELSE -e.amount END), 0)
        FROM ledger_entry e
        WHERE e.partyUid = :partyUid AND e.active = true
        """
    )
    fun signedSumForParty(@Param("partyUid") partyUid: String): BigDecimal

    @Query(
        """
        SELECT COALESCE(SUM(e.amount), 0) FROM ledger_entry e
        WHERE e.partyUid = :partyUid AND e.active = true
          AND e.direction = com.ampairs.payment.domain.enums.Direction.DR
        """
    )
    fun totalDebitForParty(@Param("partyUid") partyUid: String): BigDecimal

    @Query(
        """
        SELECT COALESCE(SUM(e.amount), 0) FROM ledger_entry e
        WHERE e.partyUid = :partyUid AND e.active = true
          AND e.direction = com.ampairs.payment.domain.enums.Direction.CR
        """
    )
    fun totalCreditForParty(@Param("partyUid") partyUid: String): BigDecimal

    // ── Sync feed (includes soft-deleted rows) ──────────────────────────────────
    fun findAllByOrderByUpdatedAtAsc(pageable: Pageable): Page<LedgerEntry>

    fun findByUpdatedAtGreaterThanEqual(lastSync: Instant, pageable: Pageable): Page<LedgerEntry>
}

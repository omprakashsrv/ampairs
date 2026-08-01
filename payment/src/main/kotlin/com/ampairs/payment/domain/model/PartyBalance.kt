package com.ampairs.payment.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.payment.config.Constants
import com.ampairs.payment.domain.enums.Direction
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

/**
 * One row per party (= customer). Carries the opening balance attributes plus the *cached*,
 * server-authoritative signed closing balance. The closing balance is never edited directly;
 * it is recomputed by `BalanceService` as `openingSigned + Σ active LedgerEntry.signedAmount`.
 */
@Entity(name = "party_balance")
@Table(
    indexes = [
        Index(name = "idx_party_balance_uid", columnList = "uid", unique = true),
        Index(name = "uk_party_balance_owner_party", columnList = "owner_id, party_uid", unique = true),
    ]
)
class PartyBalance : OwnableBaseDomain() {

    @Column(name = "party_uid", nullable = false, length = 40)
    var partyUid: String = ""

    @Column(name = "opening_balance", nullable = false, precision = 19, scale = 4)
    var openingBalance: BigDecimal = BigDecimal.ZERO

    @Column(name = "opening_direction", nullable = false, length = 4)
    @Enumerated(EnumType.STRING)
    var openingDirection: Direction = Direction.DR

    @Column(name = "opening_as_of", nullable = false)
    var openingAsOf: Instant = Instant.now()

    /** Signed: > 0 ⇒ receivable ("to receive"), < 0 ⇒ payable ("to pay"). Denormalized. */
    @Column(name = "cached_closing_balance", nullable = false, precision = 19, scale = 4)
    var cachedClosingBalance: BigDecimal = BigDecimal.ZERO

    @Column(name = "last_computed_at", nullable = true)
    var lastComputedAt: Instant? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    /** Opening contribution to the signed balance (DR ⇒ +, CR ⇒ −). */
    fun openingSigned(): BigDecimal =
        if (openingDirection == Direction.DR) openingBalance else openingBalance.negate()

    override fun obtainSeqIdPrefix(): String = Constants.PARTY_BALANCE_PREFIX
}

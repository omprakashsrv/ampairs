package com.ampairs.payment.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.payment.config.Constants
import com.ampairs.payment.domain.enums.Direction
import com.ampairs.payment.domain.enums.EntryType
import com.ampairs.payment.domain.enums.SourceType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

/**
 * A single dated posting against a party — the *sole driver* of the party balance.
 * `amount` is always ≥ 0; the sign of its contribution comes from [direction]
 * (`DR ⇒ +amount`, `CR ⇒ −amount`).
 *
 * Document-derived entries use the deterministic uid `LDG_<sourceUid>` so concurrent/offline
 * clients cannot create a duplicate. Corrections are made by edit (same uid) or reversal
 * ([reversalOf]) + soft-delete — never a hard delete.
 */
@Entity(name = "ledger_entry")
@Table(
    indexes = [
        Index(name = "idx_ledger_entry_uid", columnList = "uid", unique = true),
        Index(name = "idx_ledger_entry_party_date", columnList = "owner_id, party_uid, entry_date"),
        Index(name = "idx_ledger_entry_source", columnList = "owner_id, source_type, source_uid"),
    ]
)
class LedgerEntry : OwnableBaseDomain() {

    @Column(name = "party_uid", nullable = false, length = 40)
    var partyUid: String = ""

    @Column(name = "entry_date", nullable = false)
    var entryDate: Instant = Instant.now()

    @Column(name = "entry_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    var entryType: EntryType = EntryType.ADJUSTMENT

    @Column(name = "direction", nullable = false, length = 4)
    @Enumerated(EnumType.STRING)
    var direction: Direction = Direction.DR

    /** Always ≥ 0; never store negatives. Sign carried by [direction]. */
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    var amount: BigDecimal = BigDecimal.ZERO

    @Column(name = "source_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var sourceType: SourceType = SourceType.MANUAL

    @Column(name = "source_uid", nullable = true, length = 64)
    var sourceUid: String? = null

    @Column(name = "voucher_no", nullable = true, length = 64)
    var voucherNo: String? = null

    @Column(name = "narration", nullable = true, length = 500)
    var narration: String? = null

    /** uid of the entry this reverses (set on contra entries). */
    @Column(name = "reversal_of", nullable = true, length = 64)
    var reversalOf: String? = null

    /** True once a reversal exists for this entry. */
    @Column(name = "reversed", nullable = false)
    var reversed: Boolean = false

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    /** Signed contribution to the party balance. */
    fun signedAmount(): BigDecimal = if (direction == Direction.DR) amount else amount.negate()

    override fun obtainSeqIdPrefix(): String = Constants.LEDGER_ENTRY_PREFIX
}

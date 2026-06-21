package com.ampairs.payment.domain.dto

import com.ampairs.payment.domain.enums.Direction
import com.ampairs.payment.domain.enums.EntryType
import com.ampairs.payment.domain.enums.SourceType
import com.ampairs.payment.domain.model.LedgerEntry
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant

data class LedgerEntryRequest(
    val uid: String = "",
    @field:NotBlank val partyUid: String = "",
    val entryDate: Instant? = null,
    val entryType: EntryType = EntryType.ADJUSTMENT,
    val direction: Direction? = null,
    val amount: BigDecimal = BigDecimal.ZERO,
    val sourceType: SourceType = SourceType.MANUAL,
    val sourceUid: String? = null,
    val voucherNo: String? = null,
    val narration: String? = null,
    val reversalOf: String? = null,
    val reversed: Boolean = false,
    val active: Boolean = true,
)

data class LedgerEntryResponse(
    val uid: String = "",
    val partyUid: String = "",
    val entryDate: Instant = Instant.now(),
    val entryType: EntryType = EntryType.ADJUSTMENT,
    val direction: Direction = Direction.DR,
    val amount: BigDecimal = BigDecimal.ZERO,
    val sourceType: SourceType = SourceType.MANUAL,
    val sourceUid: String? = null,
    val voucherNo: String? = null,
    val narration: String? = null,
    val reversalOf: String? = null,
    val reversed: Boolean = false,
    val active: Boolean = true,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

fun LedgerEntryRequest.toEntity(): LedgerEntry {
    val e = LedgerEntry()
    e.uid = this.uid
    e.partyUid = this.partyUid
    if (this.entryDate != null) e.entryDate = this.entryDate
    e.entryType = this.entryType
    // direction defaults to the entry type's natural direction when not supplied
    e.direction = this.direction ?: this.entryType.naturalDirection()
    e.amount = this.amount.abs()
    e.sourceType = this.sourceType
    e.sourceUid = this.sourceUid
    e.voucherNo = this.voucherNo
    e.narration = this.narration
    e.reversalOf = this.reversalOf
    e.reversed = this.reversed
    e.active = this.active
    return e
}

fun LedgerEntry.asResponse(): LedgerEntryResponse = LedgerEntryResponse(
    uid = this.uid,
    partyUid = this.partyUid,
    entryDate = this.entryDate,
    entryType = this.entryType,
    direction = this.direction,
    amount = this.amount,
    sourceType = this.sourceType,
    sourceUid = this.sourceUid,
    voucherNo = this.voucherNo,
    narration = this.narration,
    reversalOf = this.reversalOf,
    reversed = this.reversed,
    active = this.active,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
)

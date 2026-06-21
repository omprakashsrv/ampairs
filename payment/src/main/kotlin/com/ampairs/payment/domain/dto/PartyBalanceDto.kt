package com.ampairs.payment.domain.dto

import com.ampairs.payment.domain.enums.Direction
import com.ampairs.payment.domain.model.PartyBalance
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant

/**
 * Push carries opening-balance edits only; `cached_closing_balance` is server-computed
 * and authoritative on pull.
 */
data class PartyBalanceRequest(
    val uid: String = "",
    @field:NotBlank val partyUid: String = "",
    val openingBalance: BigDecimal = BigDecimal.ZERO,
    val openingDirection: Direction = Direction.DR,
    val openingAsOf: Instant? = null,
    val active: Boolean = true,
)

data class PartyBalanceResponse(
    val uid: String = "",
    val partyUid: String = "",
    val openingBalance: BigDecimal = BigDecimal.ZERO,
    val openingDirection: Direction = Direction.DR,
    val openingAsOf: Instant = Instant.now(),
    val cachedClosingBalance: BigDecimal = BigDecimal.ZERO,
    val lastComputedAt: Instant? = null,
    val active: Boolean = true,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

fun PartyBalance.asResponse(): PartyBalanceResponse = PartyBalanceResponse(
    uid = this.uid,
    partyUid = this.partyUid,
    openingBalance = this.openingBalance,
    openingDirection = this.openingDirection,
    openingAsOf = this.openingAsOf,
    cachedClosingBalance = this.cachedClosingBalance,
    lastComputedAt = this.lastComputedAt,
    active = this.active,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
)

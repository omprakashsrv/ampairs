package com.ampairs.core.domain.dto

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Wire representation of money — `{ "amount_minor": <Long>, "currency": "<ISO-4217>" }`.
 *
 * Integers (minor units — paise/cents) on the wire are unambiguous and float-drift-free for both the
 * KMP and Angular clients. Backend persistence uses `BigDecimal(19,4)` + a `currency CHAR(3)` column;
 * convert at the boundary via [of] / [toBigDecimal]. No raw `Double` for money in new code.
 */
data class MoneyDto(
    val amountMinor: Long,
    val currency: String,
) {
    /** Back to a 2-dp major-unit amount (e.g. paise → rupees). */
    fun toBigDecimal(): BigDecimal = BigDecimal.valueOf(amountMinor, MINOR_SCALE)

    companion object {
        /** Minor-unit scale (2 = paise/cents). ISO currencies with other exponents are out of MVP scope. */
        const val MINOR_SCALE: Int = 2

        /** Build from a major-unit [amount]; returns null when [amount] is null. */
        fun of(amount: BigDecimal?, currency: String): MoneyDto? =
            amount?.let {
                MoneyDto(
                    amountMinor = it.setScale(MINOR_SCALE, RoundingMode.HALF_UP).movePointRight(MINOR_SCALE).longValueExact(),
                    currency = currency,
                )
            }
    }
}

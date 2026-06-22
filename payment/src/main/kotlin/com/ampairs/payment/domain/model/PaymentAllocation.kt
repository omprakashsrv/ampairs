package com.ampairs.payment.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.payment.config.Constants
import com.ampairs.payment.domain.enums.AllocationTarget
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal

/**
 * Matching of a payment to a specific outstanding bill. Drives open-bills and aging *only* —
 * it never affects the party balance (that is the sum of [LedgerEntry] rows). Σ amount per
 * voucher must be ≤ the voucher's total.
 */
@Entity(name = "payment_allocation")
@Table(
    indexes = [
        Index(name = "idx_payment_allocation_uid", columnList = "uid", unique = true),
        Index(name = "idx_payment_allocation_voucher", columnList = "owner_id, payment_voucher_uid"),
        Index(name = "idx_payment_allocation_target", columnList = "owner_id, target_type, target_uid"),
    ]
)
class PaymentAllocation : OwnableBaseDomain() {

    @Column(name = "payment_voucher_uid", nullable = false, length = 64)
    var paymentVoucherUid: String = ""

    @Column(name = "target_type", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    var targetType: AllocationTarget = AllocationTarget.INVOICE

    @Column(name = "target_uid", nullable = false, length = 64)
    var targetUid: String = ""

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    var amount: BigDecimal = BigDecimal.ZERO

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.PAYMENT_ALLOCATION_PREFIX
}

package com.ampairs.payment.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.payment.config.Constants
import com.ampairs.payment.domain.enums.AdjustmentType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

/**
 * A non-payment movement (sales return / credit note / purchase / purchase-return / debit note /
 * settlement discount / write-off). Posts one [LedgerEntry] of the mapped type with the
 * deterministic uid `LDG_<adjustment.uid>`.
 */
@Entity(name = "adjustment_voucher")
@Table(
    indexes = [
        Index(name = "idx_adjustment_voucher_uid", columnList = "uid", unique = true),
        Index(name = "idx_adjustment_voucher_party", columnList = "owner_id, party_uid"),
    ]
)
class AdjustmentVoucher : OwnableBaseDomain() {

    @Column(name = "party_uid", nullable = false, length = 40)
    var partyUid: String = ""

    @Column(name = "voucher_no", nullable = true, length = 64)
    var voucherNo: String? = null

    @Column(name = "voucher_date", nullable = false)
    var voucherDate: Instant = Instant.now()

    @Column(name = "adjustment_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var adjustmentType: AdjustmentType = AdjustmentType.ADJUSTMENT

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    var amount: BigDecimal = BigDecimal.ZERO

    @Column(name = "narration", nullable = true, length = 500)
    var narration: String? = null

    @Column(name = "source_ref", nullable = true, length = 64)
    var sourceRef: String? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.ADJUSTMENT_PREFIX
}

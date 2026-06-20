package com.ampairs.payment.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.payment.config.Constants
import com.ampairs.payment.domain.enums.ClearanceStatus
import com.ampairs.payment.domain.enums.PaymentDirection
import com.ampairs.payment.domain.enums.PaymentMode
import jakarta.persistence.BatchSize
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

/**
 * Money-movement header. Posts exactly one [LedgerEntry] (`PAYMENT_IN` CR when RECEIVED /
 * `PAYMENT_OUT` DR when PAID) with the deterministic uid `LDG_<voucher.uid>`.
 */
@NamedEntityGraph(
    name = "PaymentVoucher.withAllocations",
    attributeNodes = [NamedAttributeNode("allocations")]
)
@Entity(name = "payment_voucher")
@Table(
    indexes = [
        Index(name = "idx_payment_voucher_uid", columnList = "uid", unique = true),
        Index(name = "idx_payment_voucher_party", columnList = "owner_id, party_uid"),
    ]
)
class PaymentVoucher : OwnableBaseDomain() {

    @Column(name = "party_uid", nullable = false, length = 40)
    var partyUid: String = ""

    @Column(name = "voucher_no", nullable = true, length = 64)
    var voucherNo: String? = null

    @Column(name = "voucher_date", nullable = false)
    var voucherDate: Instant = Instant.now()

    @Column(name = "direction", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    var direction: PaymentDirection = PaymentDirection.RECEIVED

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    var totalAmount: BigDecimal = BigDecimal.ZERO

    @Column(name = "payment_mode", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var paymentMode: PaymentMode = PaymentMode.CASH

    @Column(name = "reference_number", nullable = true, length = 100)
    var referenceNumber: String? = null

    @Column(name = "instrument_date", nullable = true)
    var instrumentDate: Instant? = null

    @Column(name = "bank_name", nullable = true, length = 120)
    var bankName: String? = null

    @Column(name = "clearance_status", nullable = false, length = 12)
    @Enumerated(EnumType.STRING)
    var clearanceStatus: ClearanceStatus = ClearanceStatus.CLEARED

    /** = totalAmount − Σ active allocations; ≥ 0. */
    @Column(name = "unallocated_amount", nullable = false, precision = 19, scale = 4)
    var unallocatedAmount: BigDecimal = BigDecimal.ZERO

    @Column(name = "narration", nullable = true, length = 500)
    var narration: String? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    @BatchSize(size = 30)
    @OneToMany
    @JoinColumn(
        name = "payment_voucher_uid", referencedColumnName = "uid",
        insertable = false, updatable = false
    )
    var allocations: MutableList<PaymentAllocation> = mutableListOf()

    override fun obtainSeqIdPrefix(): String = Constants.PAYMENT_VOUCHER_PREFIX
}

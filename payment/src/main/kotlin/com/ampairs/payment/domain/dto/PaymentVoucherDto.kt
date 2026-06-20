package com.ampairs.payment.domain.dto

import com.ampairs.payment.domain.enums.ClearanceStatus
import com.ampairs.payment.domain.enums.PaymentDirection
import com.ampairs.payment.domain.enums.PaymentMode
import com.ampairs.payment.domain.model.PaymentVoucher
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant

data class PaymentVoucherRequest(
    val uid: String = "",
    @field:NotBlank val partyUid: String = "",
    val voucherNo: String? = null,
    val voucherDate: Instant? = null,
    val direction: PaymentDirection = PaymentDirection.RECEIVED,
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val referenceNumber: String? = null,
    val instrumentDate: Instant? = null,
    val bankName: String? = null,
    val clearanceStatus: ClearanceStatus? = null,
    val narration: String? = null,
    val active: Boolean = true,
)

data class PaymentVoucherResponse(
    val uid: String = "",
    val partyUid: String = "",
    val voucherNo: String? = null,
    val voucherDate: Instant = Instant.now(),
    val direction: PaymentDirection = PaymentDirection.RECEIVED,
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val referenceNumber: String? = null,
    val instrumentDate: Instant? = null,
    val bankName: String? = null,
    val clearanceStatus: ClearanceStatus = ClearanceStatus.CLEARED,
    val unallocatedAmount: BigDecimal = BigDecimal.ZERO,
    val narration: String? = null,
    val active: Boolean = true,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

fun PaymentVoucherRequest.toEntity(): PaymentVoucher {
    val v = PaymentVoucher()
    v.uid = this.uid
    v.partyUid = this.partyUid
    v.voucherNo = this.voucherNo
    if (this.voucherDate != null) v.voucherDate = this.voucherDate
    v.direction = this.direction
    v.totalAmount = this.totalAmount.abs()
    v.paymentMode = this.paymentMode
    v.referenceNumber = this.referenceNumber
    v.instrumentDate = this.instrumentDate
    v.bankName = this.bankName
    if (this.clearanceStatus != null) v.clearanceStatus = this.clearanceStatus
    v.narration = this.narration
    v.active = this.active
    return v
}

fun PaymentVoucher.asResponse(): PaymentVoucherResponse = PaymentVoucherResponse(
    uid = this.uid,
    partyUid = this.partyUid,
    voucherNo = this.voucherNo,
    voucherDate = this.voucherDate,
    direction = this.direction,
    totalAmount = this.totalAmount,
    paymentMode = this.paymentMode,
    referenceNumber = this.referenceNumber,
    instrumentDate = this.instrumentDate,
    bankName = this.bankName,
    clearanceStatus = this.clearanceStatus,
    unallocatedAmount = this.unallocatedAmount,
    narration = this.narration,
    active = this.active,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
)

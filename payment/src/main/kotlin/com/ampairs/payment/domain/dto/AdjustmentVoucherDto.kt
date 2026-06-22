package com.ampairs.payment.domain.dto

import com.ampairs.payment.domain.enums.AdjustmentType
import com.ampairs.payment.domain.model.AdjustmentVoucher
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant

data class AdjustmentVoucherRequest(
    val uid: String = "",
    @field:NotBlank val partyUid: String = "",
    val voucherNo: String? = null,
    val voucherDate: Instant? = null,
    val adjustmentType: AdjustmentType = AdjustmentType.ADJUSTMENT,
    val amount: BigDecimal = BigDecimal.ZERO,
    val narration: String? = null,
    val sourceRef: String? = null,
    val active: Boolean = true,
)

data class AdjustmentVoucherResponse(
    val uid: String = "",
    val partyUid: String = "",
    val voucherNo: String? = null,
    val voucherDate: Instant = Instant.now(),
    val adjustmentType: AdjustmentType = AdjustmentType.ADJUSTMENT,
    val amount: BigDecimal = BigDecimal.ZERO,
    val narration: String? = null,
    val sourceRef: String? = null,
    val active: Boolean = true,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

fun AdjustmentVoucherRequest.toEntity(): AdjustmentVoucher {
    val a = AdjustmentVoucher()
    a.uid = this.uid
    a.partyUid = this.partyUid
    a.voucherNo = this.voucherNo
    if (this.voucherDate != null) a.voucherDate = this.voucherDate
    a.adjustmentType = this.adjustmentType
    a.amount = this.amount.abs()
    a.narration = this.narration
    a.sourceRef = this.sourceRef
    a.active = this.active
    return a
}

fun AdjustmentVoucher.asResponse(): AdjustmentVoucherResponse = AdjustmentVoucherResponse(
    uid = this.uid,
    partyUid = this.partyUid,
    voucherNo = this.voucherNo,
    voucherDate = this.voucherDate,
    adjustmentType = this.adjustmentType,
    amount = this.amount,
    narration = this.narration,
    sourceRef = this.sourceRef,
    active = this.active,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
)

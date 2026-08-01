package com.ampairs.payment.domain.dto

import com.ampairs.payment.domain.enums.AllocationTarget
import com.ampairs.payment.domain.model.PaymentAllocation
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant

data class PaymentAllocationRequest(
    val uid: String = "",
    @field:NotBlank val paymentVoucherUid: String = "",
    val targetType: AllocationTarget = AllocationTarget.INVOICE,
    @field:NotBlank val targetUid: String = "",
    val amount: BigDecimal = BigDecimal.ZERO,
    val active: Boolean = true,
)

data class PaymentAllocationResponse(
    val uid: String = "",
    val paymentVoucherUid: String = "",
    val targetType: AllocationTarget = AllocationTarget.INVOICE,
    val targetUid: String = "",
    val amount: BigDecimal = BigDecimal.ZERO,
    val active: Boolean = true,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

fun PaymentAllocationRequest.toEntity(): PaymentAllocation {
    val a = PaymentAllocation()
    a.uid = this.uid
    a.paymentVoucherUid = this.paymentVoucherUid
    a.targetType = this.targetType
    a.targetUid = this.targetUid
    a.amount = this.amount.abs()
    a.active = this.active
    return a
}

fun PaymentAllocation.asResponse(): PaymentAllocationResponse = PaymentAllocationResponse(
    uid = this.uid,
    paymentVoucherUid = this.paymentVoucherUid,
    targetType = this.targetType,
    targetUid = this.targetUid,
    amount = this.amount,
    active = this.active,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
)

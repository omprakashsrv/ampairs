package com.ampairs.dms.domain.dto

import com.ampairs.dms.domain.enums.TargetTier
import com.ampairs.dms.domain.model.DistributorStockSnapshot
import com.ampairs.dms.domain.model.SalesTarget
import com.ampairs.dms.domain.model.SecondarySalesSnapshot
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal

data class SecondarySalesRow(
    val periodKey: String,
    val areaCode: String?,
    val brandProductUid: String?,
    val brandSkuCode: String?,
    val quantity: Double,
    val value: BigDecimal,
    val version: Int,
)

fun SecondarySalesSnapshot.asRow(): SecondarySalesRow =
    SecondarySalesRow(periodKey, areaCode, brandProductUid, brandSkuCode, quantity, valueAmount, version)

data class DistributorStockRow(
    val brandProductUid: String?,
    val brandSkuCode: String?,
    val onHandQuantity: Double,
    val version: Int,
)

fun DistributorStockSnapshot.asRow(): DistributorStockRow =
    DistributorStockRow(brandProductUid, brandSkuCode, onHandQuantity, version)

data class SalesTargetRequest(
    val tier: TargetTier = TargetTier.PRIMARY,
    @field:NotBlank val brandWorkspaceId: String? = null,
    val distributorWorkspaceId: String? = null,
    val repMemberUid: String? = null,
    @field:NotBlank val periodKey: String? = null,
    val brandProductUid: String? = null,
    val areaCode: String? = null,
    val targetQuantity: Double? = null,
    val targetValue: BigDecimal? = null,
)

data class SalesTargetResponse(
    val uid: String,
    val tier: TargetTier,
    val brandWorkspaceId: String,
    val distributorWorkspaceId: String?,
    val periodKey: String,
    val targetQuantity: Double,
    val targetValue: BigDecimal,
)

fun SalesTargetRequest.toEntity(): SalesTarget = SalesTarget().apply {
    tier = this@toEntity.tier
    brandWorkspaceId = this@toEntity.brandWorkspaceId ?: ""
    distributorWorkspaceId = this@toEntity.distributorWorkspaceId
    repMemberUid = this@toEntity.repMemberUid
    periodKey = this@toEntity.periodKey ?: ""
    brandProductUid = this@toEntity.brandProductUid
    areaCode = this@toEntity.areaCode
    targetQuantity = this@toEntity.targetQuantity ?: 0.0
    targetValue = this@toEntity.targetValue ?: BigDecimal.ZERO
}

fun SalesTarget.asResponse(): SalesTargetResponse =
    SalesTargetResponse(uid, tier, brandWorkspaceId, distributorWorkspaceId, periodKey, targetQuantity, targetValue)

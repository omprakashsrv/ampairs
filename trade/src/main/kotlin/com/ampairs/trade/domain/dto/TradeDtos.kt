package com.ampairs.trade.domain.dto

import com.ampairs.trade.domain.enums.LinkStatus
import com.ampairs.trade.domain.enums.RetailerVisibility
import com.ampairs.trade.domain.model.ConsentScope
import com.ampairs.trade.domain.model.NetworkBrand
import com.ampairs.trade.domain.model.PrimaryOrderLink
import com.ampairs.trade.domain.model.SchemePublication
import com.ampairs.trade.domain.model.TradeLink
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class ConsentScopeDto(
    val retailerVisibility: RetailerVisibility = RetailerVisibility.CODED,
    val shareSecondarySales: Boolean = true,
    val shareStock: Boolean = true,
    val shareTargets: Boolean = true,
)

fun ConsentScopeDto.toEntity(): ConsentScope = ConsentScope().apply {
    retailerVisibility = this@toEntity.retailerVisibility
    shareSecondarySales = this@toEntity.shareSecondarySales
    shareStock = this@toEntity.shareStock
    shareTargets = this@toEntity.shareTargets
}

fun ConsentScope.toDto(): ConsentScopeDto = ConsentScopeDto(
    retailerVisibility, shareSecondarySales, shareStock, shareTargets,
)

data class TradeLinkInviteRequest(
    @field:NotBlank(message = "brand_workspace_id is required")
    val brandWorkspaceId: String? = null,
    @field:NotBlank(message = "distributor_workspace_id is required")
    val distributorWorkspaceId: String? = null,
    val scope: ConsentScopeDto? = null,
)

data class TradeLinkAcceptRequest(val scope: ConsentScopeDto? = null)

data class TradeLinkResponse(
    val uid: String,
    val brandWorkspaceId: String,
    val distributorWorkspaceId: String,
    val status: LinkStatus,
    val scope: ConsentScopeDto,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun TradeLink.asResponse(): TradeLinkResponse = TradeLinkResponse(
    uid, brandWorkspaceId, distributorWorkspaceId, status, consentScope.toDto(), createdAt, updatedAt,
)

data class NetworkBrandRequest(
    @field:NotBlank val linkUid: String? = null,
    @field:NotBlank val distributorProductBrandUid: String? = null,
)

data class NetworkBrandResponse(
    val uid: String,
    val linkUid: String,
    val distributorProductBrandUid: String,
    val brandWorkspaceId: String,
    val status: String,
)

fun NetworkBrand.asResponse(): NetworkBrandResponse =
    NetworkBrandResponse(uid, linkUid, distributorProductBrandUid, brandWorkspaceId, status.name)

data class SchemePublishRequest(
    @field:NotBlank val linkUid: String? = null,
    @field:NotBlank val schemeRef: String? = null,
)

data class SchemePublicationResponse(
    val uid: String,
    val linkUid: String,
    val schemeRef: String,
    val status: String,
)

fun SchemePublication.asResponse(): SchemePublicationResponse =
    SchemePublicationResponse(uid, linkUid, schemeRef, status.name)

data class PrimaryOrderPlaceRequest(
    @field:NotBlank val brandWorkspaceId: String? = null,
    @field:NotBlank val distributorWorkspaceId: String? = null,
    @field:NotBlank val brandOrderUid: String? = null,
)

data class PrimaryOrderConfirmRequest(
    @field:NotBlank val distributorOrderUid: String? = null,
)

data class PrimaryOrderResponse(
    val uid: String,
    val brandWorkspaceId: String,
    val distributorWorkspaceId: String,
    val brandOrderUid: String,
    val distributorOrderUid: String?,
    val status: String,
)

fun PrimaryOrderLink.asResponse(): PrimaryOrderResponse =
    PrimaryOrderResponse(uid, brandWorkspaceId, distributorWorkspaceId, brandOrderUid, distributorOrderUid, status.name)

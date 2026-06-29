package com.ampairs.trade.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.trade.domain.dto.NetworkBrandRequest
import com.ampairs.trade.domain.dto.NetworkBrandResponse
import com.ampairs.trade.domain.dto.PrimaryOrderConfirmRequest
import com.ampairs.trade.domain.dto.PrimaryOrderPlaceRequest
import com.ampairs.trade.domain.dto.PrimaryOrderResponse
import com.ampairs.trade.domain.dto.SchemePublicationResponse
import com.ampairs.trade.domain.dto.SchemePublishRequest
import com.ampairs.trade.domain.dto.TradeLinkAcceptRequest
import com.ampairs.trade.domain.dto.TradeLinkInviteRequest
import com.ampairs.trade.domain.dto.TradeLinkResponse
import com.ampairs.trade.domain.dto.asResponse
import com.ampairs.trade.domain.dto.toEntity
import com.ampairs.trade.service.NetworkBrandService
import com.ampairs.trade.service.PrimaryOrderService
import com.ampairs.trade.service.SchemePublicationService
import com.ampairs.trade.service.TradeLinkService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Brand↔distributor network actions: link invite/accept/decline/revoke (the consent edge),
 * Hop-A brand designation, scheme publish-down-link (pricing/015), and the primary-order handshake.
 * Errors bubble to TradeExceptionHandler (403/409/422). Base path `/trade/v1`.
 */
@RestController
@RequestMapping("/trade/v1")
class TradeNetworkController(
    private val tradeLinkService: TradeLinkService,
    private val networkBrandService: NetworkBrandService,
    private val schemePublicationService: SchemePublicationService,
    private val primaryOrderService: PrimaryOrderService,
) {

    // ──────────── links ────────────
    @PostMapping("/links")
    fun invite(@RequestBody @Valid request: TradeLinkInviteRequest): ApiResponse<TradeLinkResponse> =
        ApiResponse.success(
            tradeLinkService.invite(
                request.brandWorkspaceId!!, request.distributorWorkspaceId!!, request.scope?.toEntity(),
            ).asResponse(),
        )

    @PostMapping("/links/{uid}/accept")
    fun accept(@PathVariable uid: String, @RequestBody(required = false) request: TradeLinkAcceptRequest?): ApiResponse<TradeLinkResponse> =
        ApiResponse.success(tradeLinkService.accept(uid, request?.scope?.toEntity()).asResponse())

    @PostMapping("/links/{uid}/decline")
    fun decline(@PathVariable uid: String): ApiResponse<TradeLinkResponse> =
        ApiResponse.success(tradeLinkService.decline(uid).asResponse())

    @PostMapping("/links/{uid}/revoke")
    fun revoke(@PathVariable uid: String): ApiResponse<TradeLinkResponse> =
        ApiResponse.success(tradeLinkService.revoke(uid).asResponse())

    // ──────────── Hop A brand designation ────────────
    @PostMapping("/network-brands")
    fun designate(@RequestBody @Valid request: NetworkBrandRequest): ApiResponse<NetworkBrandResponse> =
        ApiResponse.success(networkBrandService.designate(request.linkUid!!, request.distributorProductBrandUid!!).asResponse())

    @GetMapping("/network-brands")
    fun listDesignations(@RequestParam("link_uid") linkUid: String): ApiResponse<List<NetworkBrandResponse>> =
        ApiResponse.success(networkBrandService.list(linkUid).map { it.asResponse() })

    // ──────────── scheme publication (definition = pricing/015) ────────────
    @PostMapping("/links/{uid}/schemes")
    fun publishScheme(@PathVariable uid: String, @RequestBody @Valid request: SchemePublishRequest): ApiResponse<SchemePublicationResponse> =
        ApiResponse.success(schemePublicationService.publish(uid, request.schemeRef!!).asResponse())

    @GetMapping("/schemes")
    fun listSchemes(@RequestParam("link_uid") linkUid: String): ApiResponse<List<SchemePublicationResponse>> =
        ApiResponse.success(schemePublicationService.listPublished(linkUid).map { it.asResponse() })

    // ──────────── primary-order handshake ────────────
    @PostMapping("/primary-orders")
    fun place(@RequestBody @Valid request: PrimaryOrderPlaceRequest): ApiResponse<PrimaryOrderResponse> =
        ApiResponse.success(
            primaryOrderService.place(request.brandWorkspaceId!!, request.distributorWorkspaceId!!, request.brandOrderUid!!).asResponse(),
        )

    @PostMapping("/primary-orders/{uid}/confirm")
    fun confirm(@PathVariable uid: String, @RequestBody @Valid request: PrimaryOrderConfirmRequest): ApiResponse<PrimaryOrderResponse> =
        ApiResponse.success(primaryOrderService.confirm(uid, request.distributorOrderUid!!).asResponse())

    @PostMapping("/primary-orders/{uid}/reject")
    fun reject(@PathVariable uid: String): ApiResponse<PrimaryOrderResponse> =
        ApiResponse.success(primaryOrderService.reject(uid).asResponse())
}

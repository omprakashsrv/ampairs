package com.ampairs.pricing.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.pricing.domain.dto.OfferRequest
import com.ampairs.pricing.domain.dto.OfferResponse
import com.ampairs.pricing.exception.OfferNotFoundException
import com.ampairs.pricing.service.OfferService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/pricing/v1/offers")
@Validated
class OfferController(
    private val offerService: OfferService,
) {

    @GetMapping("/sync")
    fun getOffersSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<OfferResponse>> {
        val jpaSort = when (sortBy) {
            "name" -> "name"
            "priority" -> "priority"
            "status" -> "status"
            "createdAt" -> "createdAt"
            else -> "updatedAt"
        }
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), jpaSort))
        return ApiResponse.success(PageResponse.from(offerService.getAfterSync(lastSync, pageable)))
    }

    @PostMapping("/sync")
    fun bulkUpsertOffers(
        @RequestBody requests: List<@Valid OfferRequest>,
    ): ApiResponse<List<OfferResponse>> =
        ApiResponse.success(offerService.bulkUpsert(requests))

    @GetMapping("/{uid}")
    fun getOffer(@PathVariable uid: String): ApiResponse<OfferResponse> {
        val offer = offerService.findByUid(uid)
            ?: throw OfferNotFoundException("Offer not found for uid: $uid")
        return ApiResponse.success(offer)
    }
}

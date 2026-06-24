package com.ampairs.pricing.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.pricing.domain.dto.GeoZoneRequest
import com.ampairs.pricing.domain.dto.GeoZoneResponse
import com.ampairs.pricing.exception.GeoZoneNotFoundException
import com.ampairs.pricing.service.GeoZoneService
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
@RequestMapping("/pricing/v1/geo-zones")
@Validated
class GeoZoneController(
    private val geoZoneService: GeoZoneService,
) {

    @GetMapping("/sync")
    fun getZonesSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<GeoZoneResponse>> {
        val jpaSort = if (sortBy == "name") "name" else "updatedAt"
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), jpaSort))
        return ApiResponse.success(PageResponse.from(geoZoneService.getAfterSync(lastSync, pageable)))
    }

    @PostMapping("/sync")
    fun bulkUpsertZones(
        @RequestBody requests: List<@Valid GeoZoneRequest>,
    ): ApiResponse<List<GeoZoneResponse>> =
        ApiResponse.success(geoZoneService.bulkUpsert(requests))

    @GetMapping("/{uid}")
    fun getZone(@PathVariable uid: String): ApiResponse<GeoZoneResponse> {
        val zone = geoZoneService.findByUid(uid)
            ?: throw GeoZoneNotFoundException("Geo-zone not found for uid: $uid")
        return ApiResponse.success(zone)
    }
}

package com.ampairs.cb_store.controller

import com.ampairs.cb_store.domain.dto.ZonalOfficeRequest
import com.ampairs.cb_store.domain.dto.ZonalOfficeResponse
import com.ampairs.cb_store.exception.ZonalOfficeNotFoundException
import com.ampairs.cb_store.service.ZonalOfficeService
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
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
@RequestMapping("/cb_store/v1/zonal-offices")
@Validated
class ZonalOfficeController(
    private val zonalOfficeService: ZonalOfficeService,
) {

    @GetMapping("/sync")
    fun getZonalOfficesSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<ZonalOfficeResponse>> {
        val jpaPropertyName = when (sortBy) {
            "name" -> "name"
            "city" -> "city"
            "active" -> "active"
            "createdAt" -> "createdAt"
            "updatedAt" -> "updatedAt"
            else -> "updatedAt"
        }
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), jpaPropertyName))
        return ApiResponse.success(PageResponse.from(zonalOfficeService.getZonalOfficesAfterSync(lastSync, pageable)))
    }

    @PostMapping("/sync")
    fun bulkUpsertZonalOffices(
        @RequestBody requests: List<@Valid ZonalOfficeRequest>,
    ): ApiResponse<List<ZonalOfficeResponse>> {
        return ApiResponse.success(zonalOfficeService.bulkUpsert(requests))
    }

    @GetMapping("/{uid}")
    fun getZonalOffice(@PathVariable uid: String): ApiResponse<ZonalOfficeResponse> {
        val office = zonalOfficeService.findByUid(uid)
            ?: throw ZonalOfficeNotFoundException("Zonal office not found for uid: $uid")
        return ApiResponse.success(office)
    }
}

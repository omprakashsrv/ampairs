package com.ampairs.unit.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.unit.domain.dto.UnitRequest
import com.ampairs.unit.domain.dto.UnitResponse
import com.ampairs.unit.domain.dto.UnitUsageResponse
import com.ampairs.unit.exception.UnitNotFoundException
import com.ampairs.unit.service.UnitService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/unit/v1/units")
@Validated
class UnitController(
    private val unitService: UnitService
) {

    @GetMapping
    fun listUnits(
        @RequestParam(defaultValue = "true") active: Boolean,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "20") size: Int,
        @RequestParam("sort_by", defaultValue = "name") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String
    ): ApiResponse<PageResponse<UnitResponse>> {
        val jpaPropertyName = when (sortBy) {
            "name" -> "name"
            "shortName" -> "shortName"
            "category" -> "category"
            "decimalPlaces" -> "decimalPlaces"
            "active" -> "active"
            "createdAt" -> "createdAt"
            "updatedAt" -> "updatedAt"
            else -> "name"
        }
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), jpaPropertyName))
        return ApiResponse.success(PageResponse.from(unitService.findAllPaged(active, pageable)))
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createUnit(@Valid @RequestBody request: UnitRequest): ApiResponse<UnitResponse> {
        return ApiResponse.success(unitService.create(request))
    }

    @GetMapping("/{uid}")
    fun getUnit(@PathVariable uid: String): ApiResponse<UnitResponse> {
        val unit = unitService.findByUid(uid)
            ?: throw UnitNotFoundException("Unit not found for uid: $uid")
        return ApiResponse.success(unit)
    }

    @PutMapping("/{uid}")
    fun updateUnit(@PathVariable uid: String, @Valid @RequestBody request: UnitRequest): ApiResponse<UnitResponse> {
        return ApiResponse.success(unitService.update(uid, request))
    }

    @DeleteMapping("/{uid}")
    fun deleteUnit(@PathVariable uid: String): ApiResponse<Unit> {
        unitService.delete(uid)
        return ApiResponse.success(Unit)
    }

    @GetMapping("/{uid}/usage")
    fun getUsage(@PathVariable uid: String): ApiResponse<UnitUsageResponse> {
        return ApiResponse.success(unitService.getUsage(uid))
    }

}

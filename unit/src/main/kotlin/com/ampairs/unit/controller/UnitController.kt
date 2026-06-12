package com.ampairs.unit.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.unit.domain.dto.UnitRequest
import com.ampairs.unit.domain.dto.UnitResponse
import com.ampairs.unit.exception.UnitNotFoundException
import com.ampairs.unit.service.UnitService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/unit/v1/units")
@Validated
class UnitController(
    private val unitService: UnitService
) {

    /**
     * Incremental sync feed: units updated at/after last_sync, INCLUDING inactive
     * (soft-deleted) rows, ordered by updatedAt ASC, paginated. Lets mobile clients
     * do batched incremental pulls and detect deletions.
     */
    @GetMapping("/sync")
    fun getUnitsSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
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
            else -> "updatedAt"
        }
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), jpaPropertyName))
        return ApiResponse.success(PageResponse.from(unitService.getUnitsAfterSync(lastSync, pageable)))
    }

    /**
     * Bulk upsert units keyed by uid (create if absent, update if present). Soft-deleted
     * rows (active = false) are accepted in-band so deletions propagate without a per-row DELETE.
     */
    @PostMapping("/sync")
    fun bulkUpsertUnits(
        @RequestBody requests: List<@Valid UnitRequest>
    ): ApiResponse<List<UnitResponse>> {
        return ApiResponse.success(unitService.bulkUpsert(requests))
    }

    @GetMapping("/{uid}")
    fun getUnit(@PathVariable uid: String): ApiResponse<UnitResponse> {
        val unit = unitService.findByUid(uid)
            ?: throw UnitNotFoundException("Unit not found for uid: $uid")
        return ApiResponse.success(unit)
    }

}

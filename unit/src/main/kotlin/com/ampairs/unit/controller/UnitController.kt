package com.ampairs.unit.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.ErrorCodes
import com.ampairs.unit.domain.dto.UnitRequest
import com.ampairs.unit.domain.dto.UnitResponse
import com.ampairs.unit.domain.dto.UnitUsageResponse
import com.ampairs.unit.exception.UnitInUseException
import com.ampairs.unit.exception.UnitNotFoundException
import com.ampairs.unit.service.UnitService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/unit")
@Validated
class UnitController(
    private val unitService: UnitService
) {

    @GetMapping
    fun listUnits(@RequestParam(defaultValue = "true") active: Boolean): ApiResponse<List<UnitResponse>> {
        return ApiResponse.success(unitService.findAll(activeOnly = active))
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

    @ExceptionHandler(UnitNotFoundException::class)
    fun handleUnitNotFound(ex: UnitNotFoundException): ResponseEntity<ApiResponse<Unit>> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ErrorCodes.NOT_FOUND, ex.message ?: "Unit not found"))
    }

    @ExceptionHandler(UnitInUseException::class)
    fun handleUnitInUse(ex: UnitInUseException): ResponseEntity<ApiResponse<Unit>> {
        val details = buildString {
            if (ex.entityIds.isNotEmpty()) append("Entities: ${ex.entityIds.joinToString()}")
            if (ex.conversionIds.isNotEmpty()) {
                if (isNotEmpty()) append("; ")
                append("Conversions: ${ex.conversionIds.joinToString()}")
            }
        }.ifBlank { null }

        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(code = ErrorCodes.CONSTRAINT_VIOLATION,
                message = ex.message ?: "Unit is currently in use", details = details))
    }
}

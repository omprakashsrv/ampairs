package com.ampairs.unit.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.ErrorCodes
import com.ampairs.core.multitenancy.TenantContextHolder
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
    fun listUnits(
        @RequestHeader(name = WORKSPACE_HEADER, required = false) workspaceId: String?,
        @RequestParam(defaultValue = "true") active: Boolean
    ): ResponseEntity<ApiResponse<List<UnitResponse>>> {
        setTenant(workspaceId)
        val units = unitService.findAll(activeOnly = active)
        return ResponseEntity.ok(ApiResponse.success(units))
    }

    @PostMapping
    fun createUnit(
        @RequestHeader(name = WORKSPACE_HEADER, required = false) workspaceId: String?,
        @Valid @RequestBody request: UnitRequest
    ): ResponseEntity<ApiResponse<UnitResponse>> {
        setTenant(workspaceId)
        val created = unitService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created))
    }

    @GetMapping("/{uid}")
    fun getUnit(
        @RequestHeader(name = WORKSPACE_HEADER, required = false) workspaceId: String?,
        @PathVariable uid: String
    ): ResponseEntity<ApiResponse<UnitResponse>> {
        setTenant(workspaceId)
        val unit = unitService.findByUid(uid)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ErrorCodes.NOT_FOUND, "Unit not found for uid: $uid"))
        return ResponseEntity.ok(ApiResponse.success(unit))
    }

    @PutMapping("/{uid}")
    fun updateUnit(
        @RequestHeader(name = WORKSPACE_HEADER, required = false) workspaceId: String?,
        @PathVariable uid: String,
        @Valid @RequestBody request: UnitRequest
    ): ResponseEntity<ApiResponse<UnitResponse>> {
        setTenant(workspaceId)
        val updated = unitService.update(uid, request)
        return ResponseEntity.ok(ApiResponse.success(updated))
    }

    @DeleteMapping("/{uid}")
    fun deleteUnit(
        @RequestHeader(name = WORKSPACE_HEADER, required = false) workspaceId: String?,
        @PathVariable uid: String
    ): ResponseEntity<ApiResponse<Unit>> {
        setTenant(workspaceId)
        unitService.delete(uid)
        return ResponseEntity.ok(ApiResponse.success(Unit))
    }

    @GetMapping("/{uid}/usage")
    fun getUsage(
        @RequestHeader(name = WORKSPACE_HEADER, required = false) workspaceId: String?,
        @PathVariable uid: String
    ): ResponseEntity<ApiResponse<UnitUsageResponse>> {
        setTenant(workspaceId)
        val usage = unitService.getUsage(uid)
        return ResponseEntity.ok(ApiResponse.success(usage))
    }

    @ExceptionHandler(UnitNotFoundException::class)
    fun handleUnitNotFound(ex: UnitNotFoundException): ResponseEntity<ApiResponse<Unit>> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ErrorCodes.NOT_FOUND, ex.message ?: "Unit not found"))
    }

    @ExceptionHandler(UnitInUseException::class)
    fun handleUnitInUse(ex: UnitInUseException): ResponseEntity<ApiResponse<Unit>> {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(
                ApiResponse.error(
                    code = ErrorCodes.CONSTRAINT_VIOLATION,
                    message = ex.message ?: "Unit is currently in use",
                    details = buildString {
                        if (ex.productIds.isNotEmpty()) {
                            append("Products: ${ex.productIds.joinToString()}")
                        }
                        if (ex.conversionIds.isNotEmpty()) {
                            if (isNotEmpty()) append("; ")
                            append("Conversions: ${ex.conversionIds.joinToString()}")
                        }
                    }.ifBlank { null }
                )
            )
    }

    private fun setTenant(workspaceId: String?) {
        val tenant = workspaceId?.takeIf { it.isNotBlank() } ?: TenantContextHolder.getCurrentTenant()
        tenant?.let { TenantContextHolder.setCurrentTenant(it) }
    }

    companion object {
        private const val WORKSPACE_HEADER = "X-Workspace-ID"
    }
}

package com.ampairs.unit.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.ErrorCodes
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.unit.domain.dto.ConvertQuantityRequest
import com.ampairs.unit.domain.dto.ConvertedQuantityResponse
import com.ampairs.unit.domain.dto.UnitConversionRequest
import com.ampairs.unit.domain.dto.UnitConversionResponse
import com.ampairs.unit.exception.CircularConversionException
import com.ampairs.unit.exception.UnitNotFoundException
import com.ampairs.unit.service.UnitConversionService
import com.ampairs.unit.service.UnitService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/unit/conversion")
@Validated
class UnitConversionController(
    private val unitConversionService: UnitConversionService,
    private val unitService: UnitService
) {

    @GetMapping
    fun listConversions(
        @RequestHeader(name = WORKSPACE_HEADER, required = false) workspaceId: String?,
        @RequestParam(required = false) entityId: String?,
        @RequestParam(required = false) baseUnitId: String?,
        @RequestParam(required = false) derivedUnitId: String?
    ): ResponseEntity<ApiResponse<List<UnitConversionResponse>>> {
        setTenant(workspaceId)
        val conversions = when {
            !entityId.isNullOrBlank() -> unitConversionService.findByEntityId(entityId)
            else -> unitConversionService.findAll()
        }.filter { conversion ->
            (baseUnitId.isNullOrBlank() || conversion.baseUnitId == baseUnitId) &&
                (derivedUnitId.isNullOrBlank() || conversion.derivedUnitId == derivedUnitId)
        }
        return ResponseEntity.ok(ApiResponse.success(conversions))
    }

    @GetMapping("/{uid}")
    fun getConversion(
        @RequestHeader(name = WORKSPACE_HEADER, required = false) workspaceId: String?,
        @PathVariable uid: String
    ): ResponseEntity<ApiResponse<UnitConversionResponse>> {
        setTenant(workspaceId)
        val conversion = unitConversionService.findByUid(uid)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ErrorCodes.NOT_FOUND, "Unit conversion not found for uid: $uid"))
        return ResponseEntity.ok(ApiResponse.success(conversion))
    }

    @PostMapping
    fun createConversion(
        @RequestHeader(name = WORKSPACE_HEADER, required = false) workspaceId: String?,
        @Valid @RequestBody request: UnitConversionRequest
    ): ResponseEntity<ApiResponse<UnitConversionResponse>> {
        setTenant(workspaceId)
        val created = unitConversionService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created))
    }

    @PutMapping("/{uid}")
    fun updateConversion(
        @RequestHeader(name = WORKSPACE_HEADER, required = false) workspaceId: String?,
        @PathVariable uid: String,
        @Valid @RequestBody request: UnitConversionRequest
    ): ResponseEntity<ApiResponse<UnitConversionResponse>> {
        setTenant(workspaceId)
        val updated = unitConversionService.update(uid, request)
        return ResponseEntity.ok(ApiResponse.success(updated))
    }

    @DeleteMapping("/{uid}")
    fun deleteConversion(
        @RequestHeader(name = WORKSPACE_HEADER, required = false) workspaceId: String?,
        @PathVariable uid: String
    ): ResponseEntity<ApiResponse<Unit>> {
        setTenant(workspaceId)
        unitConversionService.delete(uid)
        return ResponseEntity.ok(ApiResponse.success(Unit))
    }

    @PostMapping("/convert")
    fun convertQuantity(
        @RequestHeader(name = WORKSPACE_HEADER, required = false) workspaceId: String?,
        @Valid @RequestBody request: ConvertQuantityRequest
    ): ResponseEntity<ApiResponse<ConvertedQuantityResponse>> {
        setTenant(workspaceId)
        val convertedQuantity = unitConversionService.convert(
            quantity = request.quantity.toDouble(),
            fromUnitId = request.fromUnitId,
            toUnitId = request.toUnitId,
            entityId = request.entityId
        )

        val multiplier = convertedQuantity / request.quantity.toDouble()
        val response = ConvertedQuantityResponse(
            originalQuantity = request.quantity,
            originalUnitId = request.fromUnitId,
            convertedQuantity = convertedQuantity.toBigDecimal(),
            convertedUnitId = request.toUnitId,
            multiplier = multiplier.toBigDecimal()
        )

        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @ExceptionHandler(UnitNotFoundException::class)
    fun handleConversionNotFound(ex: UnitNotFoundException): ResponseEntity<ApiResponse<Unit>> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ErrorCodes.NOT_FOUND, ex.message ?: "Unit conversion not found"))
    }

    @ExceptionHandler(CircularConversionException::class)
    fun handleCircularConversion(ex: CircularConversionException): ResponseEntity<ApiResponse<Unit>> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                ApiResponse.error(
                    code = ErrorCodes.BAD_REQUEST,
                    message = ex.message ?: "Circular conversion detected",
                    details = ex.cycle.joinToString(" -> ")
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

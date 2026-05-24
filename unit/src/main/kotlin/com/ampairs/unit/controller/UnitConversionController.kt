package com.ampairs.unit.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.unit.domain.dto.ConvertQuantityRequest
import com.ampairs.unit.domain.dto.ConvertedQuantityResponse
import com.ampairs.unit.domain.dto.UnitConversionRequest
import com.ampairs.unit.domain.dto.UnitConversionResponse
import com.ampairs.unit.exception.UnitNotFoundException
import com.ampairs.unit.service.UnitConversionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/unit/v1/conversions")
@Validated
class UnitConversionController(
    private val unitConversionService: UnitConversionService,
) {

    @GetMapping
    fun listConversions(
        @RequestParam(required = false) entityId: String?,
        @RequestParam(required = false) baseUnitId: String?,
        @RequestParam(required = false) derivedUnitId: String?
    ): ApiResponse<List<UnitConversionResponse>> {
        val conversions = when {
            !entityId.isNullOrBlank() -> unitConversionService.findByEntityId(entityId)
            else -> unitConversionService.findAll()
        }.filter { conversion ->
            (baseUnitId.isNullOrBlank() || conversion.baseUnitId == baseUnitId) &&
                (derivedUnitId.isNullOrBlank() || conversion.derivedUnitId == derivedUnitId)
        }
        return ApiResponse.success(conversions)
    }

    @GetMapping("/{uid}")
    fun getConversion(@PathVariable uid: String): ApiResponse<UnitConversionResponse> {
        val conversion = unitConversionService.findByUid(uid)
            ?: throw UnitNotFoundException("Unit conversion not found for uid: $uid")
        return ApiResponse.success(conversion)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createConversion(@Valid @RequestBody request: UnitConversionRequest): ApiResponse<UnitConversionResponse> {
        return ApiResponse.success(unitConversionService.create(request))
    }

    @PutMapping("/{uid}")
    fun updateConversion(@PathVariable uid: String, @Valid @RequestBody request: UnitConversionRequest): ApiResponse<UnitConversionResponse> {
        return ApiResponse.success(unitConversionService.update(uid, request))
    }

    @DeleteMapping("/{uid}")
    fun deleteConversion(@PathVariable uid: String): ApiResponse<Unit> {
        unitConversionService.delete(uid)
        return ApiResponse.success(Unit)
    }

    @PostMapping("/convert")
    fun convertQuantity(@Valid @RequestBody request: ConvertQuantityRequest): ApiResponse<ConvertedQuantityResponse> {
        val convertedQuantity = unitConversionService.convert(
            quantity = request.quantity.toDouble(),
            fromUnitId = request.fromUnitId,
            toUnitId = request.toUnitId,
            entityId = request.entityId
        )
        val multiplier = convertedQuantity / request.quantity.toDouble()
        return ApiResponse.success(ConvertedQuantityResponse(
            originalQuantity = request.quantity,
            originalUnitId = request.fromUnitId,
            convertedQuantity = convertedQuantity.toBigDecimal(),
            convertedUnitId = request.toUnitId,
            multiplier = multiplier.toBigDecimal()
        ))
    }

}

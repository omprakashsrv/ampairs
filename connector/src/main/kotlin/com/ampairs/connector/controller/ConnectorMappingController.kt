package com.ampairs.connector.controller

import com.ampairs.connector.domain.dto.FieldMappingDto
import com.ampairs.connector.service.ConnectorMappingService
import com.ampairs.core.domain.dto.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Per-entity field mappings for an installation. */
@RestController
@RequestMapping("/connector/v1/installations/{uid}/mappings")
class ConnectorMappingController(
    private val mappingService: ConnectorMappingService,
) {
    @GetMapping
    fun list(@PathVariable uid: String): ApiResponse<List<FieldMappingDto>> =
        ApiResponse.success(mappingService.list(uid))

    /** Upsert the mapping for one entity type (validated; bumps version). */
    @PutMapping
    fun upsert(@PathVariable uid: String, @RequestBody mapping: FieldMappingDto): ApiResponse<FieldMappingDto> =
        ApiResponse.success(mappingService.upsert(uid, mapping.entityType, mapping.rules))
}

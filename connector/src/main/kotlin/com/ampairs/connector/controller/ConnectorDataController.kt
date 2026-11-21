package com.ampairs.connector.controller

import com.ampairs.connector.domain.dto.SparseUpsertResult
import com.ampairs.connector.domain.dto.SparseUpsertRow
import com.ampairs.connector.service.ConnectorSparseUpsertService
import com.ampairs.core.domain.dto.ApiResponse
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Connector sparse-upsert data write path (FR-018). SEPARATE from the global
 * `/{module}/v1/{resource}/sync` contract (FR-018a). Body is a list of sparse rows where key presence
 * in `values` decides which columns are written; omitted columns are preserved.
 */
@RestController
@RequestMapping("/connector/v1/installations/{uid}/data")
class ConnectorDataController(
    private val sparseUpsertService: ConnectorSparseUpsertService,
) {
    @PostMapping("/{entityType}/upsert")
    fun upsert(
        @PathVariable uid: String,
        @PathVariable entityType: String,
        @RequestBody rows: List<SparseUpsertRow>,
    ): ApiResponse<List<SparseUpsertResult>> =
        ApiResponse.success(sparseUpsertService.upsert(uid, entityType, rows))
}

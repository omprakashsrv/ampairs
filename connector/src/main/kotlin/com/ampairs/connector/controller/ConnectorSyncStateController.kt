package com.ampairs.connector.controller

import com.ampairs.connector.domain.dto.SyncCheckpointDto
import com.ampairs.connector.domain.dto.SyncRunDto
import com.ampairs.connector.service.ConnectorSyncStateService
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** Sync checkpoints + run history reported by the client and read back. */
@RestController
@RequestMapping("/connector/v1/installations/{uid}")
class ConnectorSyncStateController(
    private val syncStateService: ConnectorSyncStateService,
) {
    @GetMapping("/checkpoints")
    fun listCheckpoints(@PathVariable uid: String): ApiResponse<List<SyncCheckpointDto>> =
        ApiResponse.success(syncStateService.listCheckpoints(uid))

    @PutMapping("/checkpoints")
    fun upsertCheckpoint(@PathVariable uid: String, @RequestBody dto: SyncCheckpointDto): ApiResponse<SyncCheckpointDto> =
        ApiResponse.success(syncStateService.upsertCheckpoint(uid, dto))

    @GetMapping("/runs")
    fun listRuns(
        @PathVariable uid: String,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "50") size: Int,
    ): ApiResponse<PageResponse<SyncRunDto>> {
        val runs = syncStateService.listRuns(uid, PageRequest.of(page, size))
        return ApiResponse.success(PageResponse.from(runs))
    }

    @PostMapping("/runs")
    fun recordRun(@PathVariable uid: String, @RequestBody dto: SyncRunDto): ApiResponse<SyncRunDto> =
        ApiResponse.success(syncStateService.recordRun(uid, dto))
}

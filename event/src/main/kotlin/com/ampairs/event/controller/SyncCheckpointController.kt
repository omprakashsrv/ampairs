package com.ampairs.event.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.event.domain.dto.SyncCheckpointResponse
import com.ampairs.event.service.SyncCheckpointService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * Sync bootstrap endpoint. The mobile client calls this on workspace connect, on reconnect, and on
 * an hourly timer, then marks `PENDING_PULL` only the entities whose server `updatedAt` is ahead of
 * its own last-synced timestamp.
 *
 * Workspace scoping comes from the ambient tenant context (set upstream from `X-Workspace-ID`); the
 * underlying checkpoint queries are `@TenantId`-filtered to the current workspace.
 */
@RestController
@RequestMapping("/event/v1/sync")
class SyncCheckpointController(
    private val checkpointService: SyncCheckpointService
) {

    @GetMapping("/checkpoints")
    fun getCheckpoints(): ApiResponse<SyncCheckpointResponse> {
        val checkpoints = checkpointService.checkpoints()
        return ApiResponse.success(
            SyncCheckpointResponse(checkpoints = checkpoints, serverTime = Instant.now())
        )
    }
}

package com.ampairs.event.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.event.domain.dto.DeviceSessionResponse
import com.ampairs.event.domain.dto.asDeviceSessionResponses
import com.ampairs.event.service.DeviceStatusService
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/devices")
class DeviceStatusController(
    private val deviceStatusService: DeviceStatusService
) {

    /**
     * Get active devices/sessions for current workspace
     */
    @GetMapping("/active")
    fun getActiveDevices(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): ApiResponse<List<DeviceSessionResponse>> {
        val workspaceId = com.ampairs.core.multitenancy.TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No workspace context")

        val pageable = PageRequest.of(page, size)
        val sessions = deviceStatusService.getActiveSessions(workspaceId, pageable)

        return ApiResponse.success(sessions.content.asDeviceSessionResponses())
    }

    /**
     * Get active devices for a specific user
     */
    @GetMapping("/active/user/{userId}")
    fun getActiveDevicesByUser(
        @PathVariable userId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<List<DeviceSessionResponse>> {
        val workspaceId = com.ampairs.core.multitenancy.TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No workspace context")

        val pageable = PageRequest.of(page, size)
        val sessions = deviceStatusService.getActiveSessionsByUser(workspaceId, userId, pageable)

        return ApiResponse.success(sessions.content.asDeviceSessionResponses())
    }

    /**
     * Get count of active devices in workspace
     */
    @GetMapping("/active/count")
    fun getActiveDeviceCount(): ApiResponse<Long> {
        val workspaceId = com.ampairs.core.multitenancy.TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No workspace context")

        val count = deviceStatusService.countActiveSessions(workspaceId)
        return ApiResponse.success(count)
    }
}

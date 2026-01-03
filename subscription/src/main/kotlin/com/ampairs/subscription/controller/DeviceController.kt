package com.ampairs.subscription.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.subscription.domain.dto.*
import com.ampairs.subscription.domain.model.SubscriptionAccessMode
import com.ampairs.subscription.domain.service.DeviceRegistrationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/devices")
@Tag(name = "Device Management", description = "APIs for device registration and management")
class DeviceController(
    private val deviceRegistrationService: DeviceRegistrationService
) {

    @PostMapping("/register")
    @Operation(summary = "Register a new device")
    fun registerDevice(
        @RequestBody @Valid request: RegisterDeviceRequest,
        @RequestHeader("X-User-Id") userId: String,
        servletRequest: HttpServletRequest
    ): ApiResponse<DeviceRegistrationResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        val response = deviceRegistrationService.registerDevice(workspaceId, userId, request)

        // Update activity with IP
        val ip = servletRequest.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()
            ?: servletRequest.remoteAddr
        deviceRegistrationService.updateActivity(workspaceId, request.deviceId, ip)

        return ApiResponse.success(response)
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh device token")
    fun refreshDeviceToken(
        @RequestBody @Valid request: RefreshDeviceTokenRequest
    ): ApiResponse<DeviceRegistrationResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        return ApiResponse.success(
            deviceRegistrationService.refreshDeviceToken(
                workspaceId,
                request.deviceId,
                request.appVersion
            )
        )
    }

    @GetMapping("")
    @Operation(summary = "Get all registered devices for workspace")
    fun getDevices(): ApiResponse<List<DeviceRegistrationResponse>> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        return ApiResponse.success(deviceRegistrationService.getDevices(workspaceId))
    }

    @GetMapping("/{deviceId}")
    @Operation(summary = "Get device by ID")
    fun getDevice(@PathVariable deviceId: String): ApiResponse<DeviceRegistrationResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        return ApiResponse.success(deviceRegistrationService.getDevice(workspaceId, deviceId))
    }

    @GetMapping("/{deviceId}/access-mode")
    @Operation(summary = "Get device access mode (for offline enforcement)")
    fun getAccessMode(@PathVariable deviceId: String): ApiResponse<Map<String, SubscriptionAccessMode>> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        val accessMode = deviceRegistrationService.getAccessMode(workspaceId, deviceId)
        return ApiResponse.success(mapOf("accessMode" to accessMode))
    }

    @DeleteMapping("/{deviceUid}")
    @Operation(summary = "Deactivate a device")
    fun deactivateDevice(
        @PathVariable deviceUid: String,
        @RequestParam(required = false) reason: String?
    ): ApiResponse<Map<String, Boolean>> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        deviceRegistrationService.deactivateDevice(workspaceId, deviceUid, reason)
        return ApiResponse.success(mapOf("deactivated" to true))
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all devices for a user (across workspaces)")
    fun getUserDevices(@PathVariable userId: String): ApiResponse<List<DeviceRegistrationResponse>> {
        return ApiResponse.success(deviceRegistrationService.getUserDevices(userId))
    }

    @PostMapping("/{deviceId}/push-token")
    @Operation(summary = "Update push notification token")
    fun updatePushToken(
        @PathVariable deviceId: String,
        @RequestParam pushToken: String?,
        @RequestParam pushTokenType: String?
    ): ApiResponse<Map<String, Boolean>> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        deviceRegistrationService.updatePushToken(workspaceId, deviceId, pushToken, pushTokenType)
        return ApiResponse.success(mapOf("updated" to true))
    }
}

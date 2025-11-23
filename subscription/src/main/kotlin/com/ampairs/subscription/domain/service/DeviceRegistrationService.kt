package com.ampairs.subscription.domain.service

import com.ampairs.subscription.domain.dto.*
import com.ampairs.subscription.domain.model.*
import com.ampairs.subscription.domain.repository.*
import com.ampairs.subscription.exception.SubscriptionException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Service
@Transactional
class DeviceRegistrationService(
    private val deviceRegistrationRepository: DeviceRegistrationRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val subscriptionService: SubscriptionService
) {
    private val logger = LoggerFactory.getLogger(DeviceRegistrationService::class.java)

    /**
     * Register a new device
     */
    fun registerDevice(
        workspaceId: String,
        userId: String,
        request: RegisterDeviceRequest
    ): DeviceRegistrationResponse {
        // Check device limit
        val limitCheck = subscriptionService.checkLimit(
            workspaceId,
            "DEVICE",
            deviceRegistrationRepository.countActiveByWorkspaceId(workspaceId).toInt()
        )

        if (!limitCheck.allowed && !limitCheck.isUnlimited) {
            throw SubscriptionException.LimitExceeded(
                "devices",
                limitCheck.current,
                limitCheck.limit
            )
        }

        // Check if device already registered
        val existing = deviceRegistrationRepository.findByWorkspaceIdAndDeviceId(workspaceId, request.deviceId)
        if (existing != null) {
            // Reactivate if inactive, otherwise refresh token
            if (!existing.isActive) {
                existing.isActive = true
                existing.deactivatedAt = null
                existing.deactivationReason = null
            }
            existing.refreshToken()
            existing.deviceName = request.deviceName ?: existing.deviceName
            existing.osVersion = request.osVersion ?: existing.osVersion
            existing.appVersion = request.appVersion ?: existing.appVersion
            existing.pushToken = request.pushToken ?: existing.pushToken
            existing.pushTokenType = request.pushTokenType ?: existing.pushTokenType
            existing.lastActivityAt = Instant.now()

            logger.info("Reactivated device {} for workspace {}", request.deviceId, workspaceId)
            return deviceRegistrationRepository.save(existing).asDeviceRegistrationResponse()
        }

        // Create new registration
        val device = DeviceRegistration().apply {
            this.workspaceId = workspaceId
            this.userId = userId
            this.deviceId = request.deviceId
            this.deviceName = request.deviceName
            this.platform = request.platform
            this.deviceModel = request.deviceModel
            this.osVersion = request.osVersion
            this.appVersion = request.appVersion
            this.pushToken = request.pushToken
            this.pushTokenType = request.pushTokenType
            this.tokenExpiresAt = Instant.now().plus(Duration.ofDays(DeviceRegistration.TOKEN_VALIDITY_DAYS))
            this.lastSyncAt = Instant.now()
            this.lastActivityAt = Instant.now()
            this.isActive = true
        }

        logger.info(
            "Registered new device {} ({}) for workspace {}, user {}",
            request.deviceId, request.platform, workspaceId, userId
        )
        return deviceRegistrationRepository.save(device).asDeviceRegistrationResponse()
    }

    /**
     * Refresh device token
     */
    fun refreshDeviceToken(
        workspaceId: String,
        deviceId: String,
        appVersion: String?
    ): DeviceRegistrationResponse {
        val device = deviceRegistrationRepository.findByWorkspaceIdAndDeviceId(workspaceId, deviceId)
            ?: throw SubscriptionException.DeviceNotFound(deviceId)

        if (!device.isActive) {
            throw SubscriptionException.DeviceDeactivated(deviceId)
        }

        device.refreshToken()
        if (appVersion != null) {
            device.appVersion = appVersion
        }
        device.lastActivityAt = Instant.now()

        logger.debug("Refreshed token for device {} in workspace {}", deviceId, workspaceId)
        return deviceRegistrationRepository.save(device).asDeviceRegistrationResponse()
    }

    /**
     * Get device by ID
     */
    fun getDevice(workspaceId: String, deviceId: String): DeviceRegistrationResponse {
        val device = deviceRegistrationRepository.findByWorkspaceIdAndDeviceId(workspaceId, deviceId)
            ?: throw SubscriptionException.DeviceNotFound(deviceId)
        return device.asDeviceRegistrationResponse()
    }

    /**
     * Get all devices for a workspace
     */
    fun getDevices(workspaceId: String): List<DeviceRegistrationResponse> {
        return deviceRegistrationRepository.findActiveByWorkspaceId(workspaceId)
            .asDeviceRegistrationResponses()
    }

    /**
     * Get all devices for a user
     */
    fun getUserDevices(userId: String): List<DeviceRegistrationResponse> {
        return deviceRegistrationRepository.findActiveByUserId(userId)
            .asDeviceRegistrationResponses()
    }

    /**
     * Deactivate a device
     */
    fun deactivateDevice(workspaceId: String, deviceUid: String, reason: String?): Boolean {
        val device = deviceRegistrationRepository.findByUid(deviceUid)
            ?: throw SubscriptionException.DeviceNotFound(deviceUid)

        if (device.workspaceId != workspaceId) {
            throw SubscriptionException.DeviceNotFound(deviceUid)
        }

        device.deactivate(reason ?: "User requested deactivation")
        deviceRegistrationRepository.save(device)

        logger.info("Deactivated device {} in workspace {}: {}", deviceUid, workspaceId, reason)
        return true
    }

    /**
     * Deactivate all devices for a workspace
     */
    fun deactivateAllDevices(workspaceId: String, reason: String) {
        deviceRegistrationRepository.deactivateAllByWorkspaceId(
            workspaceId,
            Instant.now(),
            reason
        )
        logger.info("Deactivated all devices for workspace {}: {}", workspaceId, reason)
    }

    /**
     * Update device activity
     */
    fun updateActivity(workspaceId: String, deviceId: String, ip: String?) {
        val device = deviceRegistrationRepository.findByWorkspaceIdAndDeviceId(workspaceId, deviceId)
        if (device != null && device.isActive) {
            deviceRegistrationRepository.updateLastActivity(device.uid, Instant.now(), ip)
        }
    }

    /**
     * Sync subscription state for a device
     */
    fun syncSubscription(
        workspaceId: String,
        userId: String,
        request: SyncSubscriptionRequest
    ): SyncSubscriptionResponse {
        // Refresh device token
        val device = deviceRegistrationRepository.findByWorkspaceIdAndDeviceId(workspaceId, request.deviceId)
            ?: throw SubscriptionException.DeviceNotFound(request.deviceId)

        if (!device.isActive) {
            throw SubscriptionException.DeviceDeactivated(request.deviceId)
        }

        device.refreshToken()
        device.lastActivityAt = Instant.now()
        val savedDevice = deviceRegistrationRepository.save(device)

        // Get subscription
        val subscription = subscriptionService.getSubscription(workspaceId)

        // Get usage
        val usage = try {
            subscriptionService.getUsage(workspaceId)
        } catch (e: Exception) {
            null
        }

        return SyncSubscriptionResponse(
            subscription = subscription,
            device = savedDevice.asDeviceRegistrationResponse(),
            usage = usage,
            serverTime = Instant.now()
        )
    }

    /**
     * Check device access mode
     */
    fun getAccessMode(workspaceId: String, deviceId: String): SubscriptionAccessMode {
        val device = deviceRegistrationRepository.findByWorkspaceIdAndDeviceId(workspaceId, deviceId)
            ?: return SubscriptionAccessMode.LOCKED

        return device.getAccessMode()
    }

    /**
     * Update push token
     */
    fun updatePushToken(
        workspaceId: String,
        deviceId: String,
        pushToken: String?,
        pushTokenType: String?
    ) {
        val device = deviceRegistrationRepository.findByWorkspaceIdAndDeviceId(workspaceId, deviceId)
            ?: throw SubscriptionException.DeviceNotFound(deviceId)

        deviceRegistrationRepository.updatePushToken(
            device.uid,
            pushToken,
            pushTokenType,
            Instant.now()
        )
    }
}

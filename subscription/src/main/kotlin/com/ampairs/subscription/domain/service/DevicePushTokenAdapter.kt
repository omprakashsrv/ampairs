package com.ampairs.subscription.domain.service

import com.ampairs.notification.port.DevicePushTokenPort
import com.ampairs.notification.port.PushTarget
import com.ampairs.subscription.domain.model.DeviceRegistration
import com.ampairs.subscription.domain.repository.DeviceRegistrationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Subscription-side implementation of the notification module's [DevicePushTokenPort].
 *
 * Resolves active device FCM tokens for push fan-out and prunes tokens FCM rejects.
 */
@Component
class DevicePushTokenAdapter(
    private val deviceRegistrationRepository: DeviceRegistrationRepository,
) : DevicePushTokenPort {

    private val logger = LoggerFactory.getLogger(DevicePushTokenAdapter::class.java)

    override fun tokensForWorkspace(workspaceId: String): List<PushTarget> =
        deviceRegistrationRepository.findActiveByWorkspaceId(workspaceId).toTargets()

    override fun tokensForUser(userId: String): List<PushTarget> =
        deviceRegistrationRepository.findActiveByUserId(userId).toTargets()

    @Transactional
    override fun invalidateToken(token: String) {
        val devices = deviceRegistrationRepository.findByPushToken(token)
        if (devices.isEmpty()) return
        devices.forEach { device ->
            deviceRegistrationRepository.updatePushToken(device.uid, null, null, Instant.now())
        }
        logger.info("Pruned {} dead push token(s)", devices.size)
    }

    private fun List<DeviceRegistration>.toTargets(): List<PushTarget> = mapNotNull { device ->
        device.pushToken
            ?.takeIf { it.isNotBlank() }
            ?.let { PushTarget(it, device.pushTokenType, device.deviceId, device.userId) }
    }
}

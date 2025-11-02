package com.ampairs.event.domain.dto

import com.ampairs.event.domain.DeviceStatus

/**
 * WebSocket message for user status changes
 */
data class UserStatusEvent(
    val userId: String,
    val deviceId: String,
    val status: DeviceStatus,
    val deviceName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

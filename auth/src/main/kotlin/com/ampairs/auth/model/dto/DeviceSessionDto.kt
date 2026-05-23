package com.ampairs.auth.model.dto

import java.time.Instant

data class DeviceSessionDto(
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
    val platform: String,
    val browser: String,
    val os: String,
    val ipAddress: String,
    val location: String?,
    val lastActivity: Instant,
    val loginTime: Instant,
    val isCurrentDevice: Boolean,
)
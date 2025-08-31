package com.ampairs.auth.model.dto

import java.time.LocalDateTime

data class DeviceSessionDto(
    val deviceId: String,

    val deviceName: String,

    val deviceType: String,

    val platform: String,
    val browser: String,
    val os: String,

    val ipAddress: String,

    val location: String?,

    val lastActivity: LocalDateTime,

    val loginTime: LocalDateTime,

    val isCurrentDevice: Boolean,
)
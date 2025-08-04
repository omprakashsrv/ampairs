package com.ampairs.auth.model.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class DeviceSessionDto(
    @JsonProperty("device_id")
    val deviceId: String,

    @JsonProperty("device_name")
    val deviceName: String,

    @JsonProperty("device_type")
    val deviceType: String,

    val platform: String,
    val browser: String,
    val os: String,

    @JsonProperty("ip_address")
    val ipAddress: String,

    val location: String?,

    @JsonProperty("last_activity")
    val lastActivity: LocalDateTime,

    @JsonProperty("login_time")
    val loginTime: LocalDateTime,

    @JsonProperty("is_current_device")
    val isCurrentDevice: Boolean,
)
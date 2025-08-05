package com.ampairs.auth.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class AuthComplete(
    @SerialName("session_id")
    val sessionId: String,
    @SerialName("otp")
    val otp: String,
    @SerialName("auth_mode")
    val authMode: String = "SMS",
    @SerialName("recaptcha_token")
    val recaptchaToken: String? = null,
    @SerialName("device_id")
    val deviceId: String? = null,
    @SerialName("device_name")
    val deviceName: String? = null,
    @SerialName("device_type")
    val deviceType: String? = null,
    @SerialName("platform")
    val platform: String? = null,
    @SerialName("browser")
    val browser: String? = null,
    @SerialName("os")
    val os: String? = null
)
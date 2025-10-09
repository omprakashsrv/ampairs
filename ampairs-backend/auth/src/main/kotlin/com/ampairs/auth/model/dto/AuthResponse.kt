package com.ampairs.auth.model.dto

import com.ampairs.user.model.dto.UserResponse
import java.time.LocalDateTime

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long, // seconds
    val refreshExpiresIn: Long, // seconds
    val user: UserResponse,
    val issuedAt: LocalDateTime = LocalDateTime.now(),
)

data class OtpInitResponse(
    val sessionId: String,
    val expiresIn: Long, // seconds
    val message: String = "OTP sent successfully",
    val canResendAfter: Long = 30, // seconds
)
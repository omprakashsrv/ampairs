package com.ampairs.auth.controller

import com.ampairs.auth.service.AccountLockoutService
import com.ampairs.core.domain.dto.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

/**
 * Controller for account lockout management
 * Provides endpoints for checking lockout status and administrative unlock functions
 */
@RestController
@RequestMapping("/auth/v1/lockout")
class AccountLockoutController(
    private val accountLockoutService: AccountLockoutService,
) {

    /**
     * Check lockout status for a specific phone number (Admin only)
     * GET /auth/v1/lockout/status?phone=1234567890&country_code=91
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    fun getLockoutStatus(
        @RequestParam phone: String,
        @RequestParam("country_code") countryCode: Int,
    ): ApiResponse<Map<String, Any>> {
        val status = accountLockoutService.getLockoutStatus(phone, countryCode)

        return ApiResponse.success(
            mapOf<String, Any>(
                "phone" to maskPhoneNumber(phone),
                "country_code" to countryCode,
                "is_locked" to status.isLocked,
                "locked_until" to (status.lockedUntil ?: ""),
                "failed_attempts" to status.failedAttempts,
                "remaining_minutes" to if (status.lockedUntil != null) {
                    java.time.Duration.between(java.time.LocalDateTime.now(), status.lockedUntil).toMinutes()
                        .coerceAtLeast(0)
                } else 0
            )
        )
    }

    /**
     * Manually unlock an account (Admin only)
     * POST /auth/v1/lockout/unlock
     */
    @PostMapping("/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    fun unlockAccount(
        @RequestBody request: UnlockAccountRequest,
        httpRequest: HttpServletRequest,
    ): ApiResponse<Map<String, Any>> {
        val currentUser = SecurityContextHolder.getContext().authentication?.name ?: "system"

        val unlocked = accountLockoutService.unlockAccount(
            request.phone,
            request.countryCode,
            currentUser,
            httpRequest
        )

        return if (unlocked) {
            ApiResponse.success(
                mapOf(
                    "message" to "Account unlocked successfully",
                    "phone" to maskPhoneNumber(request.phone),
                    "unlocked_by" to currentUser
                )
            )
        } else {
            ApiResponse.success(
                mapOf(
                    "message" to "Account was not locked or already unlocked",
                    "phone" to maskPhoneNumber(request.phone)
                )
            )
        }
    }

    /**
     * Get lockout statistics for monitoring (Admin only)
     * GET /auth/v1/lockout/statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    fun getLockoutStatistics(): ApiResponse<Map<String, Any>> {
        val statistics = accountLockoutService.getLockoutStatistics()
        return ApiResponse.success(statistics)
    }

    /**
     * Check current user's lockout status (for user self-service)
     * This endpoint is more limited - only shows if locked and remaining time
     */
    @GetMapping("/self-status")
    fun getSelfLockoutStatus(
        @RequestParam phone: String,
        @RequestParam("country_code") countryCode: Int,
    ): ApiResponse<Map<String, Any>> {
        val status = accountLockoutService.getLockoutStatus(phone, countryCode)

        return ApiResponse.success(
            mapOf(
                "is_locked" to status.isLocked,
                "remaining_minutes" to if (status.lockedUntil != null) {
                    java.time.Duration.between(java.time.LocalDateTime.now(), status.lockedUntil).toMinutes()
                        .coerceAtLeast(0)
                } else 0,
                "message" to if (status.isLocked) {
                    "Account is temporarily locked due to multiple failed authentication attempts."
                } else {
                    "Account is not locked."
                }
            )
        )
    }

    /**
     * Mask phone number for privacy
     */
    private fun maskPhoneNumber(phone: String): String {
        return if (phone.length > 4) {
            "${phone.take(2)}${"*".repeat(phone.length - 4)}${phone.takeLast(2)}"
        } else {
            "*".repeat(phone.length)
        }
    }

    /**
     * Request DTO for unlocking account
     */
    data class UnlockAccountRequest(
        val phone: String,
        val countryCode: Int,
        val reason: String? = null,
    )
}
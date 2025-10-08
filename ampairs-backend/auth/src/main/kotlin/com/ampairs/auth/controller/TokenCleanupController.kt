package com.ampairs.auth.controller

import com.ampairs.auth.service.TokenCleanupService
import com.ampairs.core.domain.dto.ApiResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for token cleanup operations.
 * Only available when token cleanup is enabled.
 */
@RestController
@RequestMapping("/api/admin/token-cleanup")
@ConditionalOnProperty(
    name = ["application.security.token-cleanup.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class TokenCleanupController(
    private val tokenCleanupService: TokenCleanupService,
) {

    /**
     * Get the count of expired/revoked tokens that would be cleaned up
     */
    @GetMapping("/status")
    fun getCleanupStatus(): ApiResponse<Map<String, Any>> {
        val expiredCount = tokenCleanupService.getExpiredTokenCount()
        return ApiResponse.success(
            mapOf(
                "expiredTokenCount" to expiredCount,
                "message" to "Number of expired/revoked tokens pending cleanup"
            )
        )
    }

    /**
     * Manually trigger token cleanup
     */
    @PostMapping("/run")
    fun runCleanup(): ApiResponse<Map<String, Any>> {
        val deletedCount = tokenCleanupService.performManualCleanup()
        return ApiResponse.success(
            mapOf(
                "deletedCount" to deletedCount,
                "message" to "Token cleanup completed successfully"
            )
        )
    }
}
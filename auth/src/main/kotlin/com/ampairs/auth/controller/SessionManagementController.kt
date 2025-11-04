package com.ampairs.auth.controller

import com.ampairs.auth.service.SessionManagementService
import com.ampairs.core.domain.dto.ApiResponse
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for session management endpoints
 * Provides session statistics and administrative controls
 */
@RestController
@RequestMapping("/core/v1/sessions")
class SessionManagementController(
    private val sessionManagementService: SessionManagementService,
) {

    /**
     * Get session statistics for monitoring and administrative purposes
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    fun getSessionStatistics(): ApiResponse<Map<String, Any>> {
        val statistics = sessionManagementService.getSessionStatistics()
        return ApiResponse.success(statistics)
    }

    /**
     * Manually trigger expired session cleanup
     */
    @GetMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    fun triggerSessionCleanup(): ApiResponse<String> {
        sessionManagementService.cleanupExpiredSessions()
        return ApiResponse.success("Session cleanup completed")
    }
}
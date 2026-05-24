package com.ampairs.core.auth.controller

import com.ampairs.core.auth.domain.*
import com.ampairs.core.auth.service.ApiKeyService
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.security.AuthenticationHelper
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

/**
 * Controller for API key management (Admin only).
 *
 * Provides endpoints for creating, listing, and revoking API keys.
 */
@RestController
@RequestMapping("/api/v1/admin/api-keys")
@PreAuthorize("hasRole('SUPER_ADMIN')")
class ApiKeyController(
    private val apiKeyService: ApiKeyService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Create a new API key.
     *
     * WARNING: The plain API key is only returned once in the response!
     *
     * @param request Creation request
     * @return Creation response with plain API key
     */
    @PostMapping
    fun createApiKey(
        @RequestBody @Valid request: CreateApiKeyRequest
    ): ApiResponse<ApiKeyCreationResponse> {
        val userId = AuthenticationHelper.getCurrentUserId(SecurityContextHolder.getContext().authentication) ?: "admin"
        logger.info("Creating API key: ${request.name} by user: $userId")
        return ApiResponse.success(apiKeyService.createApiKey(request, createdByUserId = userId))
    }

    /**
     * List all API keys.
     *
     * @return List of API keys (without plain keys)
     */
    @GetMapping
    fun listApiKeys(): ApiResponse<List<ApiKeyResponse>> {
        val keys = apiKeyService.listApiKeys()
        return ApiResponse.success(keys.asApiKeyResponses())
    }

    /**
     * Get API key details by UID.
     *
     * @param uid Key UID
     * @return API key details
     */
    @GetMapping("/{uid}")
    fun getApiKey(@PathVariable uid: String): ApiResponse<ApiKeyResponse> {
        val key = apiKeyService.getApiKey(uid)
        return ApiResponse.success(key.asApiKeyResponse())
    }

    /**
     * Revoke an API key.
     *
     * @param uid Key UID
     * @param reason Revocation reason
     * @return Success message
     */
    @PatchMapping("/{uid}/revoke")
    fun revokeApiKey(
        @PathVariable uid: String,
        @RequestParam reason: String
    ): ApiResponse<Map<String, String>> {
        val userId = AuthenticationHelper.getCurrentUserId(SecurityContextHolder.getContext().authentication) ?: "admin"
        apiKeyService.revokeApiKey(uid, reason, revokedBy = userId)
        return ApiResponse.success(mapOf(
            "message" to "API key revoked successfully",
            "uid" to uid
        ))
    }

    /**
     * Delete an API key permanently.
     *
     * @param uid Key UID
     * @return Success message
     */
    @DeleteMapping("/{uid}")
    fun deleteApiKey(@PathVariable uid: String): ApiResponse<Map<String, String>> {
        apiKeyService.deleteApiKey(uid)
        return ApiResponse.success(mapOf(
            "message" to "API key deleted successfully",
            "uid" to uid
        ))
    }
}

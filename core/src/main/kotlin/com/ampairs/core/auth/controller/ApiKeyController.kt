package com.ampairs.core.auth.controller

import com.ampairs.core.auth.domain.*
import com.ampairs.core.auth.service.ApiKeyService
import com.ampairs.core.domain.dto.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * Controller for API key management (Admin only).
 *
 * Provides endpoints for creating, listing, and revoking API keys.
 */
@RestController
@RequestMapping("/api/v1/admin/api-keys")
@PreAuthorize("hasRole('ADMIN')")
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
        @RequestBody request: CreateApiKeyRequest
        // TODO: Get current user from SecurityContext
        // @AuthenticationPrincipal user: UserPrincipal
    ): ApiResponse<ApiKeyCreationResponse> {
        logger.info("Creating API key: ${request.name}")

        val result = apiKeyService.createApiKey(request, createdByUserId = "admin")

        return ApiResponse.success(result)
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
        // TODO: Get current user from SecurityContext
    ): ApiResponse<Map<String, String>> {
        apiKeyService.revokeApiKey(uid, reason, revokedBy = "admin")
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

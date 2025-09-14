package com.ampairs.workspace.filter

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.multitenancy.CurrentTenantIdentifierResolver
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.security.AuthenticationHelper
import com.ampairs.core.service.UserService
import com.ampairs.workspace.service.WorkspaceMemberService
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class SessionUserFilter @Autowired constructor(
    private val memberService: WorkspaceMemberService,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    companion object {
        private val log = LoggerFactory.getLogger(SessionUserFilter::class.java)
        private const val WORKSPACE_HEADER = "X-Workspace-ID"
    }


    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val requestPath = request.requestURI ?: request.servletPath

        // Skip filter for auth, user, public workspace endpoints, and health checks
        if (shouldSkipFilter(requestPath)) {
            chain.doFilter(request, response)
            return
        }

        try {
            // Get authenticated user
            val auth: Authentication? = SecurityContextHolder.getContext().authentication
            if (auth == null) {
                log.warn("No authenticated user found for workspace access")
                sendAccessDeniedResponse(response, request.requestURI, "Authentication required")
                return
            }

            val userId = AuthenticationHelper.getCurrentUserId(auth)
            if (userId == null) {
                log.warn("Could not extract user ID from authentication")
                sendAccessDeniedResponse(response, request.requestURI, "Invalid authentication")
                return
            }

            val workspaceId = request.getHeader(WORKSPACE_HEADER)

            if (workspaceId.isNullOrBlank()) {
                log.warn("Missing workspace header for user: $userId")
                sendAccessDeniedResponse(response, request.requestURI, "Workspace header (X-Workspace-ID) is required")
                return
            }

            // Validate workspace membership
            if (!memberService.isWorkspaceMember(workspaceId, userId)) {
                log.warn("User $userId attempted to access workspace $workspaceId without membership")
                sendAccessDeniedResponse(response, request.requestURI, "You don't have access to this workspace")
                return
            }

            // Set tenant context using Spring-native approach
            setTenantInSecurityContext(workspaceId)
            TenantContextHolder.setCurrentTenant(workspaceId)
            log.debug("Set tenant context to workspace: $workspaceId for user: $userId")

            // Continue with request
            chain.doFilter(request, response)

        } catch (exception: Exception) {
            log.error("Error in workspace access control filter", exception)
            sendAccessDeniedResponse(response, request.requestURI, "Internal server error")
        } finally {
            // Clean up tenant context after request
            TenantContextHolder.clearTenantContext()
        }
    }

    private fun shouldSkipFilter(requestPath: String): Boolean {
        return requestPath.contains("/auth/v1") ||
                requestPath.contains("/user/v1") ||
                requestPath.contains("/workspace/v1/check-slug") ||
                requestPath.contains("/workspace/v1/search") ||
                requestPath.contains("/workspace/v1/invitations/") && requestPath.contains("/accept") ||
                requestPath.contains("/workspace/v1/invitations/my-pending") || // User-scoped endpoint (deprecated, use /user/v1/invitations/pending)
                isWorkspaceListEndpoint(requestPath) ||
                isWorkspaceDetailEndpoint(requestPath) ||
                requestPath.contains("/actuator/health") ||
                requestPath.contains("/actuator/info") ||
                requestPath.contains("/swagger") ||
                requestPath.contains("/api-docs")
    }

    private fun isWorkspaceListEndpoint(requestPath: String): Boolean {
        // Match exact path /workspace/v1 or /workspace/v1/ for GET requests (getUserWorkspaces)
        // and POST requests (createWorkspace)
        return requestPath.matches(Regex("^/workspace/v1/?$"))
    }

    private fun isWorkspaceDetailEndpoint(requestPath: String): Boolean {
        // Match workspace detail endpoints like /workspace/v1/{workspaceId}
        // These endpoints are used to fetch workspace details before setting workspace context
        return requestPath.matches(Regex("^/workspace/v1/[A-Z0-9]+$"))
    }

    private fun sendAccessDeniedResponse(
        response: HttpServletResponse,
        requestPath: String,
        message: String,
    ) {
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.status = HttpServletResponse.SC_FORBIDDEN

        val errorResponse = ApiResponse.error<Any>(
            code = "ACCESS_DENIED",
            message = "Workspace access denied",
            details = message,
            path = requestPath
        )

        objectMapper.writeValue(response.outputStream, errorResponse)
    }

    /**
     * Set tenant information in Spring Security context
     * This integrates with Hibernate's CurrentTenantIdentifierResolver
     */
    private fun setTenantInSecurityContext(workspaceId: String) {
        try {
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication != null) {
                // Create custom details with tenant information
                val details = mutableMapOf<String, Any>()
                details[CurrentTenantIdentifierResolver.TENANT_ATTRIBUTE] = workspaceId

                // Create new authentication token with tenant details
                val authWithDetails = when (authentication) {
                    is org.springframework.security.authentication.UsernamePasswordAuthenticationToken -> {
                        val newAuth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            authentication.principal,
                            authentication.credentials,
                            authentication.authorities
                        )
                        newAuth.details = details
                        newAuth
                    }

                    else -> {
                        // For other authentication types, try to set details directly
                        authentication.also {
                            // This may not work for all authentication types, but TenantContextHolder fallback will handle it
                            log.debug("Cannot set details on authentication type: ${it::class.simpleName}")
                        }
                    }
                }

                SecurityContextHolder.getContext().authentication = authWithDetails
                log.debug("Set tenant in SecurityContext: {}", workspaceId)
            }
        } catch (e: Exception) {
            log.debug("Could not set tenant in SecurityContext (fallback to TenantContextHolder will handle this)", e)
        }
    }
}
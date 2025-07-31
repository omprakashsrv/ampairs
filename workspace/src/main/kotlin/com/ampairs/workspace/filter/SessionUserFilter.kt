package com.ampairs.workspace.filter

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.ErrorCodes
import com.ampairs.core.multitenancy.TenantContext
import com.ampairs.user.model.User
import com.ampairs.workspace.model.SessionUser
import com.ampairs.workspace.service.WorkspaceService
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class SessionUserFilter @Autowired constructor(
    val companyService: WorkspaceService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        if (request.servletPath.contains("/auth/v1")
            || request.servletPath.contains("/user/v1")
            || request.servletPath.contains("/company/v1")
            || request.servletPath.contains("/actuator/health")
        ) {
            chain.doFilter(request, response)
            return
        }
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User
        val companyId = request.getHeader("X-Workspace")
        if (!companyId.isNullOrEmpty()) {
            val userCompanies = companyService.getUserCompanies(user.seqId)
            if (userCompanies.isNotEmpty()) {
                val userWorkspace = userCompanies.find { it.seqId == companyId }
                if (userWorkspace != null) {
                    TenantContext.setCurrentTenant(userWorkspace.companyId)
                    val authToken = UsernamePasswordAuthenticationToken(
                        SessionUser(user, userWorkspace), null, user.authorities
                    )
                    authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authToken
                    chain.doFilter(request, response)
                    return
                }
            }
        }
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.status = HttpServletResponse.SC_FORBIDDEN
        val mapper = ObjectMapper()
        val errorResponse = ApiResponse.error<Any>(
            code = ErrorCodes.ACCESS_DENIED,
            message = "Workspace access denied",
            details = "You don't have permission to access this company or no company header provided",
            path = request.requestURI
        )
        mapper.writeValue(response.outputStream, errorResponse)
    }
}
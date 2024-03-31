package com.ampairs.company.filter

import com.ampairs.company.model.SessionUser
import com.ampairs.company.service.CompanyService
import com.ampairs.core.domain.dto.ErrorResponse
import com.ampairs.core.multitenancy.TenantContext
import com.ampairs.user.model.User
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
    val companyService: CompanyService,
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
        val companyId = request.getHeader("X-Company")
        if (!companyId.isNullOrEmpty()) {
            val company = companyService.getUserCompany(user.id, companyId)
            if (company != null) {
                TenantContext.setCurrentTenant(company.id)
                val authToken = UsernamePasswordAuthenticationToken(
                    SessionUser(user, company), null, user.authorities
                )
                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authToken
                chain.doFilter(request, response)
                return
            }
        }
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.status = HttpServletResponse.SC_FORBIDDEN
        val mapper = ObjectMapper()
        mapper.writeValue(response.outputStream, ErrorResponse(HttpServletResponse.SC_FORBIDDEN, "Unauthorized"))
    }
}
package com.ampairs.ecom.interceptor

import com.ampairs.auth.service.JwtService
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.ecom.domain.enums.StorefrontAccessMode
import com.ampairs.ecom.domain.model.Storefront
import com.ampairs.ecom.exception.StoreAccessDeniedException
import com.ampairs.ecom.exception.StoreUnauthenticatedException
import com.ampairs.ecom.service.StorefrontAccessService
import com.ampairs.ecom.service.StorefrontService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class StorefrontTenantInterceptor(
    private val storefrontService: StorefrontService,
    private val storefrontAccessService: StorefrontAccessService,
    private val jwtService: JwtService,
) : HandlerInterceptor {

    private val logger = LoggerFactory.getLogger(StorefrontTenantInterceptor::class.java)

    companion object {
        const val STOREFRONT_ATTR = "storefront"
        private val SLUG_REGEX = Regex("/store/([^/?]+)")
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val slug = SLUG_REGEX.find(request.requestURI)?.groupValues?.get(1) ?: return true
        val storefront = storefrontService.getPublishedStorefrontBySlug(slug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        request.setAttribute(STOREFRONT_ATTR, storefront)

        if (storefront.accessMode == StorefrontAccessMode.RESTRICTED) {
            enforceAccessControl(storefront, request)
        }

        return true
    }

    override fun afterCompletion(request: HttpServletRequest, response: HttpServletResponse, handler: Any, ex: Exception?) {
        TenantContextHolder.clearTenantContext()
    }

    private fun enforceAccessControl(storefront: Storefront, request: HttpServletRequest) {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication == null || !authentication.isAuthenticated || authentication.principal == "anonymousUser") {
            throw StoreUnauthenticatedException("Authentication required to access restricted store '${storefront.slug}'")
        }

        // Workspace members bypass the access list
        if (isWorkspaceMember(request, storefront.ownerId)) {
            return
        }

        val hasAccess = storefrontAccessService.checkAccess(storefront.uid, authentication, request)
        if (!hasAccess) {
            logger.warn(
                "Store access denied: storefrontId={}, slug={}, principal={}",
                storefront.uid, storefront.slug, authentication.name
            )
            throw StoreAccessDeniedException("Access denied to restricted store '${storefront.slug}'")
        }
    }

    private fun isWorkspaceMember(request: HttpServletRequest, workspaceOwnerId: String): Boolean {
        val authHeader = request.getHeader("Authorization") ?: return false
        if (!authHeader.startsWith("Bearer ")) return false
        return try {
            val token = authHeader.substring(7)
            val tenantId = jwtService.extractTenantId(token)
            tenantId == workspaceOwnerId
        } catch (e: Exception) {
            false
        }
    }
}

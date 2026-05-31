package com.ampairs.ecom.interceptor

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.ecom.service.StorefrontService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class StorefrontTenantInterceptor(
    private val storefrontService: StorefrontService,
) : HandlerInterceptor {

    companion object {
        const val STOREFRONT_ATTR = "storefront"
        private val SLUG_REGEX = Regex("/store/([^/?]+)")
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val slug = SLUG_REGEX.find(request.requestURI)?.groupValues?.get(1) ?: return true
        val storefront = storefrontService.getPublishedStorefrontBySlug(slug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        request.setAttribute(STOREFRONT_ATTR, storefront)
        return true
    }

    override fun afterCompletion(request: HttpServletRequest, response: HttpServletResponse, handler: Any, ex: Exception?) {
        TenantContextHolder.clearTenantContext()
    }
}

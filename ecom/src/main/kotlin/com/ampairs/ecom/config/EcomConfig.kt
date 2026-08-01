package com.ampairs.ecom.config

import com.ampairs.ecom.interceptor.StorefrontRateLimitInterceptor
import com.ampairs.ecom.interceptor.StorefrontTenantInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableScheduling
class EcomConfig(
    private val storefrontTenantInterceptor: StorefrontTenantInterceptor,
    private val storefrontRateLimitInterceptor: StorefrontRateLimitInterceptor,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        // Rate limit before resolving the tenant so abusive traffic is shed early.
        registry.addInterceptor(storefrontRateLimitInterceptor)
            .addPathPatterns("/v1/store/**")
            .order(5)

        registry.addInterceptor(storefrontTenantInterceptor)
            .addPathPatterns("/v1/store/**")
            .order(10)
    }
}

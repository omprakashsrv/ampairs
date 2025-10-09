package com.ampairs.auth.config

import com.ampairs.auth.interceptor.RateLimitingInterceptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Configuration for rate limiting interceptors
 */
@Configuration
class RateLimitingConfiguration @Autowired constructor(
    private val rateLimitingInterceptor: RateLimitingInterceptor,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        // Apply rate limiting to authentication and API endpoints
        registry.addInterceptor(rateLimitingInterceptor)
            .addPathPatterns(
                "/auth/v1/**",
                "/user/v1/**",
                "/api/**"
            )
            .excludePathPatterns(
                "/auth/v1/session/**" // Exclude session check from rate limiting (read-only)
            )
    }
}
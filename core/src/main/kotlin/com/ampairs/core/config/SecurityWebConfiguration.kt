package com.ampairs.core.config

import com.ampairs.core.interceptor.SecurityValidationInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.CommonsRequestLoggingFilter
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Web configuration for security validation
 */
@Configuration
class SecurityWebConfiguration(
    private val securityValidationInterceptor: SecurityValidationInterceptor,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        // Add security validation interceptor to all requests except static resources
        registry.addInterceptor(securityValidationInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/actuator/**",
                "/health/**",
                "/metrics/**",
                "/favicon.ico",
                "/static/**",
                "/css/**",
                "/js/**",
                "/images/**",
                "/webjars/**"
            )
            .order(1) // Execute early in the interceptor chain
    }

    @Bean
    fun requestLoggingFilter(): CommonsRequestLoggingFilter {
        val loggingFilter = CommonsRequestLoggingFilter()
        loggingFilter.isIncludeClientInfo = true
        loggingFilter.isIncludeQueryString = true
        loggingFilter.isIncludePayload = false // Don't log payload for security
        loggingFilter.isIncludeHeaders = false // Don't log headers for security
        loggingFilter.setMaxPayloadLength(0) // No payload logging
        return loggingFilter
    }
}
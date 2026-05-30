package com.ampairs.config

import com.ampairs.core.config.ApplicationProperties
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
class CorsConfig(
    private val applicationProperties: ApplicationProperties
) : WebMvcConfigurer {

    private fun buildCorsConfiguration(): CorsConfiguration {
        val corsProps = applicationProperties.security.cors
        return CorsConfiguration().apply {
            allowedOrigins = corsProps.allowedOrigins
            allowedMethods = corsProps.allowedMethods
            allowedHeaders = corsProps.allowedHeaders
            allowCredentials = corsProps.allowCredentials
            maxAge = corsProps.maxAge.toSeconds()
        }
    }

    /** Used by Spring Security's filter chain to handle OPTIONS preflight before DispatcherServlet. */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", buildCorsConfiguration())
        }
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        val config = buildCorsConfiguration()
        registry.addMapping("/**")
            .allowedOrigins(*config.allowedOrigins!!.toTypedArray())
            .allowedMethods(*config.allowedMethods!!.toTypedArray())
            .allowedHeaders(*config.allowedHeaders!!.toTypedArray())
            .allowCredentials(config.allowCredentials!!)
            .maxAge(config.maxAge!!)
    }
}

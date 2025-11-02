package com.ampairs.config

import com.ampairs.core.config.ApplicationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
class CorsConfig(
    private val applicationProperties: ApplicationProperties
) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        val corsProps = applicationProperties.security.cors

        registry.addMapping("/**") // Allow CORS for all endpoints
            .allowedOrigins(*corsProps.allowedOrigins.toTypedArray()) // Use configured origins from YAML
            .allowedMethods(*corsProps.allowedMethods.toTypedArray()) // Use configured methods from YAML
            .allowedHeaders(*corsProps.allowedHeaders.toTypedArray()) // Use configured headers from YAML
            .allowCredentials(corsProps.allowCredentials) // Use configured credentials setting from YAML
            .maxAge(corsProps.maxAge.toSeconds()) // Use configured max age from YAML
    }
}

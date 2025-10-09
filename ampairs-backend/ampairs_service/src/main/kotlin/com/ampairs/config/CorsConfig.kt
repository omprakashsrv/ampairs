package com.ampairs.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
class CorsConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**") // Allow CORS for all endpoints
            .allowedOrigins("http://localhost:4200", "https://yourfrontenddomain.com") // Add your allowed origins
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Specify allowed methods
            .allowedHeaders("*") // Allow all headers
            .allowCredentials(true) // Allow credentials like cookies or authentication
    }
}

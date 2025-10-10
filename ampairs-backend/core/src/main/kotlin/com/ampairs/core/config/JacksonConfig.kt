package com.ampairs.core.config

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.TimeZone

/**
 * Jackson configuration for timezone handling.
 *
 * Configures Jackson to:
 * 1. Serialize all timestamps in UTC timezone
 * 2. Use ISO-8601 format with 'Z' suffix (e.g., "2025-01-09T14:30:00Z")
 * 3. Disable timestamp serialization as numbers
 * 4. Register JavaTimeModule for java.time.* support (Instant, LocalDateTime, etc.)
 *
 * This ensures all API responses have consistent, unambiguous timestamps in UTC format.
 *
 * @see java.time.Instant
 * @see com.ampairs.core.domain.model.BaseDomain
 */
@Configuration
class JacksonConfig {

    /**
     * Customize Jackson ObjectMapper for UTC timezone handling.
     *
     * This customizer:
     * - Sets default timezone to UTC for all date/time serialization
     * - Registers JavaTimeModule to support java.time.Instant and other java.time types
     * - Disables writing dates as timestamps (numeric values)
     * - Uses ISO-8601 string format instead: "2025-01-09T14:30:00Z"
     *
     * Works together with global SNAKE_CASE property naming strategy from application.yml.
     */
    @Bean
    fun jackson2ObjectMapperBuilderCustomizer(): Jackson2ObjectMapperBuilderCustomizer {
        return Jackson2ObjectMapperBuilderCustomizer { builder ->
            builder
                // Set UTC as default timezone for all serialization/deserialization
                .timeZone(TimeZone.getTimeZone("UTC"))
                // Register module to support java.time.* types (Instant, LocalDateTime, etc.)
                .modules(JavaTimeModule())
                // Disable numeric timestamp format, use ISO-8601 strings instead
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
}

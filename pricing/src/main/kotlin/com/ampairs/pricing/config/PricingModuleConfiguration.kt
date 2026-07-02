package com.ampairs.pricing.config

import org.springframework.context.annotation.Configuration

/**
 * Marker config for the pricing bounded context. Spring Boot auto-discovers all
 * `@RestController` / `@Service` / `@Repository` / `@Entity` under `com.ampairs` — no explicit
 * component/entity/repository scan needed.
 */
@Configuration
class PricingModuleConfiguration

package com.ampairs.config

import com.ampairs.auth.service.JwtService
import com.ampairs.auth.service.RsaKeyManager
import com.ampairs.core.auth.filter.ApiKeyAuthenticationFilter
import com.ampairs.core.auth.provider.ApiKeyAuthenticationProvider
import com.ampairs.core.config.ApplicationProperties
import com.ampairs.core.exception.AuthEntryPointJwt
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ComponentScan(value = ["com.ampairs.core"])
class SecurityConfiguration @Autowired constructor(
    val jwtService: JwtService,
    val rsaKeyManager: RsaKeyManager,
    val applicationProperties: ApplicationProperties,
    val customJwtAuthenticationConverter: CustomJwtAuthenticationConverter,
    val unauthorizedHandler: AuthEntryPointJwt,
    val apiKeyAuthenticationProvider: ApiKeyAuthenticationProvider,
) {

    /**
     * AuthenticationManager for API key authentication.
     */
    @Bean
    fun apiKeyAuthenticationManager(): AuthenticationManager {
        return ProviderManager(apiKeyAuthenticationProvider)
    }

    /**
     * API key authentication filter bean.
     */
    @Bean
    fun apiKeyAuthenticationFilter(): ApiKeyAuthenticationFilter {
        return ApiKeyAuthenticationFilter(apiKeyAuthenticationManager())
    }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        return when (val algorithm = applicationProperties.security.jwt.algorithm) {
            "RS256" -> {
                // Use RSA public key for RS256
                val currentKeyPair = rsaKeyManager.getCurrentKeyPair()
                NimbusJwtDecoder.withPublicKey(currentKeyPair.publicKey).build()
            }

            "HS256" -> {
                // Legacy HS256 support
                val secretKey = SecretKeySpec(jwtService.getSignInKey(), "HmacSHA256")
                NimbusJwtDecoder.withSecretKey(secretKey).build()
            }

            else -> {
                throw IllegalArgumentException("Unsupported JWT algorithm: $algorithm")
            }
        }
    }

    @Bean
    @Throws(Exception::class)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { csrf -> csrf.disable() }
            .cors { /* Use default CORS configuration from CorsConfig */ }
            .exceptionHandling { exception ->
                exception.authenticationEntryPoint(unauthorizedHandler)
            }
            .sessionManagement { session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            // Add API key authentication filter FIRST (before OAuth2 processing)
            .addFilterBefore(
                apiKeyAuthenticationFilter(),
                UsernamePasswordAuthenticationFilter::class.java
            )
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers(
                        "/auth/v1/**",
                        "/actuator/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/v3/api-docs",
                        "/swagger-resources/**",
                        "/api/v1/app-updates/check",
                        "/api/v1/app-updates/download/**",
                        "/webhooks/**"  // Payment provider webhooks (Google Play, App Store, Razorpay, Stripe)
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.decoder(jwtDecoder())
                        .jwtAuthenticationConverter(customJwtAuthenticationConverter)
                }
                // Only use entry point if no other authentication exists
                oauth2.authenticationEntryPoint(unauthorizedHandler)
                oauth2.bearerTokenResolver { request ->
                    // Skip OAuth2 if already authenticated (e.g., by API key)
                    if (org.springframework.security.core.context.SecurityContextHolder.getContext().authentication != null
                        && org.springframework.security.core.context.SecurityContextHolder.getContext().authentication.isAuthenticated) {
                        return@bearerTokenResolver null
                    }
                    // Only resolve bearer tokens for authenticated endpoints
                    val requestURI = request.requestURI
                    if (requestURI.startsWith("/auth/v1/") ||
                        requestURI.startsWith("/actuator/") ||
                        requestURI.startsWith("/swagger-ui/") ||
                        requestURI == "/swagger-ui.html" ||
                        requestURI.startsWith("/v3/api-docs") ||
                        requestURI.startsWith("/swagger-resources/") ||
                        requestURI == "/api/v1/app-updates/check" ||
                        requestURI.startsWith("/api/v1/app-updates/download/") ||
                        requestURI.startsWith("/webhooks/")
                    ) {
                        null // Skip JWT processing for public endpoints
                    } else {
                        // Use default bearer token resolution for protected endpoints
                        val authorizationHeaderValue = request.getHeader("Authorization")
                        if (authorizationHeaderValue != null && authorizationHeaderValue.startsWith("Bearer ")) {
                            authorizationHeaderValue.substring(7)
                        } else {
                            null
                        }
                    }
                }
            }
            .logout { logout ->
                logout.disable() // Disable Spring Security logout - handled by AuthController
            }
        return http.build()
    }
}

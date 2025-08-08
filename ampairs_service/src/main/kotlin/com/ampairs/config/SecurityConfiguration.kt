package com.ampairs.config

import com.ampairs.auth.service.JwtService
import com.ampairs.auth.service.RsaKeyManager
import com.ampairs.core.config.ApplicationProperties
import com.ampairs.core.exception.AuthEntryPointJwt
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain
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
) {

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
            .cors { cors -> cors.disable() }
            .exceptionHandling { exception ->
                exception.authenticationEntryPoint(unauthorizedHandler)
            }
            .sessionManagement { session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers(
                        "/auth/v1/**",
                        "/actuator/**"
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.decoder(jwtDecoder())
                        .jwtAuthenticationConverter(customJwtAuthenticationConverter)
                }
                oauth2.authenticationEntryPoint(unauthorizedHandler)
                oauth2.bearerTokenResolver { request ->
                    // Only resolve bearer tokens for authenticated endpoints
                    val requestURI = request.requestURI
                    if (requestURI.startsWith("/auth/v1/") || requestURI.startsWith("/actuator/")) {
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

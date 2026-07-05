package com.ampairs.config

import com.ampairs.auth.service.JwtService
import com.ampairs.auth.service.RsaKeyManager
import com.ampairs.core.auth.filter.ApiKeyAuthenticationFilter
import com.ampairs.core.auth.provider.ApiKeyAuthenticationProvider
import com.ampairs.core.config.ApplicationProperties
import com.ampairs.core.exception.AuthEntryPointJwt
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest
import org.springframework.util.AntPathMatcher
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
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.cors.CorsConfigurationSource
import javax.crypto.spec.SecretKeySpec

private val PUBLIC_PATHS = arrayOf(
    "/auth/v1/**",
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/v3/api-docs/**",
    "/v3/api-docs",
    "/swagger-resources/**",
    "/core/v1/app-updates/check",
    "/core/v1/app-updates/download/**",
    "/subscription/v1/webhooks/**",
    "/v1/storefronts",     // Public cross-tenant storefront directory (multi-store app) — auth optional
    "/v1/storefronts/**",
    "/error",
    "/ws/**",    // WebSocket upgrade (direct path)
    "/api/ws/**" // WebSocket upgrade via DispatcherServlet (spring.mvc.servlet.path=/api)
)

private val antPathMatcher = AntPathMatcher()

private fun isPublicRequest(request: HttpServletRequest): Boolean {
    val contextPath = request.contextPath.orEmpty()
    val pathAfterContext = request.requestURI?.removePrefix(contextPath) ?: return false
    val servletPath = request.servletPath.orEmpty()
    // pathInfo holds the path within the servlet (e.g. "/auth/v1/verify/firebase") when the
    // DispatcherServlet is mapped to "/api/*" and servletPath is just "/api".
    val pathInfo = request.pathInfo.orEmpty()
    return PUBLIC_PATHS.any { pattern ->
        antPathMatcher.match(pattern, pathAfterContext) ||
                (servletPath.isNotEmpty() && antPathMatcher.match(pattern, servletPath)) ||
                (pathInfo.isNotEmpty() && antPathMatcher.match(pattern, pathInfo))
    }
}

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ComponentScan(value = ["com.ampairs.core"])
class SecurityConfiguration(
    val jwtService: JwtService,
    val rsaKeyManager: RsaKeyManager,
    val applicationProperties: ApplicationProperties,
    val customJwtAuthenticationConverter: CustomJwtAuthenticationConverter,
    val unauthorizedHandler: AuthEntryPointJwt,
    val apiKeyAuthenticationProvider: ApiKeyAuthenticationProvider,
    val corsConfigurationSource: CorsConfigurationSource,
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
            "RS256" -> RotatingRsaJwtDecoder(rsaKeyManager)
            "HS256" -> {
                val secretKey = SecretKeySpec(jwtService.getSignInKey(), "HmacSHA256")
                NimbusJwtDecoder.withSecretKey(secretKey).build()
            }
            else -> throw IllegalArgumentException("Unsupported JWT algorithm: $algorithm")
        }
    }

    @Bean
    @Throws(Exception::class)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { csrf -> csrf.disable() }
            .cors { cors -> cors.configurationSource(corsConfigurationSource) }
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
                    .requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
                    .requestMatchers(RequestMatcher { request -> isPublicRequest(request) }).permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.decoder(jwtDecoder())
                        .jwtAuthenticationConverter(customJwtAuthenticationConverter)
                }
                oauth2.authenticationEntryPoint(unauthorizedHandler)
                oauth2.bearerTokenResolver { request ->
                    if (org.springframework.security.core.context.SecurityContextHolder.getContext().authentication?.isAuthenticated == true) {
                        return@bearerTokenResolver null
                    }
                    val uri = request.requestURI
                    val info = request.pathInfo.orEmpty()
                    val isPublic = uri.startsWith("/actuator/") || uri == "/actuator" ||
                        PUBLIC_PATHS.any { pattern ->
                            val prefix = pattern.removeSuffix("**").removeSuffix("*")
                            uri.startsWith(prefix) || uri == pattern ||
                                (info.isNotEmpty() && (info.startsWith(prefix) || info == pattern))
                        }
                    if (isPublic) {
                        null
                    } else {
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

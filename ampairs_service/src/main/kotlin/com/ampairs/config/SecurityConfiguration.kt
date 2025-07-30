package com.ampairs.config

import com.ampairs.auth.repository.TokenRepository
import com.ampairs.auth.service.JwtService
import com.ampairs.core.exception.AuthEntryPointJwt
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.logout.LogoutHandler
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ComponentScan(value = ["com.ampairs.core"])
class SecurityConfiguration @Autowired constructor(
    val jwtService: JwtService,
    val userDetailsService: UserDetailsService,
    val tokenRepository: TokenRepository,
    val customJwtAuthenticationConverter: CustomJwtAuthenticationConverter,
    val logoutHandler: LogoutHandler,
    val logoutSuccessHandler: LogoutSuccessHandler,
    val unauthorizedHandler: AuthEntryPointJwt,
) {

    @Bean
    fun jwtDecoder(): JwtDecoder {
        val secretKey = SecretKeySpec(jwtService.getSignInKey(), "HmacSHA256")
        return NimbusJwtDecoder.withSecretKey(secretKey).build()
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
            }
            .logout { logout ->
                logout.addLogoutHandler(logoutHandler)
                    .logoutUrl("/auth/v1/logout")
                    .logoutSuccessHandler(logoutSuccessHandler)
            }
        return http.build()
    }
}

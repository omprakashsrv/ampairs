package com.ampairs.auth.config

import com.ampairs.auth.domain.service.AuthEntryPointJwt
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.logout.LogoutHandler
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfiguration @Autowired constructor(
    val jwtAuthFilter: JwtAuthenticationFilter,
    val authenticationProvider: AuthenticationProvider,
    val logoutHandler: LogoutHandler,
    val logoutSuccessHandler: LogoutSuccessHandler,
    val unauthorizedHandler: AuthEntryPointJwt
) {

    @Bean
    @Throws(Exception::class)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf({ csrf -> csrf.disable() })
            .exceptionHandling({ exception -> exception.authenticationEntryPoint(unauthorizedHandler) })
            .sessionManagement({ session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) })
            .authorizeHttpRequests({ requests ->
                requests.requestMatchers("/v1/auth/**").permitAll()
                    .requestMatchers("/v1/user/**").permitAll()
                    .anyRequest().authenticated()
            }).authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .logout { logout: LogoutConfigurer<HttpSecurity?> ->
                logout.addLogoutHandler(logoutHandler).logoutUrl("/v1/auth/logout")
                    .logoutSuccessHandler(logoutSuccessHandler)
            }
        return http.build()
    }
}

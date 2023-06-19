package com.ampairs.auth.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer
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
    val logoutSuccessHandler: LogoutSuccessHandler
) {

    @Bean
    @Throws(Exception::class)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf({ csrf -> csrf.disable() }).authorizeHttpRequests({ requests ->
                requests.requestMatchers("/auth/v1/**").permitAll()
            }).authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .logout { logout: LogoutConfigurer<HttpSecurity?> ->
                logout.addLogoutHandler(logoutHandler).logoutUrl("/auth/v1/logout")
                    .logoutSuccessHandler(logoutSuccessHandler)
            }
        return http.build()
    }
}

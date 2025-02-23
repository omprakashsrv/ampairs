package com.ampairs.auth.controller

import com.ampairs.auth.model.dto.*
import com.ampairs.auth.service.AuthService
import com.ampairs.core.domain.dto.GenericSuccessResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth/v1")
class AuthController @Autowired constructor(
    private val authService: AuthService,
) {

    @PostMapping("/init")
    fun init(@RequestBody @Valid authInitRequest: AuthInitRequest): AuthInitResponse {
        return authService.init(authInitRequest)
    }

    @GetMapping("/session/{sessionId}")
    fun session(@PathVariable sessionId: String): SessionResponse {
        return authService.checkSession(sessionId)
    }

    @PostMapping("/verify")
    fun complete(@RequestBody @Valid authenticationRequest: AuthenticationRequest): AuthenticationResponse {
        return authService.authenticate(authenticationRequest)
    }

    @PostMapping("/refresh_token")
    fun refreshToken(@RequestBody @Valid request: RefreshTokenRequest): AuthenticationResponse {
        return authService.refreshToken(request)
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): GenericSuccessResponse {
        return authService.logout(request)
    }
}
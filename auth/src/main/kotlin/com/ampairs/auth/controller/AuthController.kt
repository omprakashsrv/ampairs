package com.ampairs.auth.controller

import com.ampairs.auth.model.dto.AuthInitRequest
import com.ampairs.auth.model.dto.AuthenticationRequest
import com.ampairs.auth.model.dto.AuthenticationResponse
import com.ampairs.auth.model.dto.RefreshTokenRequest
import com.ampairs.auth.service.AuthService
import com.ampairs.core.domain.dto.GenericSuccessResponse
import com.ampairs.user.model.User
import com.ampairs.user.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth/v1")
class AuthController @Autowired constructor(
    private val userService: UserService,
    private val authService: AuthService,
) {

    @PostMapping("/init")
    fun init(@RequestBody @Valid authInitRequest: AuthInitRequest): GenericSuccessResponse {
        val user: User = userService.createUser(authInitRequest.toUser())
        return authService.init(user)
    }

    @PostMapping("/authenticate")
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
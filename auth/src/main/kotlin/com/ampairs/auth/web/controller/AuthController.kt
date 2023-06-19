package com.ampairs.auth.web.controller

import com.ampairs.auth.domain.model.User
import com.ampairs.auth.domain.service.AuthService
import com.ampairs.auth.domain.service.UserService
import com.ampairs.auth.web.contract.AuthInitRequest
import com.ampairs.auth.web.contract.AuthenticationRequest
import com.ampairs.auth.web.contract.AuthenticationResponse
import com.ampairs.auth.web.contract.GenericSuccessResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/auth")
class AuthController @Autowired constructor(
    private val userService: UserService,
    private val authService: AuthService
) {

    @PostMapping("/init")
    fun init(@RequestBody @Valid authInitRequest: AuthInitRequest): GenericSuccessResponse {
        val user: User = userService.createUser(authInitRequest.toUser());
        return authService.init(user)
    }

    @PostMapping("/authenticate")
    fun complete(@RequestBody @Valid authenticationRequest: AuthenticationRequest): AuthenticationResponse {
        return authService.authenticate(authenticationRequest)
    }

    @PostMapping("/refresh_token")
    fun refreshToken(request: HttpServletRequest): AuthenticationResponse {
        return authService.refreshToken(request)
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): GenericSuccessResponse {
        return authService.logout(request)
    }
}
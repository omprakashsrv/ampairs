package com.ampairs.auth.controller

import com.ampairs.core.domain.dto.*
import com.ampairs.core.domain.model.User
import com.ampairs.core.domain.service.AuthService
import com.ampairs.core.domain.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth/v1")
class AuthController @Autowired constructor(
    private val userService: UserService,
    private val authService: AuthService
) {

    @PostMapping("/init")
    fun init(@RequestBody @Valid authInitRequest: AuthInitRequest): GenericSuccessResponse {
//        var requestParam: RequestParam1 = null
        val user: User = userService.createUser(authInitRequest.toUser());
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
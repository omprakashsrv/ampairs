package com.ampairs.auth.web.controller

import com.ampairs.auth.domain.model.User
import com.ampairs.auth.domain.service.UserService
import com.ampairs.auth.web.contract.*
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/user")
class UserController @Autowired constructor(
    private val userService: UserService
) {

    @PostMapping("/update")
    fun updateUser(@RequestBody @Valid userUpdateRequest: UserUpdateRequest): UserResponse {
        val user: User = userService.updateUser(userUpdateRequest);
        return UserResponse(user)
    }

    @GetMapping("")
    fun getUser(): UserResponse {
        val sessionUser = userService.getSessionUser()
        return UserResponse(sessionUser)
    }

}
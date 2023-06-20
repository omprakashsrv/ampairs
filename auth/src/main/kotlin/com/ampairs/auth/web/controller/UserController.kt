package com.ampairs.auth.web.controller

import com.ampairs.auth.domain.dto.UserResponse
import com.ampairs.auth.domain.dto.UserUpdateRequest
import com.ampairs.auth.domain.model.User
import com.ampairs.auth.domain.service.UserService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/user/v1")
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
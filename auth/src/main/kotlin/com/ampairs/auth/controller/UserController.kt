package com.ampairs.auth.controller

import com.ampairs.core.domain.dto.UserResponse
import com.ampairs.core.domain.dto.UserUpdateRequest
import com.ampairs.core.domain.model.User
import com.ampairs.core.domain.service.UserService
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
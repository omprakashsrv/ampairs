package com.ampairs.user.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.user.model.User
import com.ampairs.user.model.dto.UserResponse
import com.ampairs.user.model.dto.UserUpdateRequest
import com.ampairs.user.model.dto.toUserResponse
import com.ampairs.user.service.UserService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/user/v1")
class UserController @Autowired constructor(
    private val userService: UserService,
) {

    @PostMapping("/update")
    fun updateUser(@RequestBody @Valid userUpdateRequest: UserUpdateRequest): ApiResponse<UserResponse> {
        val user: User = userService.updateUser(userUpdateRequest)
        return ApiResponse.success(user.toUserResponse())
    }

    @GetMapping("")
    fun getUser(): ApiResponse<UserResponse> {
        val sessionUser = userService.getSessionUser()
        return ApiResponse.success(sessionUser.toUserResponse())  // No need for additional DB call
    }

}
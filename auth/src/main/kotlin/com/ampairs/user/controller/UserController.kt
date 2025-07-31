package com.ampairs.user.controller

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
    fun updateUser(@RequestBody @Valid userUpdateRequest: UserUpdateRequest): UserResponse {
        val user: User = userService.updateUser(userUpdateRequest)
        return user.toUserResponse()
    }

    @GetMapping("")
    fun getUser(): UserResponse {
        val sessionUser = userService.getSessionUser()
        return userService.getUser(sessionUser.seqId).toUserResponse()
    }

}
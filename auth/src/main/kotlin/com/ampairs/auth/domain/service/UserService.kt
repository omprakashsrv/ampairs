package com.ampairs.auth.domain.service

import com.ampairs.auth.domain.model.User
import com.ampairs.auth.persistance.respository.UserRepository
import com.ampairs.auth.web.contract.UserUpdateRequest
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service


@Service
class UserService @Autowired constructor(val userRepository: UserRepository) {

    @Transactional
    fun createUser(user: User): User {
        var existingUser = userRepository.findByUserName(user.userName).orElse(null)
        if (existingUser == null) {
            existingUser = userRepository.save(user)
        }
        return existingUser
    }

    @Transactional
    fun updateUser(userUpdateRequest: UserUpdateRequest): User {
        val user = getSessionUser()
        user.firstName = userUpdateRequest.firstName
        user.lastName = userUpdateRequest.lastName
        return userRepository.save(user)
    }

    fun getSessionUser(): User {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        return auth.principal as User
    }

}
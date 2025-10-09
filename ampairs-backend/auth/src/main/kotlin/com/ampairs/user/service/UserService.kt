package com.ampairs.user.service

import com.ampairs.user.model.User
import com.ampairs.user.model.dto.UserUpdateRequest
import com.ampairs.user.repository.UserRepository
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service


@Service
class UserService @Autowired constructor(val userRepository: UserRepository) {

    @Transactional
    fun createUser(user: User): User {
        // Check if user already exists (more efficient than findBy)
        if (userRepository.existsByUserName(user.userName)) {
            throw IllegalArgumentException("User with username '${user.userName}' already exists")
        }
        return userRepository.save(user)
    }

    @Transactional
    fun updateUser(userUpdateRequest: UserUpdateRequest): User {
        val user = getSessionUser()
        user.firstName = userUpdateRequest.firstName
        user.lastName = userUpdateRequest.lastName
        return userRepository.save(user)
    }

    fun getSessionUser(): User {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("No authentication context found")

        return when (val principal = auth.principal) {
            is User -> principal
            else -> throw IllegalStateException("Authentication principal is not a User: ${principal::class.simpleName}")
        }
    }

    fun getUser(id: String): User {
        return userRepository.findByUid(id)
            .orElseThrow { IllegalArgumentException("User not found with id: $id") }
    }

    /**
     * Get multiple users by their IDs in a single query
     * @param ids List of user IDs to fetch
     * @return List of found users (may be fewer than requested if some IDs don't exist)
     */
    fun getUsers(ids: List<String>): List<User> {
        if (ids.isEmpty()) return emptyList()
        return userRepository.findByUidIn(ids)
    }

}
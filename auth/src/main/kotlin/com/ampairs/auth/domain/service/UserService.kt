package com.ampairs.auth.domain.service

import com.ampairs.auth.domain.model.User
import com.ampairs.auth.persistance.respository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserService @Autowired constructor(val userRepository: UserRepository) {
    fun createUser(user: User): User {
        var existingUser = userRepository.findByUserName(user.userName).orElse(null)
        if (existingUser == null) {
            existingUser = userRepository.save(user)
        }
        return existingUser
    }

}
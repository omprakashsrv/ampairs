package com.ampairs.user.repository

import com.ampairs.user.model.User
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : CrudRepository<User, Long> {
    fun findByUserName(userName: String): Optional<User>

    fun findByUid(uid: String): Optional<User>

    /**
     * Batch fetch users by their UIDs using IN clause
     */
    fun findByUidIn(uids: List<String>): List<User>

    // Additional useful methods
    fun existsByUserName(userName: String): Boolean

    fun findByPhone(phone: String): Optional<User>

    fun findByEmail(email: String): Optional<User>
}
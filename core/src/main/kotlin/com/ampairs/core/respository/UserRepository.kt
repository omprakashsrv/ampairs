package com.ampairs.core.respository

import com.ampairs.core.domain.model.User
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : CrudRepository<User, String> {
    fun findByUserName(userName: String): Optional<User>
}
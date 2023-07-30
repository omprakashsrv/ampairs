package com.ampairs.tally.repository

import com.ampairs.tally.model.domain.user.User
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : CrudRepository<User, Int> {
    fun findByUserName(userName: String): Optional<User>

    fun findById(id: String): Optional<User>

}
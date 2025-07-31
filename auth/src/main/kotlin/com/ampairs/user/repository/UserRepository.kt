package com.ampairs.user.repository

import com.ampairs.user.model.User
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : CrudRepository<User, Long> {
    fun findByUserName(userName: String): Optional<User>

    fun findBySeqId(seqId: String): Optional<User>
}
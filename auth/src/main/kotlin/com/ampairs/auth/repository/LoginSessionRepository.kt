package com.ampairs.auth.repository

import com.ampairs.auth.model.LoginSession
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface LoginSessionRepository : CrudRepository<LoginSession, Int> {

    fun findById(id: String): Optional<LoginSession>
}
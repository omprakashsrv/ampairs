package com.ampairs.auth.respository

import com.ampairs.auth.domain.model.Token
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TokenRepository : CrudRepository<Token, String> {

    @Query(value = """
            select t from Token t inner join User u 
            on t.userId = u.id 
            where u.id = :userId and (t.expired = false or t.revoked = false)
            """)
    fun findAllValidTokenByUser(userId: String): List<Token>

    fun findByToken(token: String): Optional<Token>
}
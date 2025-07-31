package com.ampairs.auth.repository

import com.ampairs.auth.model.Token
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TokenRepository : CrudRepository<Token, String> {

    @Query(value = """
            select t from token t inner join user u 
            on t.userId = u.seqId 
            where u.seqId = :userId and (t.expired = false or t.revoked = false)
            """)
    fun findAllValidTokenByUser(userId: String): List<Token>

    fun findByToken(token: String): Optional<Token>
}
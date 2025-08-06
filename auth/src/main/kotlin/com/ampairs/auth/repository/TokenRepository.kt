package com.ampairs.auth.repository

import com.ampairs.auth.model.Token
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface TokenRepository : CrudRepository<Token, String> {

    @Query(value = """
            select t from token t inner join user u 
            on t.userId = u.uid 
            where u.uid = :userId and (t.expired = false or t.revoked = false)
            """)
    fun findAllValidTokenByUser(userId: String): List<Token>

    fun findByToken(token: String): Optional<Token>

    @Query(
        value = """
            select t from token t 
            where t.expired = true or t.revoked = true 
            or t.createdAt < :beforeDate
            """
    )
    fun findExpiredOrRevokedTokens(beforeDate: LocalDateTime): List<Token>

    @Query(
        value = """
            select t from token t 
            where t.expired = true or t.revoked = true 
            or t.createdAt < :beforeDate
            order by t.createdAt asc
            """
    )
    fun findExpiredOrRevokedTokensPaged(beforeDate: LocalDateTime, pageable: Pageable): List<Token>

    @Query(
        value = """
            select count(t) from token t 
            where t.expired = true or t.revoked = true 
            or t.createdAt < :beforeDate
            """
    )
    fun countExpiredOrRevokedTokens(beforeDate: LocalDateTime): Long

    @Modifying
    @Query(
        value = """
            delete from token t 
            where t.expired = true or t.revoked = true 
            or t.createdAt < :beforeDate
            """
    )
    fun deleteExpiredOrRevokedTokens(beforeDate: LocalDateTime): Int
}
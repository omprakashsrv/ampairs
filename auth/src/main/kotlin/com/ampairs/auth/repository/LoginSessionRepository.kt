package com.ampairs.auth.repository

import com.ampairs.auth.model.LoginSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface LoginSessionRepository : JpaRepository<LoginSession, Long> {

    fun findByPhoneAndCountryCodeAndVerifiedFalse(phone: String, countryCode: Int): List<LoginSession>

    fun findByUidAndVerifiedFalseAndExpiredFalse(uid: String): LoginSession?

    fun findByUid(uid: String): LoginSession?

    @Modifying
    @Query(
        "UPDATE login_session ls SET ls.expired = true WHERE ls.expiresAt < ?1 AND ls.expired = false",
        nativeQuery = true
    )
    fun expireOldSessions(currentTime: LocalDateTime): Int

    @Query(
        "SELECT COUNT(*) FROM login_session WHERE phone = ?1 AND country_code = ?2 AND created_at > ?3",
        nativeQuery = true
    )
    fun countRecentAttempts(phone: String, countryCode: Int, since: String): Long
}
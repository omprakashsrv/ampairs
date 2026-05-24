package com.ampairs.auth.service

import com.ampairs.auth.model.Token
import com.ampairs.auth.repository.TokenRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.transaction.annotation.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.LogoutHandler
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.stereotype.Service

@Service
class LogoutService @Autowired constructor(
    val tokenRepository: TokenRepository,
    val tokenValidationCacheService: TokenValidationCacheService,
) : LogoutHandler, LogoutSuccessHandler {

    @Transactional
    override fun logout(
        request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication?,
    ) {
        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return
        }
        val jwt: String = authHeader.substring(7)

        val storedToken = tokenRepository.findByToken(jwt).orElse(null)
        if (storedToken != null) {
            storedToken.expired = true
            storedToken.revoked = true
            tokenRepository.save(storedToken)
        } else {
            val blacklistToken = Token()
            blacklistToken.token = jwt
            blacklistToken.expired = true
            blacklistToken.revoked = true
            blacklistToken.userId = authentication?.name ?: "unknown"
            blacklistToken.tokenType = com.ampairs.auth.model.enums.TokenType.BEARER
            tokenRepository.save(blacklistToken)
        }

        // Evict cached validity so the revoked token is rejected immediately
        tokenValidationCacheService.evictTokenValidity(jwt.take(32))

        SecurityContextHolder.clearContext()
    }

    override fun onLogoutSuccess(
        request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication?
    ) {
        SecurityContextHolder.clearContext()
    }
}

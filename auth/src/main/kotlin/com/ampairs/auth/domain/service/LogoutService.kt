package com.ampairs.auth.domain.service

import com.ampairs.auth.persistance.respository.TokenRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.LogoutHandler
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.stereotype.Service

@Service
class LogoutService @Autowired constructor(val tokenRepository: TokenRepository) : LogoutHandler, LogoutSuccessHandler {

    @Transactional
    override fun logout(
        request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication
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
            SecurityContextHolder.clearContext()
        }
    }

    override fun onLogoutSuccess(
        request: HttpServletRequest?, response: HttpServletResponse?, authentication: Authentication?
    ) {
        SecurityContextHolder.clearContext()
    }
}

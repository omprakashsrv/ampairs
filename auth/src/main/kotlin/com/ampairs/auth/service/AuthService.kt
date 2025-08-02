package com.ampairs.auth.service

import com.ampairs.auth.model.LoginSession
import com.ampairs.auth.model.Token
import com.ampairs.auth.model.dto.*
import com.ampairs.auth.model.enums.TokenType
import com.ampairs.auth.repository.LoginSessionRepository
import com.ampairs.auth.repository.TokenRepository
import com.ampairs.core.domain.dto.GenericSuccessResponse
import com.ampairs.core.utils.UniqueIdGenerators
import com.ampairs.notification.service.NotificationService
import com.ampairs.user.model.User
import com.ampairs.user.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service

val OTP_LENGTH: Int = 6
val SMS_VERIFICATION_VALIDITY = 10 * 60 * 1000

@Service
class AuthService @Autowired constructor(
    val userRepository: UserRepository,
    val tokenRepository: TokenRepository,
    val loginSessionRepository: LoginSessionRepository,
    val jwtService: JwtService,
    val notificationService: NotificationService,
) {
    @Transactional
    fun init(authInitRequest: AuthInitRequest): AuthInitResponse {
        val loginSession = LoginSession()
        loginSession.phone = authInitRequest.phone
        loginSession.countryCode = authInitRequest.countryCode
        loginSession.code = UniqueIdGenerators.NUMERIC.generate(OTP_LENGTH)
        loginSession.expiresAt = java.time.LocalDateTime.now().plusSeconds(SMS_VERIFICATION_VALIDITY.toLong())
        val savedSession = loginSessionRepository.save(loginSession)
        // Queue SMS for async sending
        notificationService.queueSms(
            ("+" + authInitRequest.countryCode.toString() + authInitRequest.phone),
            loginSession.code + " is one time password to verify the phone number."
        )
        val genericSuccessResponse = AuthInitResponse()
        genericSuccessResponse.message = "OTP sent successfully"
        genericSuccessResponse.sessionId = savedSession.seqId
        return genericSuccessResponse
    }

    @Transactional
    fun authenticate(request: AuthenticationRequest): AuthenticationResponse {
        val loginSession = loginSessionRepository.findBySeqIdAndVerifiedFalseAndExpiredFalse(request.sessionId)
            ?: throw Exception("Invalid session Id")

        if (loginSession.code == request.otp) {
            val user: User = userRepository.findByUserName(loginSession.userName())
                .orElseThrow()
            val jwtToken: String = jwtService.generateToken(user)
            val refreshToken: String = jwtService.generateRefreshToken(user)
            revokeAllUserTokens(user)
            saveUserToken(user, jwtToken)
            val authResponse = AuthenticationResponse()
            authResponse.accessToken = jwtToken
            authResponse.refreshToken = refreshToken
            return authResponse
        } else {
            throw Exception("Invalid otp")
        }
    }

    private fun saveUserToken(user: User, jwtToken: String) {
        val token = Token()
        token.userId = user.seqId
        token.token = jwtToken
        token.tokenType = TokenType.BEARER
        tokenRepository.save(token)
    }

    private fun revokeAllUserTokens(user: User) {
        val validUserTokens: List<Token> = tokenRepository.findAllValidTokenByUser(user.seqId)
        if (validUserTokens.isEmpty()) return
        validUserTokens.forEach { token ->
            token.expired = true
            token.revoked = true
        }
        tokenRepository.saveAll(validUserTokens)
    }

    @Throws(Exception::class)
    @Transactional
    fun refreshToken(
        refreshTokenRequest: RefreshTokenRequest,
    ): AuthenticationResponse {
        val refreshToken: String = refreshTokenRequest.refreshToken
            ?: throw IllegalArgumentException("Refresh token is required")
        val userName: String = jwtService.extractUsername(refreshToken)
        val user: User = this.userRepository.findByUserName(userName)
            .orElseThrow()
        if (jwtService.isTokenValid(refreshToken, user)) {
            val accessToken: String = jwtService.generateToken(user)
            revokeAllUserTokens(user)
            saveUserToken(user, accessToken)
            val authResponse = AuthenticationResponse()
            authResponse.accessToken = accessToken
            authResponse.refreshToken = refreshToken
            return authResponse
        }
        throw Exception("Access Token not valid")
    }

    @Throws(Exception::class)
    @Transactional
    fun logout(
        request: HttpServletRequest,
    ): GenericSuccessResponse {
        val authHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw Exception("Access token not found")
        }

        val refreshToken: String = authHeader.substring(7)
        val userName: String = jwtService.extractUsername(refreshToken)
        val user: User = this.userRepository.findByUserName(userName)
            .orElseThrow()
        revokeAllUserTokens(user)
        val genericSuccessResponse = GenericSuccessResponse()
        genericSuccessResponse.message = "User logged out successfully"
        return genericSuccessResponse
    }

    fun checkSession(sessionId: String): SessionResponse {
        val loginSession = loginSessionRepository.findBySeqId(sessionId)
        val genericSuccessResponse = GenericSuccessResponse()
        genericSuccessResponse.message = if (loginSession != null) "Session is valid" else "Session is not valid"
        genericSuccessResponse.success = (loginSession != null)
        return if (loginSession != null) {
            SessionResponse(loginSession.seqId, loginSession.countryCode, loginSession.phone, loginSession.isExpired())
        } else {
            SessionResponse("", 0, "", true)
        }
    }


}
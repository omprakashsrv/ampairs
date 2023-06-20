package com.ampairs.core.domain.service

import com.ampairs.core.domain.dto.AuthenticationRequest
import com.ampairs.core.domain.dto.AuthenticationResponse
import com.ampairs.core.domain.dto.GenericSuccessResponse
import com.ampairs.core.domain.model.Token
import com.ampairs.core.domain.model.User
import com.ampairs.core.respository.SmsVerificationRepository
import com.ampairs.core.respository.TokenRepository
import com.ampairs.core.respository.UserRepository
import com.ampairs.core.utils.UniqueIdGenerators
import jakarta.servlet.http.HttpServletRequest
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.sql.Timestamp

val OTP_LENGTH: Int = 6
val SMS_VERIFICATION_VALIDITY = 30 * 60 * 1000

@Service
class AuthService @Autowired constructor(
    val userRepository: UserRepository,
    val tokenRepository: TokenRepository,
    val smsVerificationRepository: SmsVerificationRepository,
    val jwtService: com.ampairs.core.domain.service.JwtService,
    val authenticationManager: AuthenticationManager,
    val encoder: PasswordEncoder
) {
    @Transactional
    fun init(user: User): com.ampairs.core.domain.dto.GenericSuccessResponse {
        val smsVerification = com.ampairs.core.domain.model.SmsVerification()
        smsVerification.countryCode = user.countryCode
        smsVerification.phone = user.phone
        smsVerification.userId = user.id
        smsVerification.code = UniqueIdGenerators.NUMERIC.generate(com.ampairs.core.domain.service.OTP_LENGTH)
        smsVerification.validTill =
            Timestamp(System.currentTimeMillis() + com.ampairs.core.domain.service.SMS_VERIFICATION_VALIDITY)
        smsVerificationRepository.save(smsVerification)
        user.userPassword = encoder.encode(smsVerification.code)
        userRepository.save(user)
        val genericSuccessResponse = com.ampairs.core.domain.dto.GenericSuccessResponse()
        genericSuccessResponse.message = "OTP sent successfully"
        return genericSuccessResponse
    }

    @Transactional
    fun authenticate(request: AuthenticationRequest): com.ampairs.core.domain.dto.AuthenticationResponse {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                request.userName,
                request.password
            )
        )
        val user: User = userRepository.findByUserName(request.userName!!)
            .orElseThrow()
        val jwtToken: String = jwtService.generateToken(user)
        val refreshToken: String = jwtService.generateRefreshToken(user)
        revokeAllUserTokens(user)
        saveUserToken(user, jwtToken)
        val authResponse = com.ampairs.core.domain.dto.AuthenticationResponse()
        authResponse.accessToken = jwtToken
        authResponse.refreshToken = refreshToken
        return authResponse
    }

    private fun saveUserToken(user: User, jwtToken: String) {
        val token = Token()
        token.userId = user.id
        token.token = jwtToken
        token.tokenType = com.ampairs.core.domain.enums.TokenType.BEARER
        tokenRepository.save(token)
    }

    private fun revokeAllUserTokens(user: User) {
        val validUserTokens: List<Token> = tokenRepository.findAllValidTokenByUser(user.id)
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
        request: HttpServletRequest,
    ): com.ampairs.core.domain.dto.AuthenticationResponse {
        val authHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw Exception("Access token not found")
        }

        val refreshToken: String = authHeader.substring(7)
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


}
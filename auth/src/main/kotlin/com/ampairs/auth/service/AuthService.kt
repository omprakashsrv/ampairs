package com.ampairs.auth.service

import com.ampairs.auth.config.OtpProperties
import com.ampairs.auth.model.DeviceSession
import com.ampairs.auth.model.LoginSession
import com.ampairs.auth.model.Token
import com.ampairs.auth.model.dto.*
import com.ampairs.auth.repository.DeviceSessionRepository
import com.ampairs.auth.repository.LoginSessionRepository
import com.ampairs.auth.repository.TokenRepository
import com.ampairs.auth.utils.DeviceInfoExtractor
import com.ampairs.core.domain.dto.GenericSuccessResponse
import com.ampairs.core.service.RateLimitingService
import com.ampairs.core.service.SecurityAuditService
import com.ampairs.core.utils.UniqueIdGenerators
import com.ampairs.notification.service.NotificationService
import com.ampairs.user.model.User
import com.ampairs.user.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.*

val OTP_LENGTH: Int = 6
val SMS_VERIFICATION_VALIDITY = 10 * 60 * 1000L

@Service
class AuthService @Autowired constructor(
    val userRepository: UserRepository,
    val tokenRepository: TokenRepository,
    val loginSessionRepository: LoginSessionRepository,
    val deviceSessionRepository: DeviceSessionRepository,
    val jwtService: JwtService,
    val notificationService: NotificationService,
    val deviceInfoExtractor: DeviceInfoExtractor,
    val otpProperties: OtpProperties,
    val rateLimitingService: RateLimitingService,
    val securityAuditService: SecurityAuditService,
    val sessionManagementService: SessionManagementService,
    val accountLockoutService: AccountLockoutService,
    val firebaseAuthService: FirebaseAuthService,
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    @Transactional
    fun init(authInitRequest: AuthInitRequest, httpRequest: HttpServletRequest): AuthInitResponse {
        // Check if account is locked before generating OTP
        if (accountLockoutService.isAccountLocked(authInitRequest.phone, authInitRequest.countryCode)) {
            val lockoutStatus =
                accountLockoutService.getLockoutStatus(authInitRequest.phone, authInitRequest.countryCode)
            val remainingMinutes = if (lockoutStatus.lockedUntil != null) {
                java.time.Duration.between(LocalDateTime.now(), lockoutStatus.lockedUntil).toMinutes()
            } else 0

            throw Exception("Account temporarily locked due to multiple failed attempts. Please try again in $remainingMinutes minutes.")
        }
        val loginSession = LoginSession()
        loginSession.phone = authInitRequest.phone
        loginSession.countryCode = authInitRequest.countryCode
        loginSession.code = UniqueIdGenerators.NUMERIC.generate(OTP_LENGTH)
        // Create expiry time - 10 minutes from now
        loginSession.expiresAt = Date(System.currentTimeMillis() + SMS_VERIFICATION_VALIDITY)
        val savedSession = loginSessionRepository.save(loginSession)

        // Log OTP generation event
        securityAuditService.logOtpEvent(
            phone = authInitRequest.phone,
            eventType = SecurityAuditService.OtpEventType.GENERATION,
            success = true,
            sessionId = savedSession.id!!.toString(),
            request = httpRequest,
            reason = null
        )
        
        // Queue SMS for async sending
        notificationService.queueSms(
            ("+" + authInitRequest.countryCode.toString() + authInitRequest.phone),
            loginSession.code + " is one time password to verify the phone number."
        )
        val genericSuccessResponse = AuthInitResponse()
        genericSuccessResponse.message = "OTP sent successfully"
        genericSuccessResponse.sessionId = savedSession.uid
        return genericSuccessResponse
    }

    @Transactional
    fun authenticate(request: AuthenticationRequest, httpRequest: HttpServletRequest): AuthenticationResponse {
        val clientIp = getClientIp(httpRequest)
        
        val loginSession = loginSessionRepository.findByUidAndVerifiedFalseAndExpiredFalse(request.sessionId)
            ?: run {
                // Record failure for invalid session ID
                rateLimitingService.recordAuthFailure(clientIp, "verify", "Invalid session ID")

                // Log authentication failure
                securityAuditService.logAuthenticationAttempt(
                    phone = "unknown",
                    countryCode = 0,
                    success = false,
                    reason = "Invalid session ID",
                    request = httpRequest,
                    sessionId = request.sessionId
                )
                
                throw Exception("Invalid session Id")
            }

        // Check if account is locked before OTP verification
        if (accountLockoutService.isAccountLocked(loginSession.phone, loginSession.countryCode)) {
            val lockoutStatus = accountLockoutService.getLockoutStatus(loginSession.phone, loginSession.countryCode)
            val remainingMinutes = if (lockoutStatus.lockedUntil != null) {
                java.time.Duration.between(LocalDateTime.now(), lockoutStatus.lockedUntil).toMinutes()
            } else 0

            throw Exception("Account temporarily locked due to multiple failed attempts. Please try again in $remainingMinutes minutes.")
        }

        if (loginSession.code == request.otp || isHardcodedOtpValid(request.otp)) {
            val user: User = userRepository.findByUserName(loginSession.userName())
                .orElseGet {
                    // Create new user if doesn't exist
                    val newUser = User()
                    newUser.userName = loginSession.userName()
                    newUser.countryCode = loginSession.countryCode
                    newUser.phone = loginSession.phone
                    newUser.active = true
                    userRepository.save(newUser)
                }

            // Extract device information
            val deviceInfo = deviceInfoExtractor.extractDeviceInfo(
                httpRequest,
                request.deviceId,
                request.deviceName
            )

            // Enforce concurrent session limits before creating new session
            sessionManagementService.enforceConcurrentSessionLimits(user.uid)

            // Create or update device session
            val deviceSession = createOrUpdateDeviceSession(user, deviceInfo)

            // Generate tokens with device information
            val jwtToken: String = jwtService.generateTokenWithDevice(user, deviceSession.deviceId)
            val refreshToken: String = jwtService.generateRefreshTokenWithDevice(user, deviceSession.deviceId)

            // Update device session with refresh token hash
            deviceSession.refreshTokenHash = hashToken(refreshToken)
            deviceSessionRepository.save(deviceSession)

            // Mark login session as verified to prevent reuse
            loginSession.verified = true
            loginSession.verifiedAt = Date()
            loginSessionRepository.save(loginSession)

            // Record successful authentication
            rateLimitingService.recordAuthSuccess(clientIp, "verify")
            accountLockoutService.recordAuthenticationSuccess(loginSession.phone, loginSession.countryCode)

            // Log successful authentication
            securityAuditService.logAuthenticationAttempt(
                phone = loginSession.phone,
                countryCode = loginSession.countryCode,
                success = true,
                reason = null,
                request = httpRequest,
                sessionId = request.sessionId,
                deviceInfo = mapOf<String, Any>(
                    "device_id" to deviceSession.deviceId,
                    "device_name" to (deviceSession.deviceName ?: "Unknown"),
                    "platform" to (deviceSession.platform ?: "Unknown"),
                    "browser" to (deviceSession.browser ?: "Unknown")
                )
            )

            // Log JWT token generation
            securityAuditService.logTokenEvent(
                eventType = SecurityAuditService.TokenEventType.GENERATED,
                userId = user.id!!.toString(),
                deviceId = deviceSession.deviceId,
                request = httpRequest
            )
            
            val authResponse = AuthenticationResponse()
            authResponse.accessToken = jwtToken
            authResponse.refreshToken = refreshToken
            authResponse.accessTokenExpiresAt = jwtService.extractExpirationAsLocalDateTime(jwtToken)
            authResponse.refreshTokenExpiresAt = jwtService.extractExpirationAsLocalDateTime(refreshToken)
            return authResponse
        } else {
            // Record failure for invalid OTP
            rateLimitingService.recordAuthFailure(clientIp, "verify", "Invalid OTP")
            accountLockoutService.recordAuthenticationFailure(
                loginSession.phone,
                loginSession.countryCode,
                "Invalid OTP",
                httpRequest
            )

            // Log OTP verification failure
            securityAuditService.logOtpEvent(
                phone = loginSession.phone,
                eventType = SecurityAuditService.OtpEventType.VERIFICATION,
                success = false,
                sessionId = request.sessionId,
                request = httpRequest,
                reason = "Invalid OTP"
            )

            // Log authentication failure
            securityAuditService.logAuthenticationAttempt(
                phone = loginSession.phone,
                countryCode = loginSession.countryCode,
                success = false,
                reason = "Invalid OTP",
                request = httpRequest,
                sessionId = request.sessionId
            )
            
            throw Exception("Invalid otp")
        }
    }

    private fun createOrUpdateDeviceSession(user: User, deviceInfo: DeviceInfoExtractor.DeviceInfo): DeviceSession {
        // Use list query to handle potential race condition duplicates
        val existingSessions = deviceSessionRepository.findAllByUserIdAndDeviceIdAndIsActiveTrue(
            user.uid, deviceInfo.deviceId
        )

        return if (existingSessions.isNotEmpty()) {
            // Handle potential duplicate sessions from race conditions
            val session = existingSessions.first()

            // Deactivate any duplicate sessions (keep only the first/most recent)
            if (existingSessions.size > 1) {
                logger.warn(
                    "Found {} duplicate active sessions for user {} device {}, cleaning up",
                    existingSessions.size, user.uid, deviceInfo.deviceId
                )
                existingSessions.drop(1).forEach { duplicate ->
                    duplicate.isActive = false
                    duplicate.expiredAt = LocalDateTime.now()
                    deviceSessionRepository.save(duplicate)
                }
            }

            // Update existing session
            session.deviceName = deviceInfo.deviceName
            session.deviceType = deviceInfo.deviceType
            session.platform = deviceInfo.platform
            session.browser = deviceInfo.browser
            session.os = deviceInfo.os
            session.ipAddress = deviceInfo.ipAddress
            session.userAgent = deviceInfo.userAgent
            session.location = deviceInfo.location
            session.updateActivity()
            session
        } else {
            // Create new device session
            val newSession = DeviceSession()
            newSession.userId = user.uid
            newSession.deviceId = deviceInfo.deviceId
            newSession.deviceName = deviceInfo.deviceName
            newSession.deviceType = deviceInfo.deviceType
            newSession.platform = deviceInfo.platform
            newSession.browser = deviceInfo.browser
            newSession.os = deviceInfo.os
            newSession.ipAddress = deviceInfo.ipAddress
            newSession.userAgent = deviceInfo.userAgent
            newSession.location = deviceInfo.location
            newSession.loginTime = LocalDateTime.now()
            newSession.lastActivity = LocalDateTime.now()
            newSession.isActive = true
            newSession
        }
    }

    /**
     * Check if the provided OTP is a valid hardcoded OTP for development/test environments
     * Only allows hardcoded OTP when specifically configured for development mode
     */
    private fun isHardcodedOtpValid(otp: String): Boolean {
        return otpProperties.allowHardcoded &&
                otpProperties.developmentMode &&
                otp == otpProperties.hardcodedOtp
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Extract client IP address from HTTP request
     */
    private fun getClientIp(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            return xForwardedFor.split(",")[0].trim()
        }

        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            return xRealIp
        }

        return request.remoteAddr ?: "unknown"
    }

    private fun saveUserToken(user: User, jwtToken: String) {
        // OPTIMIZATION: Don't store valid tokens in database
        // Only store tokens when they need to be revoked/blacklisted
        // This reduces database storage and improves authentication performance

        // No action needed - valid tokens are validated via JWT signature/expiry only
        // Tokens will be stored in database only when revoked (see revokeAllUserTokens)
    }

    private fun revokeAllUserTokens(user: User) {
        // OPTIMIZATION: Since we don't store all tokens anymore,
        // we need to blacklist any existing tokens for this user
        val validUserTokens: List<Token> = tokenRepository.findAllValidTokenByUser(user.uid)
        if (validUserTokens.isNotEmpty()) {
            validUserTokens.forEach { token ->
                token.expired = true
                token.revoked = true
            }
            tokenRepository.saveAll(validUserTokens)
        }

        // Note: Previously issued tokens that aren't in DB will naturally expire
        // based on their JWT expiration time. For immediate revocation of all
        // user tokens, we'd need to track user's last login time and compare
        // against token issue time, but current approach is sufficient for most use cases.
    }

    @Throws(Exception::class)
    @Transactional
    fun refreshToken(
        refreshTokenRequest: RefreshTokenRequest,
        httpRequest: HttpServletRequest,
    ): AuthenticationResponse {
        val refreshToken: String = refreshTokenRequest.refreshToken
            ?: throw IllegalArgumentException("Refresh token is required")

        val userName: String = jwtService.extractUsername(refreshToken)
        val deviceId: String = jwtService.extractDeviceId(refreshToken)
            ?: refreshTokenRequest.deviceId
            ?: throw IllegalArgumentException("Device ID is required")
        
        val user: User = this.userRepository.findByUserName(userName)
            .orElseThrow()

        // Verify refresh token is valid and belongs to the device
        if (jwtService.isTokenValid(refreshToken, user)) {
            // Use list query to handle potential race condition duplicates
            val sessions = deviceSessionRepository.findAllByUserIdAndDeviceIdAndIsActiveTrue(user.uid, deviceId)
            if (sessions.isEmpty()) {
                throw Exception("Device session not found or inactive")
            }
            val deviceSession = sessions.first()

            // Clean up any duplicate sessions
            if (sessions.size > 1) {
                logger.warn(
                    "Found {} duplicate active sessions for user {} device {} during token refresh, cleaning up",
                    sessions.size, user.uid, deviceId
                )
                sessions.drop(1).forEach { duplicate ->
                    duplicate.isActive = false
                    duplicate.expiredAt = LocalDateTime.now()
                    deviceSessionRepository.save(duplicate)
                }
            }

            // Validate session hasn't expired due to timeout rules
            if (!sessionManagementService.validateAndExpireIfNeeded(deviceSession)) {
                throw Exception("Device session has expired")
            }

            // Verify refresh token hash matches
            if (deviceSession.refreshTokenHash != hashToken(refreshToken)) {
                throw Exception("Invalid refresh token for device")
            }

            // CRITICAL: Update device session activity to prevent expiration
            // This ensures the session remains active when refresh tokens are used
            sessionManagementService.updateSessionActivity(deviceSession, httpRequest)

            // Generate new access token
            val accessToken: String = jwtService.generateTokenWithDevice(user, deviceId)

            // Log token refresh event
            securityAuditService.logTokenEvent(
                eventType = SecurityAuditService.TokenEventType.REFRESHED,
                userId = user.id!!.toString(),
                deviceId = deviceId,
                request = httpRequest
            )
            
            val authResponse = AuthenticationResponse()
            authResponse.accessToken = accessToken
            authResponse.refreshToken = refreshToken // Keep same refresh token
            authResponse.accessTokenExpiresAt = jwtService.extractExpirationAsLocalDateTime(accessToken)
            authResponse.refreshTokenExpiresAt = jwtService.extractExpirationAsLocalDateTime(refreshToken)
            return authResponse
        }
        throw Exception("Refresh token not valid")
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

        val accessToken: String = authHeader.substring(7)
        val userName: String = jwtService.extractUsername(accessToken)
        val deviceId: String = jwtService.extractDeviceId(accessToken)
            ?: throw Exception("Device ID not found in token")
            
        val user: User = this.userRepository.findByUserName(userName)
            .orElseThrow()

        // Deactivate specific device session only
        deviceSessionRepository.deactivateDeviceSession(user.uid, deviceId)

        val genericSuccessResponse = GenericSuccessResponse()
        genericSuccessResponse.message = "Device logged out successfully"
        return genericSuccessResponse
    }

    @Throws(Exception::class)
    @Transactional
    fun logoutAllDevices(
        request: HttpServletRequest,
    ): GenericSuccessResponse {
        val authHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw Exception("Access token not found")
        }

        val accessToken: String = authHeader.substring(7)
        val userName: String = jwtService.extractUsername(accessToken)
        val user: User = this.userRepository.findByUserName(userName)
            .orElseThrow()

        // Deactivate all device sessions for the user
        deviceSessionRepository.deactivateAllUserSessions(user.uid)
        revokeAllUserTokens(user)

        val genericSuccessResponse = GenericSuccessResponse()
        genericSuccessResponse.message = "Logged out from all devices successfully"
        return genericSuccessResponse
    }

    fun checkSession(sessionId: String): SessionResponse {
        val loginSession = loginSessionRepository.findByUid(sessionId)
        val genericSuccessResponse = GenericSuccessResponse()
        genericSuccessResponse.message = if (loginSession != null) "Session is valid" else "Session is not valid"
        genericSuccessResponse.success = (loginSession != null)
        return if (loginSession != null) {
            SessionResponse(loginSession.uid, loginSession.countryCode, loginSession.phone, loginSession.isExpired())
        } else {
            SessionResponse("", 0, "", true)
        }
    }

    /**
     * Get all active device sessions for a user
     */
    fun getUserDevices(request: HttpServletRequest): List<DeviceSessionDto> {
        val authHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw Exception("Access token not found")
        }

        val accessToken: String = authHeader.substring(7)
        val userName: String = jwtService.extractUsername(accessToken)
        val user: User = this.userRepository.findByUserName(userName)
            .orElseThrow()

        val deviceSessions = deviceSessionRepository.findByUserIdAndIsActiveTrueOrderByLastActivityDesc(user.uid)

        return deviceSessions.map { session ->
            DeviceSessionDto(
                deviceId = session.deviceId,
                deviceName = session.deviceName ?: "Unknown Device",
                deviceType = session.deviceType ?: "Unknown",
                platform = session.platform ?: "Unknown",
                browser = session.browser ?: "Unknown",
                os = session.os ?: "Unknown",
                ipAddress = session.ipAddress ?: "Unknown",
                location = session.location,
                lastActivity = session.lastActivity,
                loginTime = session.loginTime,
                isCurrentDevice = session.deviceId == jwtService.extractDeviceId(accessToken)
            )
        }
    }

    /**
     * Logout from a specific device
     */
    @Transactional
    fun logoutFromDevice(request: HttpServletRequest, targetDeviceId: String): GenericSuccessResponse {
        val authHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw Exception("Access token not found")
        }

        val accessToken: String = authHeader.substring(7)
        val userName: String = jwtService.extractUsername(accessToken)
        val user: User = this.userRepository.findByUserName(userName)
            .orElseThrow()

        // Deactivate specific device session
        val deactivatedCount = deviceSessionRepository.deactivateDeviceSession(user.uid, targetDeviceId)

        if (deactivatedCount == 0) {
            throw Exception("Device not found or already inactive")
        }

        val genericSuccessResponse = GenericSuccessResponse()
        genericSuccessResponse.message = "Device logged out successfully"
        return genericSuccessResponse
    }

    /**
     * Authenticate user with Firebase ID token
     * Verifies the Firebase token, creates or retrieves user, and generates JWT tokens
     */
    @Transactional
    fun authenticateWithFirebase(request: FirebaseAuthRequest, httpRequest: HttpServletRequest): AuthenticationResponse {
        val clientIp = getClientIp(httpRequest)

        // Verify Firebase ID token
        val firebaseToken = firebaseAuthService.verifyIdToken(request.firebaseIdToken)
            ?: run {
                // Record failure for invalid Firebase token
                rateLimitingService.recordAuthFailure(clientIp, "firebase_verify", "Invalid Firebase token")

                // Log authentication failure
                securityAuditService.logAuthenticationAttempt(
                    phone = request.phone,
                    countryCode = request.countryCode,
                    success = false,
                    reason = "Invalid Firebase token",
                    request = httpRequest,
                    sessionId = null
                )

                throw Exception("Invalid Firebase authentication token")
            }

        // Extract phone number from Firebase token
        val tokenPhoneNumber = firebaseAuthService.extractPhoneNumber(firebaseToken)
        val fullPhone = "+${request.countryCode}${request.phone}"

        // Verify the phone number matches
        if (tokenPhoneNumber != null && tokenPhoneNumber != fullPhone &&
            tokenPhoneNumber != "${request.countryCode}${request.phone}") {
            logger.debug("Phone mismatch - Token: $tokenPhoneNumber, Request: $fullPhone")

            rateLimitingService.recordAuthFailure(clientIp, "firebase_verify", "Phone number mismatch")
            securityAuditService.logAuthenticationAttempt(
                phone = request.phone,
                countryCode = request.countryCode,
                success = false,
                reason = "Phone number mismatch with Firebase token",
                request = httpRequest,
                sessionId = null
            )

            throw Exception("Phone number does not match Firebase authentication")
        }

        // Create username from phone number
        val userName = "${request.countryCode}${request.phone}"

        // Get or create user
        val user: User = userRepository.findByUserName(userName)
            .orElseGet {
                // Create new user if doesn't exist
                val newUser = User()
                newUser.userName = userName
                newUser.countryCode = request.countryCode
                newUser.phone = request.phone
                newUser.active = true
                // Optionally store Firebase UID for future reference
                newUser.firebaseUid = firebaseAuthService.getFirebaseUid(firebaseToken)
                userRepository.save(newUser)
            }

        // Update Firebase UID if not set
        if (user.firebaseUid.isNullOrBlank()) {
            user.firebaseUid = firebaseAuthService.getFirebaseUid(firebaseToken)
            userRepository.save(user)
        }

        // Extract device information
        val deviceInfo = deviceInfoExtractor.extractDeviceInfo(
            httpRequest,
            request.deviceId,
            request.deviceName
        )

        // Enforce concurrent session limits before creating new session
        sessionManagementService.enforceConcurrentSessionLimits(user.uid)

        // Create or update device session
        val deviceSession = createOrUpdateDeviceSession(user, deviceInfo)

        // Generate tokens with device information
        val jwtToken: String = jwtService.generateTokenWithDevice(user, deviceSession.deviceId)
        val refreshToken: String = jwtService.generateRefreshTokenWithDevice(user, deviceSession.deviceId)

        // Update device session with refresh token hash
        deviceSession.refreshTokenHash = hashToken(refreshToken)
        deviceSessionRepository.save(deviceSession)

        // Record successful authentication
        rateLimitingService.recordAuthSuccess(clientIp, "firebase_verify")

        // Log successful authentication
        securityAuditService.logAuthenticationAttempt(
            phone = user.phone,
            countryCode = user.countryCode,
            success = true,
            reason = "Firebase authentication",
            request = httpRequest,
            sessionId = null,
            deviceInfo = mapOf(
                "device_id" to deviceSession.deviceId,
                "device_name" to (deviceSession.deviceName ?: "Unknown"),
                "platform" to (deviceSession.platform ?: "Unknown"),
                "browser" to (deviceSession.browser ?: "Unknown"),
                "firebase_uid" to firebaseAuthService.getFirebaseUid(firebaseToken)
            )
        )

        // Log JWT token generation
        securityAuditService.logTokenEvent(
            eventType = SecurityAuditService.TokenEventType.GENERATED,
            userId = user.id!!.toString(),
            deviceId = deviceSession.deviceId,
            request = httpRequest
        )

        val authResponse = AuthenticationResponse()
        authResponse.accessToken = jwtToken
        authResponse.refreshToken = refreshToken
        authResponse.accessTokenExpiresAt = jwtService.extractExpirationAsLocalDateTime(jwtToken)
        authResponse.refreshTokenExpiresAt = jwtService.extractExpirationAsLocalDateTime(refreshToken)
        return authResponse
    }

}
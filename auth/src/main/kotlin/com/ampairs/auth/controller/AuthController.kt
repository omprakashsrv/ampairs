package com.ampairs.auth.controller

import com.ampairs.auth.exception.RecaptchaValidationException
import com.ampairs.auth.model.dto.*
import com.ampairs.auth.service.AuthService
import com.ampairs.auth.service.RecaptchaValidationService
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.GenericSuccessResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth/v1")
class AuthController @Autowired constructor(
    private val authService: AuthService,
    private val recaptchaValidationService: RecaptchaValidationService,
) {

    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    @PostMapping("/init")
    fun init(
        @RequestBody @Valid authInitRequest: AuthInitRequest,
        request: HttpServletRequest,
    ): ApiResponse<AuthInitResponse> {
        logger.info("Auth init request for phone: {}", authInitRequest.phoneNumber())
        logger.debug("Received reCAPTCHA token: '{}'", authInitRequest.recaptchaToken)
        
        // Validate reCAPTCHA if token is provided
        validateRecaptcha(authInitRequest.recaptchaToken, "login", getClientIp(request))

        return ApiResponse.success(authService.init(authInitRequest, request))
    }

    @GetMapping("/session/{sessionId}")
    fun session(@PathVariable sessionId: String): ApiResponse<SessionResponse> {
        return ApiResponse.success(authService.checkSession(sessionId))
    }

    @PostMapping("/verify")
    fun complete(
        @RequestBody @Valid authenticationRequest: AuthenticationRequest,
        request: HttpServletRequest,
    ): ApiResponse<AuthenticationResponse> {
        logger.info("Auth verification request for session: {}", authenticationRequest.sessionId)
        
        // Validate reCAPTCHA if token is provided
        validateRecaptcha(authenticationRequest.recaptchaToken, "verify_otp", getClientIp(request))

        return ApiResponse.success(authService.authenticate(authenticationRequest, request))
    }

    @PostMapping("/refresh_token")
    fun refreshToken(
        @RequestBody @Valid refreshTokenRequest: RefreshTokenRequest,
        request: HttpServletRequest,
    ): ApiResponse<AuthenticationResponse> {
        // No reCAPTCHA validation needed for refresh token (already authenticated)
        return ApiResponse.success(authService.refreshToken(refreshTokenRequest, request))
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): ApiResponse<GenericSuccessResponse> {
        // No reCAPTCHA validation needed for logout (already authenticated)
        return ApiResponse.success(authService.logout(request))
    }

    @PostMapping("/logout/all")
    fun logoutAllDevices(request: HttpServletRequest): ApiResponse<GenericSuccessResponse> {
        // Logout from all devices
        return ApiResponse.success(authService.logoutAllDevices(request))
    }

    @GetMapping("/devices")
    fun getUserDevices(request: HttpServletRequest): ApiResponse<List<DeviceSessionDto>> {
        // Get all active device sessions for the authenticated user
        return ApiResponse.success(authService.getUserDevices(request))
    }

    @PostMapping("/devices/{deviceId}/logout")
    fun logoutFromDevice(
        @PathVariable deviceId: String,
        request: HttpServletRequest,
    ): ApiResponse<GenericSuccessResponse> {
        // Logout from a specific device
        return ApiResponse.success(authService.logoutFromDevice(request, deviceId))
    }

    /**
     * Validate reCAPTCHA token if reCAPTCHA is enabled
     */
    private fun validateRecaptcha(recaptchaToken: String?, expectedAction: String, clientIp: String?) {
        logger.debug("validateRecaptcha called: token={}, action={}, ip={}", recaptchaToken, expectedAction, clientIp)
        
        if (!recaptchaValidationService.isEnabled()) {
            logger.debug("reCAPTCHA validation disabled, skipping validation")
            return
        }

        logger.debug("reCAPTCHA validation enabled, proceeding with validation")
        val validationResult = recaptchaValidationService.validateRecaptcha(
            token = recaptchaToken,
            expectedAction = expectedAction,
            remoteIp = clientIp
        )

        logger.debug(
            "reCAPTCHA validation result: success={}, score={}, message={}",
            validationResult.success, validationResult.score, validationResult.message
        )

        if (!validationResult.success) {
            logger.warn(
                "reCAPTCHA validation failed: action={}, score={}, errors={}, message={}",
                validationResult.action,
                validationResult.score,
                validationResult.errorCodes,
                validationResult.message
            )
            throw RecaptchaValidationException(validationResult)
        }

        logger.info(
            "reCAPTCHA validation successful: action={}, score={}",
            validationResult.action,
            validationResult.score
        )
    }

    /**
     * Extract client IP address from request
     */
    private fun getClientIp(request: HttpServletRequest): String? {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            return xForwardedFor.split(",")[0].trim()
        }

        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            return xRealIp
        }

        return request.remoteAddr
    }
}
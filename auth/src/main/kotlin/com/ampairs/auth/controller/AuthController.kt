package com.ampairs.auth.controller

import com.ampairs.auth.exception.RecaptchaValidationException
import com.ampairs.auth.model.dto.*
import com.ampairs.auth.service.AuthService
import com.ampairs.auth.service.RecaptchaValidationService
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
        request: HttpServletRequest
    ): AuthInitResponse {
        logger.info("Auth init request for phone: {}", authInitRequest.phoneNumber())
        
        // Validate reCAPTCHA if token is provided
        validateRecaptcha(authInitRequest.recaptchaToken, "login", getClientIp(request))
        
        return authService.init(authInitRequest)
    }

    @GetMapping("/session/{sessionId}")
    fun session(@PathVariable sessionId: String): SessionResponse {
        return authService.checkSession(sessionId)
    }

    @PostMapping("/verify")
    fun complete(
        @RequestBody @Valid authenticationRequest: AuthenticationRequest,
        request: HttpServletRequest
    ): AuthenticationResponse {
        logger.info("Auth verification request for session: {}", authenticationRequest.sessionId)
        
        // Validate reCAPTCHA if token is provided
        validateRecaptcha(authenticationRequest.recaptchaToken, "verify_otp", getClientIp(request))
        
        return authService.authenticate(authenticationRequest)
    }

    @PostMapping("/refresh_token")
    fun refreshToken(@RequestBody @Valid request: RefreshTokenRequest): AuthenticationResponse {
        // No reCAPTCHA validation needed for refresh token (already authenticated)
        return authService.refreshToken(request)
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): GenericSuccessResponse {
        // No reCAPTCHA validation needed for logout (already authenticated)
        return authService.logout(request)
    }

    /**
     * Validate reCAPTCHA token if reCAPTCHA is enabled
     */
    private fun validateRecaptcha(recaptchaToken: String?, expectedAction: String, clientIp: String?) {
        if (!recaptchaValidationService.isEnabled()) {
            logger.debug("reCAPTCHA validation disabled, skipping validation")
            return
        }

        val validationResult = recaptchaValidationService.validateRecaptcha(
            token = recaptchaToken,
            expectedAction = expectedAction,
            remoteIp = clientIp
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
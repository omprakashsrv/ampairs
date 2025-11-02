package com.ampairs.auth.controller

import com.ampairs.auth.exception.RecaptchaValidationException
import com.ampairs.auth.model.dto.*
import com.ampairs.auth.service.AuthService
import com.ampairs.auth.service.RecaptchaValidationService
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.GenericSuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@RestController
@RequestMapping("/auth/v1")
@Tag(name = "Authentication", description = "Authentication and session management APIs")
class AuthController @Autowired constructor(
    private val authService: AuthService,
    private val recaptchaValidationService: RecaptchaValidationService,
) {

    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    @PostMapping("/init")
    @Operation(
        summary = "Initialize authentication",
        description = "Start the authentication process by sending OTP to the provided phone number. This endpoint supports reCAPTCHA validation and handles device identification for multi-device authentication."
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "OTP sent successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "Success Response",
                        value = """{
                            "success": true,
                            "data": {
                                "message": "OTP sent successfully",
                                "session_id": "LSQ20250804100456522TBFOQ8U44LIBLX"
                            },
                            "timestamp": "2025-01-04T10:04:56Z"
                        }"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "Validation error or invalid request",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        name = "Validation Error",
                        value = """{
                            "success": false,
                            "error": {
                                "code": "VALIDATION_ERROR",
                                "message": "Invalid input data",
                                "details": "Request validation failed",
                                "validation_errors": {
                                    "phone": "Phone number is required",
                                    "country_code": "Invalid country code"
                                },
                                "module": "auth"
                            },
                            "timestamp": "2025-01-04T10:04:56Z",
                            "path": "/auth/v1/init"
                        }"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "429",
                description = "Rate limit exceeded",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        name = "Rate Limit Error",
                        value = """{
                            "success": false,
                            "error": {
                                "code": "RATE_LIMIT_EXCEEDED",
                                "message": "Rate limit exceeded. Try again later.",
                                "details": "You can only make 1 request per 20 seconds",
                                "module": "auth"
                            },
                            "timestamp": "2025-01-04T10:04:56Z"
                        }"""
                    )]
                )]
            )
        ]
    )
    fun init(
        @Parameter(
            description = "Authentication initialization request",
            required = true,
            content = [Content(
                examples = [ExampleObject(
                    name = "Mobile App Request",
                    value = """{
                        "phone": "9591781662",
                        "country_code": 91,
                        "device_id": "MOBILE_ABC123_DEVICE_FINGERPRINT",
                        "device_name": "John's iPhone 15",
                        "recaptcha_token": "your_recaptcha_token"
                    }"""
                ), ExampleObject(
                    name = "Web Browser Request",
                    value = """{
                        "phone": "9591781662",
                        "country_code": 91,
                        "recaptcha_token": "your_recaptcha_token"
                    }"""
                )]
            )]
        )
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
    @Operation(
        summary = "Check session status",
        description = "Check the status of an authentication session by session ID"
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "Session status retrieved successfully",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        name = "Session Active",
                        value = """{
                            "success": true,
                            "data": {
                                "session_id": "LSQ20250804100456522TBFOQ8U44LIBLX",
                                "status": "ACTIVE",
                                "expires_at": "2025-01-04T10:14:56Z"
                            },
                            "timestamp": "2025-01-04T10:04:56Z"
                        }"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "Session not found or expired"
            )
        ]
    )
    fun session(
        @Parameter(description = "Session ID to check", example = "LSQ20250804100456522TBFOQ8U44LIBLX")
        @PathVariable sessionId: String,
    ): ApiResponse<SessionResponse> {
        return ApiResponse.success(authService.checkSession(sessionId))
    }

    @PostMapping("/verify")
    @Operation(
        summary = "Verify OTP and complete authentication",
        description = "Verify the OTP sent to the phone number and complete the authentication process. Returns JWT access and refresh tokens."
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "Authentication successful",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        name = "Success Response",
                        value = """{
                            "success": true,
                            "data": {
                                "access_token": "eyJhbGciOiJIUzI1NiJ9...",
                                "refresh_token": "def456-ghi789-jkl012",
                                "access_token_expires_at": "2025-01-04T11:04:56Z",
                                "refresh_token_expires_at": "2025-01-11T10:04:56Z"
                            },
                            "timestamp": "2025-01-04T10:04:56Z"
                        }"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "Invalid OTP or request data"
            ),
            SwaggerApiResponse(
                responseCode = "422",
                description = "Invalid or expired session"
            )
        ]
    )
    fun complete(
        @Parameter(
            description = "OTP verification request",
            content = [Content(
                examples = [ExampleObject(
                    name = "Verify OTP Request",
                    value = """{
                        "session_id": "LSQ20250804100456522TBFOQ8U44LIBLX",
                        "otp": "123456",
                        "auth_mode": "SMS",
                        "device_id": "MOBILE_ABC123_DEVICE_FINGERPRINT",
                        "device_name": "John's iPhone 15",
                        "recaptcha_token": "your_recaptcha_token"
                    }"""
                )]
            )]
        )
        @RequestBody @Valid authenticationRequest: AuthenticationRequest,
        request: HttpServletRequest,
    ): ApiResponse<AuthenticationResponse> {
        logger.info("Auth verification request for session: {}", authenticationRequest.sessionId)
        
        // Validate reCAPTCHA if token is provided
        validateRecaptcha(authenticationRequest.recaptchaToken, "verify_otp", getClientIp(request))

        return ApiResponse.success(authService.authenticate(authenticationRequest, request))
    }

    @PostMapping("/verify/firebase")
    @Operation(
        summary = "Verify Firebase authentication and get tokens",
        description = "Verify Firebase ID token and return JWT access and refresh tokens. This endpoint is used for Firebase-based phone authentication."
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "Firebase authentication successful",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        name = "Success Response",
                        value = """{
                            "success": true,
                            "data": {
                                "access_token": "eyJhbGciOiJIUzI1NiJ9...",
                                "refresh_token": "def456-ghi789-jkl012",
                                "access_token_expires_at": "2025-01-04T11:04:56Z",
                                "refresh_token_expires_at": "2025-01-11T10:04:56Z"
                            },
                            "timestamp": "2025-01-04T10:04:56Z"
                        }"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "Invalid Firebase token or request data"
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "Firebase authentication failed"
            )
        ]
    )
    fun verifyFirebase(
        @Parameter(
            description = "Firebase authentication request",
            content = [Content(
                examples = [ExampleObject(
                    name = "Firebase Verify Request",
                    value = """{
                        "firebase_id_token": "eyJhbGciOiJSUzI1NiIsImtpZCI6...",
                        "phone": "9591781662",
                        "country_code": 91,
                        "device_id": "MOBILE_ABC123_DEVICE_FINGERPRINT",
                        "device_name": "John's iPhone 15",
                        "recaptcha_token": "your_recaptcha_token"
                    }"""
                )]
            )]
        )
        @RequestBody @Valid firebaseAuthRequest: FirebaseAuthRequest,
        request: HttpServletRequest,
    ): ApiResponse<AuthenticationResponse> {
        logger.info("Firebase auth verification request for phone: {}", firebaseAuthRequest.phone)

        // Validate reCAPTCHA if token is provided
//        validateRecaptcha(firebaseAuthRequest.recaptchaToken, "firebase_verify", getClientIp(request))

        return ApiResponse.success(authService.authenticateWithFirebase(firebaseAuthRequest, request))
    }

    @PostMapping("/refresh_token")
    @Operation(
        summary = "Refresh access token",
        description = "Use refresh token to obtain a new access token. This endpoint is device-specific."
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "Token refreshed successfully",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        name = "Success Response",
                        value = """{
                            "success": true,
                            "data": {
                                "access_token": "eyJhbGciOiJIUzI1NiJ9...",
                                "refresh_token": "new-refresh-token-456",
                                "access_token_expires_at": "2025-01-04T11:04:56Z",
                                "refresh_token_expires_at": "2025-01-11T10:04:56Z"
                            },
                            "timestamp": "2025-01-04T10:04:56Z"
                        }"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "Invalid or expired refresh token"
            )
        ]
    )
    fun refreshToken(
        @Parameter(
            description = "Refresh token request",
            content = [Content(
                examples = [ExampleObject(
                    name = "Refresh Token Request",
                    value = """{
                        "refresh_token": "your_refresh_token",
                        "device_id": "MOBILE_ABC123_DEVICE_FINGERPRINT"
                    }"""
                )]
            )]
        )
        @RequestBody @Valid refreshTokenRequest: RefreshTokenRequest,
        request: HttpServletRequest,
    ): ApiResponse<AuthenticationResponse> {
        // No reCAPTCHA validation needed for refresh token (already authenticated)
        return ApiResponse.success(authService.refreshToken(refreshTokenRequest, request))
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Logout from current device",
        description = "Logout from the current device session. Other device sessions remain active."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "Logout successful",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        name = "Success Response",
                        value = """{
                            "success": true,
                            "data": {
                                "message": "Logout successful"
                            },
                            "timestamp": "2025-01-04T10:04:56Z"
                        }"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "Authentication required"
            )
        ]
    )
    fun logout(request: HttpServletRequest): ApiResponse<GenericSuccessResponse> {
        // No reCAPTCHA validation needed for logout (already authenticated)
        return ApiResponse.success(authService.logout(request))
    }

    @PostMapping("/logout/all")
    @Operation(
        summary = "Logout from all devices",
        description = "Logout from all device sessions associated with the authenticated user."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "Logout from all devices successful",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        name = "Success Response",
                        value = """{
                            "success": true,
                            "data": {
                                "message": "Logged out from all devices successfully"
                            },
                            "timestamp": "2025-01-04T10:04:56Z"
                        }"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "Authentication required"
            )
        ]
    )
    fun logoutAllDevices(request: HttpServletRequest): ApiResponse<GenericSuccessResponse> {
        // Logout from all devices
        return ApiResponse.success(authService.logoutAllDevices(request))
    }

    @GetMapping("/devices")
    @Operation(
        summary = "Get user devices",
        description = "Retrieve all active device sessions for the authenticated user with device information and activity status."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "Device list retrieved successfully",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        name = "Success Response",
                        value = """{
                            "success": true,
                            "data": [
                                {
                                    "device_id": "MOBILE_ABC123_DEVICE_FINGERPRINT",
                                    "device_name": "John's iPhone 15",
                                    "device_type": "Mobile",
                                    "platform": "iOS",
                                    "browser": "Mobile App",
                                    "os": "iOS 17.1",
                                    "ip_address": "192.168.1.100",
                                    "location": null,
                                    "last_activity": "2025-01-04T10:30:00Z",
                                    "login_time": "2025-01-04T09:00:00Z",
                                    "is_current_device": true
                                },
                                {
                                    "device_id": "WEB_DEF456_BROWSER_HASH",
                                    "device_name": "Chrome on Windows",
                                    "device_type": "Desktop",
                                    "platform": "Windows",
                                    "browser": "Google Chrome",
                                    "os": "Windows 11",
                                    "ip_address": "192.168.1.101",
                                    "location": null,
                                    "last_activity": "2025-01-04T08:45:00Z",
                                    "login_time": "2025-01-04T08:00:00Z",
                                    "is_current_device": false
                                }
                            ],
                            "timestamp": "2025-01-04T10:04:56Z"
                        }"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "Authentication required"
            )
        ]
    )
    fun getUserDevices(request: HttpServletRequest): ApiResponse<List<DeviceSessionDto>> {
        // Get all active device sessions for the authenticated user
        return ApiResponse.success(authService.getUserDevices(request))
    }

    @PostMapping("/devices/{deviceId}/logout")
    @Operation(
        summary = "Logout from specific device",
        description = "Logout from a specific device session by device ID. The current device session and other device sessions remain active."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "Device logout successful",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        name = "Success Response",
                        value = """{
                            "success": true,
                            "data": {
                                "message": "Device logged out successfully"
                            },
                            "timestamp": "2025-01-04T10:04:56Z"
                        }"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "Authentication required"
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "Device not found or not associated with user"
            )
        ]
    )
    fun logoutFromDevice(
        @Parameter(
            description = "Device ID to logout from",
            example = "MOBILE_ABC123_DEVICE_FINGERPRINT"
        )
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
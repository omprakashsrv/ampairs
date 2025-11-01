package com.ampairs.auth.model.dto

import com.ampairs.core.validation.SafeString
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class FirebaseAuthRequest(

    @field:NotNull(message = "Firebase ID token is required")
    @field:NotBlank(message = "Firebase ID token cannot be blank")
    @field:SafeString(maxLength = 5000, message = "Firebase ID token contains invalid characters")
    var firebaseIdToken: String = "",

    @field:NotNull(message = "Phone number is required")
    @field:NotBlank(message = "Phone number cannot be blank")
    @field:Size(min = 10, max = 15, message = "Phone number must be between 10 and 15 characters")
    var phone: String = "",

    @field:NotNull(message = "Country code is required")
    var countryCode: Int = 91,

    @field:SafeString(maxLength = 1000, message = "reCAPTCHA token contains invalid characters")
    var recaptchaToken: String? = null,

    @field:SafeString(maxLength = 100, message = "Device ID contains invalid characters")
    @field:Size(max = 100, message = "Device ID cannot exceed 100 characters")
    var deviceId: String? = null,

    @field:SafeString(maxLength = 100, message = "Device name contains invalid characters")
    @field:Size(max = 100, message = "Device name cannot exceed 100 characters")
    var deviceName: String? = null,
)

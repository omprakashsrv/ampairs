package com.ampairs.auth.utils

import com.ampairs.auth.model.dto.AuthInitRequest
import com.ampairs.auth.model.dto.OtpVerificationRequest
import com.ampairs.core.utils.UniqueIdGenerators
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DeviceInfoExtractor {

    private val logger = LoggerFactory.getLogger(DeviceInfoExtractor::class.java)

    data class DeviceInfo(
        val deviceId: String,
        val deviceName: String,
        val deviceType: String,
        val platform: String,
        val browser: String,
        val os: String,
        val ipAddress: String,
        val userAgent: String,
        val location: String?,
    )

    /**
     * Extract device information from HTTP request
     */
    fun extractDeviceInfo(
        request: HttpServletRequest,
        providedDeviceId: String? = null,
        providedDeviceName: String? = null,
    ): DeviceInfo {
        val userAgent = request.getHeader("User-Agent") ?: "Unknown"
        val ipAddress = extractClientIp(request)

        // Generate device ID if not provided (for web clients)
        val deviceId = providedDeviceId ?: generateWebDeviceId(request)

        // Parse user agent to extract device information
        val parsedInfo = parseUserAgent(userAgent)

        // Use provided device name or generate one
        val deviceName = providedDeviceName ?: generateDeviceName(parsedInfo)

        return DeviceInfo(
            deviceId = deviceId,
            deviceName = deviceName,
            deviceType = parsedInfo.deviceType,
            platform = parsedInfo.platform,
            browser = parsedInfo.browser,
            os = parsedInfo.os,
            ipAddress = ipAddress,
            userAgent = userAgent,
            location = null // Can be enhanced with IP geolocation service
        )
    }

    /**
     * Extract device information from HTTP request with frontend device info for validation
     */
    fun extractDeviceInfoWithValidation(
        request: HttpServletRequest,
        authRequest: AuthInitRequest,
    ): DeviceInfo {
        val userAgent = request.getHeader("User-Agent") ?: "Unknown"
        val ipAddress = extractClientIp(request)

        // Generate device ID if not provided (for web clients)
        val deviceId = authRequest.deviceId ?: generateWebDeviceId(request)

        // Parse user agent to extract device information from server-side
        val serverParsed = parseUserAgent(userAgent)

        // Use provided device name or generate one
        val deviceName = authRequest.deviceName ?: generateDeviceName(serverParsed)

        // Validate frontend vs backend parsing
        validateDeviceInfo(authRequest, serverParsed, userAgent)

        return DeviceInfo(
            deviceId = deviceId,
            deviceName = deviceName,
            deviceType = serverParsed.deviceType,
            platform = serverParsed.platform,
            browser = serverParsed.browser,
            os = serverParsed.os,
            ipAddress = ipAddress,
            userAgent = userAgent,
            location = null // Can be enhanced with IP geolocation service
        )
    }

    /**
     * Extract device information from HTTP request with OTP verification request
     */
    fun extractDeviceInfoWithValidation(
        request: HttpServletRequest,
        otpRequest: OtpVerificationRequest,
    ): DeviceInfo {
        val userAgent = request.getHeader("User-Agent") ?: "Unknown"
        val ipAddress = extractClientIp(request)

        // Generate device ID if not provided (for web clients)
        val deviceId = otpRequest.deviceId ?: generateWebDeviceId(request)

        // Parse user agent to extract device information from server-side
        val serverParsed = parseUserAgent(userAgent)

        // Use provided device name or generate one
        val deviceName = otpRequest.deviceName ?: generateDeviceName(serverParsed)

        // Validate frontend vs backend parsing
        validateDeviceInfo(otpRequest, serverParsed, userAgent)

        return DeviceInfo(
            deviceId = deviceId,
            deviceName = deviceName,
            deviceType = serverParsed.deviceType,
            platform = serverParsed.platform,
            browser = serverParsed.browser,
            os = serverParsed.os,
            ipAddress = ipAddress,
            userAgent = userAgent,
            location = null // Can be enhanced with IP geolocation service
        )
    }

    /**
     * Validate frontend device info against server-side parsing
     */
    private fun validateDeviceInfo(authRequest: AuthInitRequest, serverParsed: ParsedUserAgent, userAgent: String) {
        val frontendInfo = mapOf(
            "device_type" to authRequest.deviceType,
            "platform" to authRequest.platform,
            "browser" to authRequest.browser,
            "os" to authRequest.os
        )

        val serverInfo = mapOf(
            "device_type" to serverParsed.deviceType,
            "platform" to serverParsed.platform,
            "browser" to serverParsed.browser,
            "os" to serverParsed.os
        )

        validateAndWarnDiscrepancies(frontendInfo, serverInfo, "AuthInit", userAgent)
    }

    /**
     * Validate frontend device info against server-side parsing for OTP verification
     */
    private fun validateDeviceInfo(
        otpRequest: OtpVerificationRequest,
        serverParsed: ParsedUserAgent,
        userAgent: String,
    ) {
        val frontendInfo = mapOf(
            "device_type" to otpRequest.deviceType,
            "platform" to otpRequest.platform,
            "browser" to otpRequest.browser,
            "os" to otpRequest.os
        )

        val serverInfo = mapOf(
            "device_type" to serverParsed.deviceType,
            "platform" to serverParsed.platform,
            "browser" to serverParsed.browser,
            "os" to serverParsed.os
        )

        validateAndWarnDiscrepancies(frontendInfo, serverInfo, "OtpVerification", userAgent)
    }

    /**
     * Compare frontend and backend device info and log warnings for discrepancies
     */
    private fun validateAndWarnDiscrepancies(
        frontendInfo: Map<String, String?>,
        serverInfo: Map<String, String?>,
        context: String,
        userAgent: String,
    ) {
        val discrepancies = mutableListOf<String>()

        frontendInfo.forEach { (field, frontendValue) ->
            val serverValue = serverInfo[field]

            // Skip null/empty frontend values
            if (frontendValue.isNullOrBlank()) return@forEach

            // Compare values (case-insensitive for robustness)
            if (frontendValue.lowercase() != serverValue?.lowercase()) {
                discrepancies.add("$field: frontend='$frontendValue' vs server='$serverValue'")
            }
        }

        if (discrepancies.isNotEmpty()) {
            logger.warn(
                "[$context] Device info discrepancies detected: {}. " +
                        "Frontend detection: {} | Server detection: {} | User-Agent: '{}'",
                discrepancies.joinToString(", "),
                frontendInfo.filterValues { !it.isNullOrBlank() },
                serverInfo,
                userAgent
            )
        } else {
            logger.debug("[$context] Device info validation passed - frontend and server parsing agree")
        }
    }

    /**
     * Extract client IP address from request headers
     */
    private fun extractClientIp(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            return xForwardedFor.split(",")[0].trim()
        }

        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            return xRealIp
        }

        return request.remoteAddr ?: "Unknown"
    }

    /**
     * Generate device ID for web clients based on request characteristics
     */
    private fun generateWebDeviceId(request: HttpServletRequest): String {
        val userAgent = request.getHeader("User-Agent") ?: ""
        val acceptLanguage = request.getHeader("Accept-Language") ?: ""
        val acceptEncoding = request.getHeader("Accept-Encoding") ?: ""

        // Create a unique signature based on browser characteristics
        val signature = "$userAgent|$acceptLanguage|$acceptEncoding"
        val hash = signature.hashCode().toString()

        return "WEB_${UniqueIdGenerators.ALPHANUMERIC.generate(8)}_$hash"
    }

    /**
     * Parse user agent string to extract device information
     */
    private fun parseUserAgent(userAgent: String): ParsedUserAgent {
        val ua = userAgent.lowercase()

        val browser = when {
            ua.contains("edg/") -> "Microsoft Edge"
            ua.contains("chrome/") && !ua.contains("edg/") -> "Google Chrome"
            ua.contains("firefox/") -> "Mozilla Firefox"
            ua.contains("safari/") && !ua.contains("chrome/") -> "Safari"
            ua.contains("opera/") || ua.contains("opr/") -> "Opera"
            ua.contains("mobile app") -> "Mobile App"
            else -> "Unknown Browser"
        }

        val os = when {
            ua.contains("windows nt 10.0") -> "Windows 10/11"
            ua.contains("windows nt 6.3") -> "Windows 8.1"
            ua.contains("windows nt 6.2") -> "Windows 8"
            ua.contains("windows nt 6.1") -> "Windows 7"
            ua.contains("windows") -> "Windows"
            ua.contains("mac os x") || ua.contains("macintosh") -> extractMacOSVersion(ua)
            ua.contains("iphone os") -> extractiOSVersion(ua)
            ua.contains("android") -> extractAndroidVersion(ua)
            ua.contains("linux") -> "Linux"
            ua.contains("ubuntu") -> "Ubuntu"
            else -> "Unknown OS"
        }

        val platform = when {
            ua.contains("iphone") || ua.contains("ipad") -> "iOS"
            ua.contains("android") -> "Android"
            ua.contains("windows") -> "Windows"
            ua.contains("mac os") || ua.contains("macintosh") -> "macOS"
            ua.contains("linux") -> "Linux"
            else -> "Web"
        }

        val deviceType = when {
            ua.contains("mobile") || ua.contains("iphone") -> "Mobile"
            ua.contains("ipad") || ua.contains("tablet") -> "Tablet"
            else -> "Desktop"
        }

        return ParsedUserAgent(browser, os, platform, deviceType)
    }

    private fun extractMacOSVersion(ua: String): String {
        val regex = "mac os x ([0-9_]+)".toRegex()
        val match = regex.find(ua)
        return if (match != null) {
            "macOS ${match.groupValues[1].replace("_", ".")}"
        } else {
            "macOS"
        }
    }

    private fun extractiOSVersion(ua: String): String {
        val regex = "os ([0-9_]+)".toRegex()
        val match = regex.find(ua)
        return if (match != null) {
            "iOS ${match.groupValues[1].replace("_", ".")}"
        } else {
            "iOS"
        }
    }

    private fun extractAndroidVersion(ua: String): String {
        val regex = "android ([0-9.]+)".toRegex()
        val match = regex.find(ua)
        return if (match != null) {
            "Android ${match.groupValues[1]}"
        } else {
            "Android"
        }
    }

    /**
     * Generate human-readable device name
     */
    private fun generateDeviceName(parsed: ParsedUserAgent): String {
        return when (parsed.platform.lowercase()) {
            "ios" -> when {
                parsed.deviceType == "Mobile" -> "iPhone"
                parsed.deviceType == "Tablet" -> "iPad"
                else -> "iOS Device"
            }

            "android" -> when {
                parsed.deviceType == "Mobile" -> "Android Phone"
                parsed.deviceType == "Tablet" -> "Android Tablet"
                else -> "Android Device"
            }

            "windows" -> "${parsed.browser} on Windows"
            "macos" -> "${parsed.browser} on Mac"
            "linux" -> "${parsed.browser} on Linux"
            else -> "${parsed.browser} Browser"
        }
    }

    private data class ParsedUserAgent(
        val browser: String,
        val os: String,
        val platform: String,
        val deviceType: String,
    )
}
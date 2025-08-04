package com.ampairs.auth.utils

import com.ampairs.core.utils.UniqueIdGenerators
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class DeviceInfoExtractor {

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
            ua.contains("mac os x") -> extractMacOSVersion(ua)
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
            ua.contains("mac os") -> "macOS"
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
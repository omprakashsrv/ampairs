package com.ampairs.auth.config

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment

/**
 * Guards the security-critical startup check that prevents the hardcoded-OTP login bypass (which
 * accepts the configured OTP for ANY account) from being silently enabled in production.
 */
class OtpSecurityStartupCheckTest {

    private fun check(props: OtpProperties, vararg profiles: String): OtpSecurityStartupCheck {
        val env = MockEnvironment().apply { setActiveProfiles(*profiles) }
        return OtpSecurityStartupCheck(props, env)
    }

    private fun bypassEnabled(prodOptIn: Boolean = false) = OtpProperties(
        developmentMode = true,
        hardcodedOtp = "123456",
        allowHardcoded = true,
        allowHardcodedInProduction = prodOptIn,
    )

    @Test
    fun `fails fast when the bypass is enabled under production without explicit opt-in`() {
        val subject = check(bypassEnabled(prodOptIn = false), "production")
        assertThrows(IllegalStateException::class.java) { subject.verifyOtpConfiguration() }
    }

    @Test
    fun `permits the bypass under production when explicitly opted in`() {
        val subject = check(bypassEnabled(prodOptIn = true), "production")
        assertDoesNotThrow { subject.verifyOtpConfiguration() }
    }

    @Test
    fun `permits the bypass outside the production profile`() {
        val subject = check(bypassEnabled(prodOptIn = false), "dev")
        assertDoesNotThrow { subject.verifyOtpConfiguration() }
    }

    @Test
    fun `is a no-op in production when the secure defaults are in effect`() {
        val secureDefaults = OtpProperties(
            developmentMode = false,
            hardcodedOtp = "",
            allowHardcoded = false,
        )
        val subject = check(secureDefaults, "production")
        assertDoesNotThrow { subject.verifyOtpConfiguration() }
    }
}

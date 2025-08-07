package com.ampairs.auth

import com.ampairs.AmpairsApplication
import com.ampairs.auth.model.dto.AuthInitRequest
import com.ampairs.auth.model.dto.AuthMode
import com.ampairs.auth.model.dto.AuthenticationRequest
import com.ampairs.auth.model.dto.RefreshTokenRequest
import com.ampairs.auth.service.JwtService
import com.ampairs.user.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(classes = [AmpairsApplication::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class JwtAuthenticationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jwtService: JwtService

    private fun authenticateTestUser(): String {
        // Initialize OTP session
        val initRequest = AuthInitRequest(
            phone = "9591781662",
            countryCode = 91,
            recaptchaToken = "test-token-12345"
        )

        val initResponse = mockMvc.perform(
            post("/auth/v1/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val sessionId = objectMapper.readTree(initResponse.response.contentAsString)
            .get("response").get("session_id").asText()

        // Authenticate with hardcoded OTP
        val authRequest = AuthenticationRequest(
            sessionId = sessionId,
            otp = "123456",
            authMode = AuthMode.OTP,
            recaptchaToken = "test-token-12345",
            deviceId = "TEST_DEVICE_001",
            deviceName = "Test Device"
        )

        val authResponse = mockMvc.perform(
            post("/auth/v1/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val authResponseBody = objectMapper.readTree(authResponse.response.contentAsString)
        return authResponseBody.get("response").get("access_token").asText()
    }

    @Test
    fun `should access protected endpoint with valid JWT`() {
        val accessToken = authenticateTestUser()

        mockMvc.perform(
            get("/user/v1")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.phone").value("9591781662"))
            .andExpect(jsonPath("$.country_code").value(91))
    }

    @Test
    fun `should reject access without JWT token`() {
        mockMvc.perform(
            get("/user/v1")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should reject access with invalid JWT token`() {
        mockMvc.perform(
            get("/user/v1")
                .header("Authorization", "Bearer invalid-token")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should refresh JWT token successfully`() {
        // First authenticate to get tokens
        val initRequest = AuthInitRequest(
            phone = "9591781662",
            countryCode = 91,
            recaptchaToken = "test-token-12345"
        )

        val initResponse = mockMvc.perform(
            post("/auth/v1/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val sessionId = objectMapper.readTree(initResponse.response.contentAsString)
            .get("response").get("session_id").asText()

        val authRequest = AuthenticationRequest(
            sessionId = sessionId,
            otp = "123456",
            authMode = AuthMode.OTP,
            recaptchaToken = "test-token-12345",
            deviceId = "TEST_DEVICE_002",
            deviceName = "Test Device 2"
        )

        val authResponse = mockMvc.perform(
            post("/auth/v1/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val authResponseBody = objectMapper.readTree(authResponse.response.contentAsString)
        val refreshToken = authResponseBody.get("response").get("refresh_token").asText()

        // Now refresh the token
        val refreshRequest = RefreshTokenRequest(
            refreshToken = refreshToken,
            deviceId = "TEST_DEVICE_002"
        )

        mockMvc.perform(
            post("/auth/v1/refresh_token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.response.access_token").exists())
            .andExpect(jsonPath("$.response.refresh_token").exists())
            .andExpect(jsonPath("$.response.access_token_expires_at").exists())
    }

    @Test
    fun `should reject refresh with invalid refresh token`() {
        val refreshRequest = RefreshTokenRequest().apply {
            refreshToken = "invalid-refresh-token"
            deviceId = "TEST_DEVICE_003"
        }

        mockMvc.perform(
            post("/auth/v1/refresh_token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest))
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `should logout successfully`() {
        val accessToken = authenticateTestUser()

        mockMvc.perform(
            post("/auth/v1/logout")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Device logged out successfully"))

        // Verify token is no longer valid (depends on token blacklisting implementation)
        // This test may need adjustment based on actual logout implementation
    }

    @Test
    fun `should get user devices`() {
        val accessToken = authenticateTestUser()

        mockMvc.perform(
            get("/auth/v1/devices")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].device_id").exists())
            .andExpect(jsonPath("$[0].device_name").exists())
            .andExpect(jsonPath("$[0].device_type").exists())
            .andExpect(jsonPath("$[0].platform").exists())
            .andExpect(jsonPath("$[0].last_activity").exists())
            .andExpect(jsonPath("$[0].is_current_device").exists())
    }

    @Test
    fun `should logout from all devices`() {
        val accessToken = authenticateTestUser()

        mockMvc.perform(
            post("/auth/v1/logout/all")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Logged out from all devices successfully"))
    }

    @Test
    fun `should support multiple device authentication`() {
        // Authenticate from first device
        val device1Token = authenticateTestUser()

        // Authenticate from second device with same phone number
        val initRequest2 = AuthInitRequest(
            phone = "9591781662",
            countryCode = 91,
            recaptchaToken = "test-token-12345"
        )

        val initResponse2 = mockMvc.perform(
            post("/auth/v1/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initRequest2))
        )
            .andExpect(status().isOk)
            .andReturn()

        val sessionId2 = objectMapper.readTree(initResponse2.response.contentAsString)
            .get("response").get("session_id").asText()

        val authRequest2 = AuthenticationRequest(
            sessionId = sessionId2,
            otp = "123456",
            authMode = AuthMode.OTP,
            recaptchaToken = "test-token-12345",
            deviceId = "TEST_DEVICE_SECOND",
            deviceName = "Test Device Second"
        )

        val authResponse2 = mockMvc.perform(
            post("/auth/v1/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest2))
        )
            .andExpect(status().isOk)
            .andReturn()

        val device2Token = objectMapper.readTree(authResponse2.response.contentAsString)
            .get("response").get("access_token").asText()

        // Both devices should be able to access protected endpoints
        mockMvc.perform(
            get("/user/v1")
                .header("Authorization", "Bearer $device1Token")
        )
            .andExpect(status().isOk)

        mockMvc.perform(
            get("/user/v1")
                .header("Authorization", "Bearer $device2Token")
        )
            .andExpect(status().isOk)

        // Check that both devices are listed
        mockMvc.perform(
            get("/auth/v1/devices")
                .header("Authorization", "Bearer $device1Token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }
}
package com.ampairs.auth

import com.ampairs.AmpairsApplication
import com.ampairs.auth.model.dto.AuthInitRequest
import com.ampairs.auth.model.dto.AuthMode
import com.ampairs.auth.model.dto.AuthenticationRequest
import com.ampairs.user.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.sql.DriverManager

class DatabaseInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        // Create the test database before Spring context starts
        try {
            val connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/",
                "root",
                "pass"
            )
            val statement = connection.createStatement()
            statement.executeUpdate("DROP DATABASE IF EXISTS ampairs_auth_test")
            statement.executeUpdate("CREATE DATABASE ampairs_auth_test")
            statement.close()
            connection.close()
            println("Test database 'ampairs_auth_test' created successfully")
        } catch (e: Exception) {
            println("Warning: Could not create test database: ${e.message}")
            e.printStackTrace()
        }
    }
}

@SpringBootTest(classes = [AmpairsApplication::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = [DatabaseInitializer::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class AuthIntegrationTest {

    companion object {
        @JvmStatic
        @AfterAll
        fun cleanupDatabase() {
            // Clean up the test database after all tests complete
            try {
                val connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/",
                    "root",
                    "pass"
                )
                val statement = connection.createStatement()
                statement.executeUpdate("DROP DATABASE IF EXISTS ampairs_auth_test")
                statement.close()
                connection.close()
                println("Test database 'ampairs_auth_test' dropped successfully")
            } catch (e: Exception) {
                println("Warning: Could not drop test database: ${e.message}")
            }
        }
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `should initialize OTP session successfully`() {
        val request = AuthInitRequest(
            phone = "9591781662",
            countryCode = 91,
            recaptchaToken = "test-token-12345"
        )

        mockMvc.perform(
            post("/auth/v1/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.response.success").value(true))
            .andExpect(jsonPath("$.response.message").value("OTP sent successfully"))
            .andExpect(jsonPath("$.response.session_id").exists())
    }

    @Test
    fun `should authenticate with hardcoded OTP in test environment`() {
        // First, initialize OTP session
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

        val initResponseBody = objectMapper.readTree(initResponse.response.contentAsString)
        val sessionId = initResponseBody.get("response").get("session_id").asText()

        // Then authenticate with hardcoded OTP
        val authRequest = AuthenticationRequest(
            sessionId = sessionId,
            otp = "123456", // Hardcoded OTP for tests
            authMode = AuthMode.OTP,
            recaptchaToken = "test-token-12345"
        )

        mockMvc.perform(
            post("/auth/v1/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.response.access_token").exists())
            .andExpect(jsonPath("$.response.refresh_token").exists())
            .andExpect(jsonPath("$.response.access_token_expires_at").exists())
            .andExpect(jsonPath("$.response.refresh_token_expires_at").exists())
    }

    @Test
    fun `should reject invalid OTP`() {
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

        val initResponseBody = objectMapper.readTree(initResponse.response.contentAsString)
        val sessionId = initResponseBody.get("response").get("session_id").asText()

        // Try to authenticate with wrong OTP
        val authRequest = AuthenticationRequest(
            sessionId = sessionId,
            otp = "999999", // Wrong OTP
            authMode = AuthMode.OTP,
            recaptchaToken = "test-token-12345"
        )

        mockMvc.perform(
            post("/auth/v1/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest))
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `should create new user on first authentication`() {
        val phone = "9876543210"
        val countryCode = 91

        // Ensure user doesn't exist
        val existingUser = userRepository.findByUserName("$countryCode$phone")
        assert(existingUser.isEmpty)

        // Initialize and authenticate
        val initRequest = AuthInitRequest(
            phone = phone,
            countryCode = countryCode,
            recaptchaToken = "test-token-12345",
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
            recaptchaToken = "test-token-12345"
        )

        mockMvc.perform(
            post("/auth/v1/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest))
        )
            .andExpect(status().isOk)

        // Verify user was created
        val createdUser = userRepository.findByUserName("$countryCode$phone")
        assert(createdUser.isPresent)
        assert(createdUser.get().phone == phone)
        assert(createdUser.get().countryCode == countryCode)
        assert(createdUser.get().active)
    }

    @Test
    fun `should validate session check endpoint`() {
        // Create a valid session
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

        // Check valid session
        mockMvc.perform(
            get("/auth/v1/session/{sessionId}", sessionId)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.response.id").value(sessionId))
            .andExpect(jsonPath("$.response.phone").value("9591781662"))
            .andExpect(jsonPath("$.response.country_code").value(91))
            .andExpect(jsonPath("$.response.valid").value(false))

        // Check invalid session
        mockMvc.perform(
            get("/auth/v1/session/{sessionId}", "invalid-session-id")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.response.id").value(""))
            .andExpect(jsonPath("$.response.valid").value(true))
    }

    @Test
    fun `should handle missing recaptcha token`() {
        val request = AuthInitRequest(
            phone = "9591781662",
            countryCode = 91,
            recaptchaToken = null // Missing recaptcha token
        )

        mockMvc.perform(
            post("/auth/v1/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should handle invalid phone number format`() {
        val request = AuthInitRequest(
            phone = "invalid", // Invalid phone format
            countryCode = 91,
            recaptchaToken = "test-token-12345"
        )

        mockMvc.perform(
            post("/auth/v1/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }
}
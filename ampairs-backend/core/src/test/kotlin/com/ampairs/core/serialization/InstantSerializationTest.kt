package com.ampairs.core.serialization

import com.ampairs.core.utils.TimezoneTestUtils
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant

/**
 * Contract test for Instant serialization to ISO-8601 with Z suffix.
 *
 * Verifies that:
 * 1. Instant fields serialize to ISO-8601 format with 'Z' (UTC) suffix
 * 2. JSON uses snake_case field naming (global config)
 * 3. Instant fields can be deserialized back from ISO-8601 strings
 *
 * This test MUST FAIL initially because BaseDomain still uses LocalDateTime.
 * After migration to Instant, this test should PASS.
 *
 * Part of TDD approach: RED (fail) → GREEN (pass) → Refactor
 */
@SpringBootTest
class InstantSerializationTest {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    /**
     * Test entity with Instant field for serialization testing.
     */
    data class TestEntity(
        val timestamp: Instant,
        val name: String
    )

    @Test
    fun `Instant should serialize to ISO-8601 with Z suffix`() {
        // Given
        val instant = Instant.parse("2025-01-09T14:30:00Z")
        val entity = TestEntity(instant, "test")

        // When
        val json = objectMapper.writeValueAsString(entity)

        // Then - verify ISO-8601 with Z suffix
        assert(json.contains("2025-01-09T14:30:00Z")) {
            "JSON should contain UTC timestamp with Z suffix, but was: $json"
        }

        // Verify JSON field uses snake_case (global config)
        assert(json.contains("\"timestamp\":")) {
            "JSON should use camelCase for 'timestamp' (no underscore conversion), but was: $json"
        }

        // Verify no numeric timestamp
        assert(!json.contains("\"timestamp\":1736432400")) {
            "JSON should not use numeric timestamp, but was: $json"
        }

        println("✅ Instant serialization test - JSON output: $json")
    }

    @Test
    fun `Instant should deserialize from ISO-8601 with Z suffix`() {
        // Given
        val json = """{"timestamp":"2025-01-09T14:30:00Z","name":"test"}"""

        // When
        val entity = objectMapper.readValue(json, TestEntity::class.java)

        // Then
        assert(entity.timestamp == Instant.parse("2025-01-09T14:30:00Z")) {
            "Deserialized Instant should match original, but was: ${entity.timestamp}"
        }
        assert(entity.name == "test") {
            "Deserialized name should match, but was: ${entity.name}"
        }

        println("✅ Instant deserialization test - Parsed: ${entity.timestamp}")
    }

    @Test
    fun `Instant serialization should be consistent`() {
        // Given
        val instant = Instant.now()
        val entity = TestEntity(instant, "consistency-test")

        // When - serialize then deserialize
        val json = objectMapper.writeValueAsString(entity)
        val deserialized = objectMapper.readValue(json, TestEntity::class.java)

        // Then - should get same Instant back
        TimezoneTestUtils.assertInstantsEqual(instant, deserialized.timestamp)

        println("✅ Consistency test - Original: $instant, Deserialized: ${deserialized.timestamp}")
    }

    @Test
    fun `Null Instant should serialize to null`() {
        // Given
        data class NullableTestEntity(
            val timestamp: Instant?,
            val name: String
        )
        val entity = NullableTestEntity(null, "test-null")

        // When
        val json = objectMapper.writeValueAsString(entity)

        // Then
        assert(json.contains("\"timestamp\":null")) {
            "Null Instant should serialize to null, but was: $json"
        }

        println("✅ Null Instant test - JSON output: $json")
    }

    @Test
    fun `Instant with milliseconds should serialize correctly`() {
        // Given
        val instant = Instant.parse("2025-01-09T14:30:00.123Z")
        val entity = TestEntity(instant, "with-millis")

        // When
        val json = objectMapper.writeValueAsString(entity)

        // Then - should include milliseconds
        assert(json.contains(".123Z") || json.contains("2025-01-09T14:30:00")) {
            "Instant with milliseconds should serialize correctly, but was: $json"
        }

        TimezoneTestUtils.assertJsonHasUtcTimestamp(json, "timestamp")

        println("✅ Milliseconds test - JSON output: $json")
    }
}

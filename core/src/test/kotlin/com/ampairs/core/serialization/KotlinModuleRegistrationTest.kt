package com.ampairs.core.serialization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

/**
 * Guards the Jackson 3 Kotlin module registration (tools.jackson.module:jackson-module-kotlin).
 *
 * Without it, missing JSON fields map to null instead of the Kotlin constructor default and
 * non-nullable primitives fail deserialization with "Cannot map `null` into type `int`"
 * (Jackson 3 enables FAIL_ON_NULL_FOR_PRIMITIVES by default). Spring Boot's auto-configuration
 * discovers the module the same way this test does — via findAndAddModules().
 */
class KotlinModuleRegistrationTest {

    data class DefaultedDto(
        val name: String = "",
        val step: Int = 1,
        val factor: Double = 2.5,
        val active: Boolean = true,
    )

    private val mapper = JsonMapper.builder()
        .findAndAddModules()
        .build()

    @Test
    fun `missing fields fall back to kotlin constructor defaults`() {
        val dto = mapper.readValue("""{"name":"x"}""", DefaultedDto::class.java)
        assertEquals("x", dto.name)
        assertEquals(1, dto.step)
        assertEquals(2.5, dto.factor)
        assertEquals(true, dto.active)
    }

    @Test
    fun `present fields override defaults`() {
        val dto = mapper.readValue("""{"name":"y","step":7,"active":false}""", DefaultedDto::class.java)
        assertEquals(7, dto.step)
        assertEquals(false, dto.active)
    }
}

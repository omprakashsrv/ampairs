package com.ampairs.form.domain.service.validation

import com.ampairs.form.domain.model.validation.FormatKind
import com.ampairs.form.domain.model.validation.ValidationRule
import com.ampairs.form.domain.model.validation.ValidationRuleType
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

/**
 * Unit tests for [ValidationEngine.assertConsistent] — the server-side defense that rejects
 * self-contradictory typed validation rules when a schema is saved (backs T047/T009).
 */
class ValidationEngineTest {

    private val engine = ValidationEngine()

    @Test
    fun `accepts a well-formed rule set`() {
        assertDoesNotThrow {
            engine.assertConsistent(
                "email",
                listOf(
                    ValidationRule(ValidationRuleType.REQUIRED),
                    ValidationRule(ValidationRuleType.LENGTH_RANGE, min = 1, max = 120),
                    ValidationRule(ValidationRuleType.FORMAT, kind = FormatKind.EMAIL),
                ),
            )
        }
    }

    @Test
    fun `rejects length-range with min greater than max`() {
        assertThrows(IllegalArgumentException::class.java) {
            engine.assertConsistent("name", listOf(ValidationRule(ValidationRuleType.LENGTH_RANGE, min = 10, max = 2)))
        }
    }

    @Test
    fun `rejects negative length bounds`() {
        assertThrows(IllegalArgumentException::class.java) {
            engine.assertConsistent("name", listOf(ValidationRule(ValidationRuleType.LENGTH_RANGE, min = -1)))
        }
    }

    @Test
    fun `rejects number-range with min greater than max`() {
        assertThrows(IllegalArgumentException::class.java) {
            engine.assertConsistent("price", listOf(ValidationRule(ValidationRuleType.NUMBER_RANGE, minValue = 100.0, maxValue = 10.0)))
        }
    }

    @Test
    fun `rejects allowed-choices with no values`() {
        assertThrows(IllegalArgumentException::class.java) {
            engine.assertConsistent("type", listOf(ValidationRule(ValidationRuleType.ALLOWED_CHOICES, values = emptyList())))
        }
    }

    @Test
    fun `rejects format rule without a kind`() {
        assertThrows(IllegalArgumentException::class.java) {
            engine.assertConsistent("gst", listOf(ValidationRule(ValidationRuleType.FORMAT, kind = null)))
        }
    }
}

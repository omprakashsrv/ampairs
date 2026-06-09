package com.ampairs.form.domain.service.validation

import com.ampairs.form.domain.model.validation.ValidationRule
import com.ampairs.form.domain.model.validation.ValidationRuleType
import org.springframework.stereotype.Component

/**
 * Shared interpretation of typed [ValidationRule]s. The same rule vocabulary is enforced on the
 * client renderer (inline at entry) and here on the server (defense in depth). This server side
 * currently enforces rule *consistency* (rejecting self-contradictory rules) when a schema is saved;
 * per-value enforcement is primarily a client concern.
 */
@Component
class ValidationEngine {

    /** @throws IllegalArgumentException when a rule set is internally contradictory. */
    fun assertConsistent(fieldKey: String, rules: List<ValidationRule>) {
        rules.forEach { rule ->
            when (rule.type) {
                ValidationRuleType.LENGTH_RANGE -> {
                    val min = rule.min
                    val max = rule.max
                    if (min != null && max != null && min > max) {
                        throw IllegalArgumentException(
                            "Field '$fieldKey': length-range min ($min) cannot exceed max ($max)"
                        )
                    }
                    if ((min != null && min < 0) || (max != null && max < 0)) {
                        throw IllegalArgumentException("Field '$fieldKey': length-range bounds cannot be negative")
                    }
                }
                ValidationRuleType.NUMBER_RANGE -> {
                    val min = rule.minValue
                    val max = rule.maxValue
                    if (min != null && max != null && min > max) {
                        throw IllegalArgumentException(
                            "Field '$fieldKey': number-range min ($min) cannot exceed max ($max)"
                        )
                    }
                }
                ValidationRuleType.ALLOWED_CHOICES -> {
                    if (rule.values.isNullOrEmpty()) {
                        throw IllegalArgumentException("Field '$fieldKey': allowed-choices rule needs at least one value")
                    }
                }
                ValidationRuleType.FORMAT -> {
                    if (rule.kind == null) {
                        throw IllegalArgumentException("Field '$fieldKey': format rule needs a kind")
                    }
                }
                ValidationRuleType.REQUIRED -> Unit
            }
        }
    }
}

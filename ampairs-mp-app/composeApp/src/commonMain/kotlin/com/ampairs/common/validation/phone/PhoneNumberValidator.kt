package com.ampairs.common.validation.phone

import com.ampairs.common.validation.Invalid
import com.ampairs.common.validation.Valid
import com.ampairs.common.validation.ValidationResult
import com.ampairs.common.validation.Validator

class PhoneNumberValidator : Validator<String?, String> {

    companion object {
        private val PHONE_NUMBER_REGEX = Regex("^[6789]\\d{9}\$")
    }

    override fun validate(input: String?): ValidationResult<String> {
        if (input == null) return Invalid(PhoneNumberValidationError.InputIsNull)

        if (!input.matches(PHONE_NUMBER_REGEX)) return Invalid(PhoneNumberValidationError.InvalidFormat)

        return Valid(input)
    }
}
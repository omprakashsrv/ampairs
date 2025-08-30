package com.ampairs.workspace.validation

import com.ampairs.workspace.model.dto.CreateInvitationRequest
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ContactMethodValidator::class])
annotation class ValidContactMethod(
    val message: String = "Either email or phone number must be provided, but not both",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class ContactMethodValidator : ConstraintValidator<ValidContactMethod, CreateInvitationRequest> {
    override fun initialize(constraintAnnotation: ValidContactMethod) {}

    override fun isValid(request: CreateInvitationRequest?, context: ConstraintValidatorContext): Boolean {
        if (request == null) return true

        val hasEmail = !request.recipientEmail.isNullOrBlank()
        val hasPhone = !request.recipientPhone.isNullOrBlank()

        return hasEmail xor hasPhone
    }
}

package com.ampairs.core.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Comprehensive business data validation service
 * Provides validation for Indian business-specific data formats and patterns
 */
@Service
class BusinessDataValidator(
    private val validationService: ValidationService,
) {

    private val logger = LoggerFactory.getLogger(BusinessDataValidator::class.java)

    /**
     * Comprehensive validation result
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList(),
    ) {
        companion object {
            fun valid() = ValidationResult(true)
            fun invalid(errors: List<String>) = ValidationResult(false, errors)
            fun withWarnings(warnings: List<String>) = ValidationResult(true, emptyList(), warnings)
        }
    }

    /**
     * Validate customer registration data comprehensively
     */
    fun validateCustomerRegistration(
        name: String?,
        phone: String?,
        countryCode: Int?,
        email: String?,
        gstin: String?,
        pan: String?,
        address: String?,
        pincode: String?,
        state: String?,
        city: String?,
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Name validation
        if (name.isNullOrBlank()) {
            errors.add("Customer name is required")
        } else {
            try {
                val sanitizedName = validationService.validateAndSanitizeInput(name, "customer_name", 100)
                if (sanitizedName.length < 2) {
                    errors.add("Customer name must be at least 2 characters long")
                }
                if (!validationService.isValidAlphanumeric(sanitizedName)) {
                    warnings.add("Customer name contains special characters")
                }
            } catch (e: SecurityException) {
                errors.add("Customer name contains invalid or dangerous characters")
            }
        }

        // Phone validation
        if (phone.isNullOrBlank()) {
            errors.add("Phone number is required")
        } else {
            if (!validationService.isValidPhone(phone)) {
                errors.add("Phone number must be exactly 10 digits")
            }

            // Indian mobile number validation
            if (countryCode == 91 && !validationService.isValidIndianMobile(phone, countryCode)) {
                errors.add("Invalid Indian mobile number (must start with 6-9)")
            }
        }

        // Country code validation
        if (countryCode == null) {
            errors.add("Country code is required")
        } else if (!validationService.isValidCountryCode(countryCode)) {
            errors.add("Invalid country code")
        }

        // Email validation
        if (!email.isNullOrBlank() && !validationService.isValidEmail(email)) {
            errors.add("Invalid email format")
        }

        // GSTIN validation
        if (!gstin.isNullOrBlank() && !validationService.isValidGstin(gstin)) {
            errors.add("Invalid GSTIN format")
        }

        // PAN validation
        if (!pan.isNullOrBlank() && !validationService.isValidPan(pan)) {
            errors.add("Invalid PAN format")
        }

        // Address validation
        if (!address.isNullOrBlank()) {
            try {
                validationService.validateAndSanitizeInput(address, "address", 500)
            } catch (e: SecurityException) {
                errors.add("Address contains invalid characters")
            }
        }

        // Pincode validation
        if (!pincode.isNullOrBlank() && !validationService.isValidPincode(pincode)) {
            errors.add("Pincode must be exactly 6 digits")
        }

        // State validation
        if (!state.isNullOrBlank()) {
            try {
                validationService.validateAndSanitizeInput(state, "state", 100)
            } catch (e: SecurityException) {
                errors.add("State name contains invalid characters")
            }
        }

        // City validation
        if (!city.isNullOrBlank()) {
            try {
                validationService.validateAndSanitizeInput(city, "city", 100)
            } catch (e: SecurityException) {
                errors.add("City name contains invalid characters")
            }
        }

        return if (errors.isEmpty()) {
            if (warnings.isEmpty()) ValidationResult.valid() else ValidationResult.withWarnings(warnings)
        } else {
            ValidationResult.invalid(errors)
        }
    }

    /**
     * Validate product data comprehensively
     */
    fun validateProductData(
        name: String?,
        code: String?,
        description: String?,
        category: String?,
        price: Double?,
        costPrice: Double?,
        stockQuantity: Double?,
        minStockLevel: Double?,
        taxRate: Double?,
        unit: String?,
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Product name validation
        if (name.isNullOrBlank()) {
            errors.add("Product name is required")
        } else {
            try {
                val sanitizedName = validationService.validateAndSanitizeInput(name, "product_name", 200)
                if (sanitizedName.length < 2) {
                    errors.add("Product name must be at least 2 characters long")
                }
            } catch (e: SecurityException) {
                errors.add("Product name contains invalid characters")
            }
        }

        // Product code validation
        if (!code.isNullOrBlank()) {
            if (!validationService.isValidAlphanumeric(code)) {
                errors.add("Product code can only contain letters, numbers, and basic symbols")
            }
            if (code.length > 50) {
                errors.add("Product code cannot exceed 50 characters")
            }
        }

        // Description validation
        if (!description.isNullOrBlank()) {
            try {
                validationService.validateAndSanitizeInput(description, "description", 1000)
            } catch (e: SecurityException) {
                errors.add("Description contains invalid characters")
            }
        }

        // Category validation
        if (!category.isNullOrBlank()) {
            try {
                validationService.validateAndSanitizeInput(category, "category", 100)
            } catch (e: SecurityException) {
                errors.add("Category contains invalid characters")
            }
        }

        // Price validations
        price?.let {
            if (!validationService.isValidNumericRange(it, 0.01, 9999999.99)) {
                errors.add("Price must be between 0.01 and 9,999,999.99")
            }
        }

        costPrice?.let {
            if (!validationService.isValidNumericRange(it, 0.01, 9999999.99)) {
                errors.add("Cost price must be between 0.01 and 9,999,999.99")
            }
        }

        // Price comparison validation
        if (price != null && costPrice != null && costPrice > price) {
            warnings.add("Cost price is higher than selling price")
        }

        // Stock validation
        stockQuantity?.let {
            if (!validationService.isValidNumericRange(it, 0.0, 999999.99)) {
                errors.add("Stock quantity must be between 0 and 999,999.99")
            }
        }

        minStockLevel?.let {
            if (!validationService.isValidNumericRange(it, 0.0, 999999.99)) {
                errors.add("Minimum stock level must be between 0 and 999,999.99")
            }
        }

        // Tax rate validation
        taxRate?.let {
            if (!validationService.isValidNumericRange(it, 0.0, 100.0)) {
                errors.add("Tax rate must be between 0% and 100%")
            }
        }

        // Unit validation
        if (!unit.isNullOrBlank()) {
            try {
                validationService.validateAndSanitizeInput(unit, "unit", 50)
            } catch (e: SecurityException) {
                errors.add("Unit contains invalid characters")
            }
        }

        return if (errors.isEmpty()) {
            if (warnings.isEmpty()) ValidationResult.valid() else ValidationResult.withWarnings(warnings)
        } else {
            ValidationResult.invalid(errors)
        }
    }

    /**
     * Validate order data comprehensively
     */
    fun validateOrderData(
        customerId: String?,
        items: List<OrderItemData>?,
        totalAmount: Double?,
        discount: Double?,
        taxAmount: Double?,
        notes: String?,
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Customer ID validation
        if (customerId.isNullOrBlank()) {
            errors.add("Customer ID is required")
        } else {
            try {
                validationService.validateAndSanitizeInput(customerId, "customer_id", 50)
            } catch (e: SecurityException) {
                errors.add("Customer ID contains invalid characters")
            }
        }

        // Order items validation
        if (items.isNullOrEmpty()) {
            errors.add("At least one order item is required")
        } else {
            items.forEachIndexed { index, item ->
                val itemErrors = validateOrderItem(item, index + 1)
                errors.addAll(itemErrors)
            }
        }

        // Amount validations
        totalAmount?.let {
            if (!validationService.isValidNumericRange(it, 0.01, 10000000.0)) {
                errors.add("Total amount must be between 0.01 and 10,000,000")
            }
        }

        discount?.let {
            if (!validationService.isValidNumericRange(it, 0.0, totalAmount ?: 10000000.0)) {
                errors.add("Discount cannot be negative or exceed total amount")
            }
        }

        taxAmount?.let {
            if (!validationService.isValidNumericRange(it, 0.0, 10000000.0)) {
                errors.add("Tax amount cannot be negative")
            }
        }

        // Notes validation
        if (!notes.isNullOrBlank()) {
            try {
                validationService.validateAndSanitizeInput(notes, "notes", 1000)
            } catch (e: SecurityException) {
                errors.add("Notes contain invalid characters")
            }
        }

        return if (errors.isEmpty()) {
            if (warnings.isEmpty()) ValidationResult.valid() else ValidationResult.withWarnings(warnings)
        } else {
            ValidationResult.invalid(errors)
        }
    }

    /**
     * Validate individual order item
     */
    private fun validateOrderItem(item: OrderItemData, itemNumber: Int): List<String> {
        val errors = mutableListOf<String>()

        if (item.productId.isNullOrBlank()) {
            errors.add("Product ID is required for item $itemNumber")
        }

        if (item.quantity == null || item.quantity <= 0) {
            errors.add("Quantity must be greater than 0 for item $itemNumber")
        }

        if (item.unitPrice == null || !validationService.isValidNumericRange(item.unitPrice, 0.01, 9999999.99)) {
            errors.add("Unit price must be between 0.01 and 9,999,999.99 for item $itemNumber")
        }

        return errors
    }

    /**
     * Validate financial transaction data
     */
    fun validateFinancialTransaction(
        amount: Double?,
        accountNumber: String?,
        ifscCode: String?,
        beneficiaryName: String?,
        description: String?,
        transactionType: String?,
    ): ValidationResult {
        return ValidationResult(
            isValid = validationService.validateFinancialData(amount, accountNumber, ifscCode, description).isEmpty(),
            errors = validationService.validateFinancialData(amount, accountNumber, ifscCode, description)
                .toMutableList().apply {
                // Additional validations
                if (!beneficiaryName.isNullOrBlank()) {
                    try {
                        validationService.validateAndSanitizeInput(beneficiaryName, "beneficiary_name", 100)
                    } catch (e: SecurityException) {
                        add("Beneficiary name contains invalid characters")
                    }
                }

                if (!transactionType.isNullOrBlank()) {
                    val validTypes = setOf("CREDIT", "DEBIT", "TRANSFER", "PAYMENT", "REFUND")
                    if (transactionType.uppercase() !in validTypes) {
                        add("Invalid transaction type")
                    }
                }
            }
        )
    }

    /**
     * Data class for order item validation
     */
    data class OrderItemData(
        val productId: String?,
        val quantity: Double?,
        val unitPrice: Double?,
    )
}
package com.ampairs.customer.domain.service

import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.domain.model.State
import com.ampairs.customer.repository.CustomerPagingRepository
import com.ampairs.customer.repository.CustomerRepository
import com.ampairs.customer.repository.StateRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class CustomerService @Autowired constructor(
    val customerRepository: CustomerRepository,
    val customerPagingRepository: CustomerPagingRepository,
    val stateRepository: StateRepository,
    val customerConfigValidationService: CustomerConfigValidationService
) {

    @Transactional
    fun updateCustomer(customer: Customer): Customer {
        return customerRepository.save(customer)
    }

    @Transactional
    fun updateCustomers(customers: List<Customer>): List<Customer> {
        customers.forEach { customer ->
            if (customer.uid.isNotEmpty()) {
                val existingCustomer = customerRepository.findByUid(customer.uid)
                customer.refId = existingCustomer?.refId ?: ""
                customer.createdAt = existingCustomer?.createdAt ?: LocalDateTime.now()
                customer.updatedAt = existingCustomer?.updatedAt ?: LocalDateTime.now()
            } else if (customer.refId?.isNotEmpty() == true) {
                val existingCustomer = customerRepository.findByRefId(customer.refId)
                customer.uid = existingCustomer?.uid ?: ""
                customer.createdAt = existingCustomer?.createdAt ?: LocalDateTime.now()
                customer.updatedAt = existingCustomer?.updatedAt ?: LocalDateTime.now()
            }
            customer.lastUpdated = System.currentTimeMillis()
            customerRepository.save(customer)
        }
        return customers
    }

    fun getCustomers(lastUpdate: Long?): List<Customer> {
        val customers =
            customerPagingRepository.findAllByLastUpdatedGreaterThanEqual(
                lastUpdate ?: 0, PageRequest.of(0, 1000, Sort.by("lastUpdated").ascending())
            )
        return customers
    }

    fun getStates(): List<State> {
        return stateRepository.findAll().toMutableList()
    }

    fun getCustomersAfterSync(lastSync: String?, pageable: Pageable): Page<Customer> {
        return if (lastSync.isNullOrBlank()) {
            // If no last_sync provided, return all customers with pagination
            customerRepository.findAll(pageable)
        } else {
            try {
                // URL decode the datetime string first (handles %3A to : conversion)
                val decodedLastSync = URLDecoder.decode(lastSync, StandardCharsets.UTF_8)

                // Parse ISO datetime string to LocalDateTime (supports both with and without 'T')
                val lastSyncDateTime = if (decodedLastSync.contains('T')) {
                    LocalDateTime.parse(decodedLastSync, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                } else {
                    LocalDateTime.parse(decodedLastSync.replace(' ', 'T'), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                }

                customerRepository.findCustomersUpdatedAfter(lastSyncDateTime, pageable)
            } catch (e: Exception) {
                // If parsing fails, return all customers with pagination
                customerRepository.findAll(pageable)
            }
        }
    }

    /**
     * Retail-specific customer management methods
     */

    @Transactional
    fun createCustomer(customer: Customer): Customer {

        // Apply default attribute values
        customerConfigValidationService.applyDefaultAttributes(customer)

        // Validate against field configurations and attribute definitions
        val validationResult = customerConfigValidationService.validateCustomer(customer)
        if (!validationResult.valid) {
            throw IllegalArgumentException("Customer validation failed: ${validationResult.errors.joinToString("; ")}")
        }

        // Validate GST number if provided
        if (!customer.isValidGstNumber()) {
            throw IllegalArgumentException("Invalid GST number format: ${customer.gstNumber}")
        }

        customer.gstNumber?.let {
            if (customerRepository.findByGstNumber(it).isPresent) {
                throw IllegalArgumentException("GST number already exists: $it")
            }
        }

        customer.status = "ACTIVE"
        customer.lastUpdated = System.currentTimeMillis()
        return customerRepository.save(customer)
    }

    @Transactional
    fun updateCustomer(customerId: String, updates: Customer): Customer? {
        val existingCustomer = customerRepository.findByUid(customerId) ?: return null

        // Update fields
        existingCustomer.name = updates.name.takeIf { it.isNotBlank() } ?: existingCustomer.name
        existingCustomer.phone = updates.phone.takeIf { it.isNotBlank() } ?: existingCustomer.phone
        existingCustomer.email = updates.email.takeIf { it.isNotBlank() } ?: existingCustomer.email
        existingCustomer.customerType = updates.customerType
        existingCustomer.creditLimit = updates.creditLimit.takeIf { it >= 0 } ?: existingCustomer.creditLimit
        existingCustomer.creditDays = updates.creditDays.takeIf { it >= 0 } ?: existingCustomer.creditDays
        existingCustomer.attributes = updates.attributes
        existingCustomer.status = updates.status.takeIf { it.isNotBlank() } ?: existingCustomer.status

        // Validate against field configurations and attribute definitions
        val validationResult = customerConfigValidationService.validateCustomer(existingCustomer)
        if (!validationResult.valid) {
            throw IllegalArgumentException("Customer validation failed: ${validationResult.errors.joinToString("; ")}")
        }

        // Validate GST number if updated
        if (updates.gstNumber != existingCustomer.gstNumber && !existingCustomer.isValidGstNumber()) {
            throw IllegalArgumentException("Invalid GST number format: ${updates.gstNumber}")
        }

        existingCustomer.lastUpdated = System.currentTimeMillis()
        return customerRepository.save(existingCustomer)
    }

    fun searchCustomers(
        searchTerm: String?,
        customerType: String?,
        city: String?,
        state: String?,
        hasCredit: Boolean?,
        hasOutstanding: Boolean?,
        pageable: Pageable
    ): Page<Customer> {
        return when {
            !searchTerm.isNullOrBlank() -> customerRepository.searchCustomers(searchTerm, pageable)
            customerType != null -> customerRepository.findActiveCustomersByType(customerType, pageable)
            !city.isNullOrBlank() -> customerRepository.findActiveCustomersByCity(city, pageable)
            !state.isNullOrBlank() -> customerRepository.findActiveCustomersByState(state, pageable)
            hasCredit == true -> customerRepository.findCustomersWithCredit(pageable)
            hasOutstanding == true -> customerRepository.findCustomersWithOutstanding(pageable)
            else -> customerRepository.findByStatus("ACTIVE").let { 
                org.springframework.data.domain.PageImpl(it, pageable, it.size.toLong()) 
            }
        }
    }

    fun getCustomerByGstNumber(gstNumber: String): Customer? {
        return customerRepository.findByGstNumber(gstNumber).orElse(null)
    }

    fun getActiveCustomers(pageable: Pageable): List<Customer> {
        return customerRepository.findByStatus("ACTIVE")
    }

    @Transactional
    fun updateOutstanding(customerId: String, amount: Double, isPayment: Boolean = false): Customer? {
        val customer = customerRepository.findByUid(customerId) ?: return null
        
        if (isPayment) {
            customer.reduceOutstanding(amount)
        } else {
            customer.addToOutstanding(amount)
        }
        
        customer.lastUpdated = System.currentTimeMillis()
        return customerRepository.save(customer)
    }

    fun validateGstNumber(gstNumber: String): Boolean {
        return gstNumber.matches(Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$"))
    }

    @Transactional
    fun upsertCustomer(customer: Customer): Customer {
        return if (customer.uid.isNotEmpty()) {
            // Check if customer exists with this UID
            val existingCustomer = customerRepository.findByUid(customer.uid)
            if (existingCustomer != null) {
                // Customer exists, update it
                updateCustomer(customer)
            } else {
                // Customer doesn't exist, create new one
                createCustomer(customer)
            }
        } else {
            // No UID provided, create new customer
            createCustomer(customer)
        }
    }
}

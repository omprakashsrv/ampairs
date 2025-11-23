package com.ampairs.customer.domain.service

import com.ampairs.core.multitenancy.DeviceContextHolder
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.security.AuthenticationHelper
import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.domain.model.State
import com.ampairs.customer.repository.CustomerPagingRepository
import com.ampairs.customer.repository.CustomerRepository
import com.ampairs.customer.repository.StateRepository
import com.ampairs.event.domain.events.CustomerCreatedEvent
import com.ampairs.event.domain.events.CustomerDeletedEvent
import com.ampairs.event.domain.events.CustomerUpdatedEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*

@Service
class CustomerService @Autowired constructor(
    val customerRepository: CustomerRepository,
    val customerPagingRepository: CustomerPagingRepository,
    val stateRepository: StateRepository,
    val eventPublisher: ApplicationEventPublisher
) {

    /**
     * Helper methods for event publishing
     */
    private fun getWorkspaceId(): String = TenantContextHolder.getCurrentTenant() ?: ""

    private fun getUserId(): String {
        val auth = SecurityContextHolder.getContext().authentication
        return auth?.let { AuthenticationHelper.getCurrentUserId(it) } ?: ""
    }

    private fun getDeviceId(): String = DeviceContextHolder.getCurrentDevice() ?: ""

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
                customer.createdAt = existingCustomer?.createdAt ?: Instant.now()
                customer.updatedAt = existingCustomer?.updatedAt ?: Instant.now()
            } else if (customer.refId?.isNotEmpty() == true) {
                val existingCustomer = customerRepository.findByRefId(customer.refId)
                customer.uid = existingCustomer?.uid ?: ""
                customer.createdAt = existingCustomer?.createdAt ?: Instant.now()
                customer.updatedAt = existingCustomer?.updatedAt ?: Instant.now()
            }
            customerRepository.save(customer)
        }
        return customers
    }

    fun getCustomers(): List<Customer> {
        val customers =
            customerPagingRepository.findAllByUpdatedAtGreaterThanEqual(Instant.MIN ,PageRequest.of(0, 1000, Sort.by("updatedAt").ascending())
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

                // Parse ISO-8601 datetime string to Instant (expects format like "2025-01-09T14:30:00Z")
                val lastSyncInstant = Instant.parse(decodedLastSync)

                customerRepository.findCustomersUpdatedAfter(lastSyncInstant, pageable)
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
        val savedCustomer = customerRepository.save(customer)

        // Publish CustomerCreatedEvent
        eventPublisher.publishEvent(
            CustomerCreatedEvent(
                source = this,
                workspaceId = getWorkspaceId(),
                entityId = savedCustomer.uid,
                userId = getUserId(),
                deviceId = getDeviceId(),
                customerName = savedCustomer.name,
                customerType = savedCustomer.customerType
            )
        )

        return savedCustomer
    }

    @Transactional
    fun updateCustomer(customerId: String, updates: Customer): Customer? {
        val existingCustomer = customerRepository.findByUid(customerId) ?: return null

        // Track changes for event
        val fieldChanges = mutableMapOf<String, Any>()

        // Update fields and track changes
        if (updates.name.isNotBlank() && updates.name != existingCustomer.name) {
            fieldChanges["name"] = mapOf("old" to existingCustomer.name, "new" to updates.name)
            existingCustomer.name = updates.name
        }
        if (updates.phone.isNotBlank() && updates.phone != existingCustomer.phone) {
            fieldChanges["phone"] = mapOf("old" to existingCustomer.phone, "new" to updates.phone)
            existingCustomer.phone = updates.phone
        }
        if (updates.email.isNotBlank() && updates.email != existingCustomer.email) {
            fieldChanges["email"] = mapOf("old" to existingCustomer.email, "new" to updates.email)
            existingCustomer.email = updates.email
        }
        if (updates.customerType != existingCustomer.customerType) {
            fieldChanges["customerType"] = mapOf("old" to existingCustomer.customerType, "new" to updates.customerType)
            existingCustomer.customerType = updates.customerType
        }
        if (updates.creditLimit >= 0 && updates.creditLimit != existingCustomer.creditLimit) {
            fieldChanges["creditLimit"] = mapOf("old" to existingCustomer.creditLimit, "new" to updates.creditLimit)
            existingCustomer.creditLimit = updates.creditLimit
        }
        if (updates.creditDays >= 0 && updates.creditDays != existingCustomer.creditDays) {
            fieldChanges["creditDays"] = mapOf("old" to existingCustomer.creditDays, "new" to updates.creditDays)
            existingCustomer.creditDays = updates.creditDays
        }
        if (updates.attributes?.isNotEmpty() == true && updates.attributes != existingCustomer.attributes) {
            fieldChanges["attributes"] = mapOf("old" to existingCustomer.attributes, "new" to updates.attributes!!)
            existingCustomer.attributes = updates.attributes
        }
        if (updates.status.isNotBlank() && updates.status != existingCustomer.status) {
            fieldChanges["status"] = mapOf("old" to existingCustomer.status, "new" to updates.status)
            existingCustomer.status = updates.status
        }

        // Validate GST number if updated
        if (updates.gstNumber != existingCustomer.gstNumber && !existingCustomer.isValidGstNumber()) {
            throw IllegalArgumentException("Invalid GST number format: ${updates.gstNumber}")
        }

        val savedCustomer = customerRepository.save(existingCustomer)

        // Publish CustomerUpdatedEvent only if there were changes
        if (fieldChanges.isNotEmpty()) {
            eventPublisher.publishEvent(
                CustomerUpdatedEvent(
                    source = this,
                    workspaceId = getWorkspaceId(),
                    entityId = savedCustomer.uid,
                    userId = getUserId(),
                    deviceId = getDeviceId(),
                    fieldChanges = fieldChanges
                )
            )
        }

        return savedCustomer
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
        
        return customerRepository.save(customer)
    }

    fun validateGstNumber(gstNumber: String): Boolean {
        return gstNumber.matches(Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$"))
    }

    /**
     * Soft delete a customer by setting status to DELETED
     */
    @Transactional
    fun deleteCustomer(customerId: String): Boolean {
        val customer = customerRepository.findByUid(customerId) ?: return false

        customer.status = "DELETED"
        customerRepository.save(customer)

        // Publish CustomerDeletedEvent
        eventPublisher.publishEvent(
            CustomerDeletedEvent(
                source = this,
                workspaceId = getWorkspaceId(),
                entityId = customer.uid,
                userId = getUserId(),
                deviceId = getDeviceId(),
                customerName = customer.name
            )
        )

        return true
    }

    @Transactional
    fun upsertCustomer(customer: Customer): Customer {
        return if (customer.uid.isNotEmpty()) {
            // Check if customer exists with this UID
            val existingCustomer = customerRepository.findByUid(customer.uid)
            if (existingCustomer != null) {
                // Customer exists, update it
                customer.id = existingCustomer.id
                customer.images = existingCustomer.images
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

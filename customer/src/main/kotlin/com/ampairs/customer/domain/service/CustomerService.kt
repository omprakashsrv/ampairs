package com.ampairs.customer.domain.service

import com.ampairs.core.multitenancy.DeviceContextHolder
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.security.AuthenticationHelper
import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.domain.model.State
import com.ampairs.customer.repository.CustomerPagingRepository
import com.ampairs.customer.repository.CustomerRepository
import com.ampairs.customer.repository.StateRepository
import com.ampairs.event.domain.events.CustomerCreatedEvent
import com.ampairs.event.domain.events.CustomerDeletedEvent
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

@Service
@Transactional(readOnly = true)
class CustomerService(
    val customerRepository: CustomerRepository,
    val customerPagingRepository: CustomerPagingRepository,
    val stateRepository: StateRepository,
    val eventPublisher: ApplicationEventPublisher,
    private val entityChangePublisher: EntityChangePublisher,
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
                customer.id = existingCustomer?.id ?: 0
                // Prefer the client-sent ref_id (e.g. Tally GUID); fall back to the existing one
                // so a blank incoming value never wipes a stored ref_id.
                customer.refId = customer.refId?.takeIf { it.isNotBlank() } ?: existingCustomer?.refId ?: ""
                customer.createdAt = existingCustomer?.createdAt ?: Instant.now()
                customer.updatedAt = existingCustomer?.updatedAt ?: Instant.now()
            } else if (customer.refId?.isNotEmpty() == true) {
                val existingCustomer = customerRepository.findByRefId(customer.refId)
                customer.id = existingCustomer?.id ?: 0
                customer.uid = existingCustomer?.uid ?: ""
                customer.createdAt = existingCustomer?.createdAt ?: Instant.now()
                customer.updatedAt = existingCustomer?.updatedAt ?: Instant.now()
            }
            val saved = customerRepository.save(customer)
            // Broadcast so other devices of this workspace pull the bulk-synced change.
            entityChangePublisher.publish(
                "customer",
                saved.uid,
                if (saved.status.equals("DELETED", ignoreCase = true)) com.ampairs.core.sync.EntityChangeType.DELETED
                else com.ampairs.core.sync.EntityChangeType.UPDATED,
            )
        }
        return customers
    }

    fun getCustomers(): List<Customer> {
        val customers =
            customerPagingRepository.findAllByUpdatedAtGreaterThanEqual(Instant.EPOCH, PageRequest.of(0, 1000, Sort.by("updatedAt").ascending())
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

    fun getCustomerByUid(uid: String): Customer? = customerRepository.findByUid(uid)

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

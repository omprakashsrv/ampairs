package com.ampairs.ecom.service

import com.ampairs.ecom.domain.dto.CustomerAddressRequest
import com.ampairs.ecom.domain.model.CustomerAddress
import com.ampairs.ecom.exception.EcomOrderNotFoundException
import com.ampairs.ecom.repository.CustomerAddressRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomerAddressService(
    private val addressRepository: CustomerAddressRepository,
) {

    fun getAddresses(customerId: String): List<CustomerAddress> =
        addressRepository.findByCustomerId(customerId)

    @Transactional
    fun addAddress(customerId: String, request: CustomerAddressRequest): CustomerAddress {
        // Honor a client-provided uid (the app generates address ids locally and is the source of
        // truth). This makes create idempotent on the uid, so the local id matches at checkout and
        // a retried/duplicated push updates the same row instead of minting a divergent server uid.
        val existing = request.uid?.takeIf { it.isNotBlank() }
            ?.let { addressRepository.findByCustomerIdAndUid(customerId, it) }

        if (request.isDefault) {
            addressRepository.findByCustomerIdAndIsDefaultTrue(customerId)?.let {
                if (it.uid != existing?.uid) {
                    it.isDefault = false
                    addressRepository.save(it)
                }
            }
        }
        val address = existing ?: CustomerAddress().also { a ->
            a.customerId = customerId
            request.uid?.takeIf { it.isNotBlank() }?.let { a.uid = it }
        }
        address.label = request.label
        address.addressLine1 = request.addressLine1
        address.addressLine2 = request.addressLine2
        address.city = request.city
        address.state = request.state
        address.pinCode = request.pinCode
        address.country = request.country
        address.phone = request.phone
        address.isDefault = request.isDefault
        return addressRepository.save(address)
    }

    @Transactional
    fun updateAddress(customerId: String, addressId: String, request: CustomerAddressRequest): CustomerAddress {
        val address = addressRepository.findByCustomerIdAndUid(customerId, addressId)
            ?: throw EcomOrderNotFoundException("Address not found")
        if (address.customerId != customerId) throw AccessDeniedException("Access denied")

        if (request.isDefault && !address.isDefault) {
            addressRepository.findByCustomerIdAndIsDefaultTrue(customerId)?.let {
                it.isDefault = false
                addressRepository.save(it)
            }
        }
        address.label = request.label
        address.addressLine1 = request.addressLine1
        address.addressLine2 = request.addressLine2
        address.city = request.city
        address.state = request.state
        address.pinCode = request.pinCode
        address.country = request.country
        address.phone = request.phone
        address.isDefault = request.isDefault
        return addressRepository.save(address)
    }

    @Transactional
    fun deleteAddress(customerId: String, addressId: String) {
        val address = addressRepository.findByCustomerIdAndUid(customerId, addressId)
            ?: throw EcomOrderNotFoundException("Address not found")
        if (address.customerId != customerId) throw AccessDeniedException("Access denied")
        addressRepository.delete(address)
    }
}

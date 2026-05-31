package com.ampairs.ecom.service

import com.ampairs.ecom.domain.dto.CheckoutRequest
import com.ampairs.ecom.domain.enums.CartStatus
import com.ampairs.ecom.domain.model.CustomerAddress
import com.ampairs.ecom.domain.model.EcomOrder
import com.ampairs.ecom.domain.model.EcomOrderLineItem
import com.ampairs.ecom.domain.model.Storefront
import com.ampairs.ecom.exception.CartExpiredException
import com.ampairs.ecom.exception.EmptyCartException
import com.ampairs.ecom.kafka.EcomOrderKafkaProducer
import com.ampairs.ecom.repository.CustomerAddressRepository
import com.ampairs.ecom.repository.EcomCartRepository
import com.ampairs.ecom.repository.EcomOrderLineItemRepository
import com.ampairs.ecom.repository.EcomOrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class CheckoutService(
    private val cartRepository: EcomCartRepository,
    private val orderRepository: EcomOrderRepository,
    private val orderLineItemRepository: EcomOrderLineItemRepository,
    private val addressRepository: CustomerAddressRepository,
    private val orderKafkaProducer: EcomOrderKafkaProducer,
) {

    @Transactional
    fun checkout(
        sessionToken: String,
        request: CheckoutRequest,
        customerId: String,
        customerEmail: String,
        customerName: String,
        storefront: Storefront,
    ): EcomOrder {
        val cart = cartRepository.findBySessionToken(sessionToken)
            ?: throw CartExpiredException("Cart not found: $sessionToken")

        if (cart.cartItems.isEmpty()) throw EmptyCartException("Cannot checkout with an empty cart")

        val deliveryAddress = resolveDeliveryAddress(request, customerId)

        val order = EcomOrder()
        order.storefrontId = storefront.uid
        order.workspaceId = storefront.ownerId
        order.customerId = customerId
        order.customerName = customerName
        order.customerEmail = customerEmail
        order.placedAt = Instant.now()
        order.deliveryAddress = deliveryAddress
        order.notes = request.notes

        val cartItems = cart.cartItems
        order.subtotal = cartItems.fold(java.math.BigDecimal.ZERO) { acc, item ->
            acc + item.unitPrice.multiply(java.math.BigDecimal(item.quantity))
        }
        order.totalAmount = order.subtotal

        val savedOrder = orderRepository.save(order)

        val lineItems = cartItems.map { item ->
            val li = EcomOrderLineItem()
            li.ecomOrderId = savedOrder.uid
            li.listedProductId = item.listedProductId
            li.managementProductId = item.managementProductId
            li.productName = item.productName
            li.unitPrice = item.unitPrice
            li.quantityOrdered = item.quantity
            li.lineTotal = item.unitPrice.multiply(java.math.BigDecimal(item.quantity))
            li
        }
        orderLineItemRepository.saveAll(lineItems)

        if (request.saveAddress && request.deliveryAddress != null) {
            val addr = CustomerAddress()
            addr.customerId = customerId
            addr.addressLine1 = request.deliveryAddress.addressLine1
            addr.addressLine2 = request.deliveryAddress.addressLine2
            addr.city = request.deliveryAddress.city
            addr.state = request.deliveryAddress.state
            addr.pinCode = request.deliveryAddress.pinCode
            addr.country = request.deliveryAddress.country
            addr.phone = request.deliveryAddress.phone
            addressRepository.save(addr)
        }

        cart.status = CartStatus.CONVERTED
        cartRepository.save(cart)

        val orderWithItems = orderRepository.findByEcomOrderRef(savedOrder.ecomOrderRef)!!
        orderKafkaProducer.publishOrderPlaced(orderWithItems)

        return orderWithItems
    }

    private fun resolveDeliveryAddress(request: CheckoutRequest, customerId: String): Map<String, Any> {
        if (request.deliveryAddressId != null) {
            val saved = addressRepository.findByCustomerIdAndUid(customerId, request.deliveryAddressId)
            if (saved != null) {
                return mapOf(
                    "addressLine1" to saved.addressLine1,
                    "addressLine2" to (saved.addressLine2 ?: ""),
                    "city" to saved.city,
                    "state" to saved.state,
                    "pinCode" to saved.pinCode,
                    "country" to saved.country,
                    "phone" to (saved.phone ?: ""),
                )
            }
        }
        val dto = request.deliveryAddress!!
        return mapOf(
            "addressLine1" to dto.addressLine1,
            "addressLine2" to (dto.addressLine2 ?: ""),
            "city" to dto.city,
            "state" to dto.state,
            "pinCode" to dto.pinCode,
            "country" to dto.country,
            "phone" to (dto.phone ?: ""),
        )
    }
}

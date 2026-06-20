package com.ampairs.ecom.service

import com.ampairs.ecom.domain.dto.CheckoutRequest
import com.ampairs.ecom.domain.enums.CartStatus
import com.ampairs.ecom.domain.enums.EcomOrderStatus
import com.ampairs.ecom.domain.model.CustomerAddress
import com.ampairs.ecom.domain.model.EcomOrder
import com.ampairs.ecom.domain.model.EcomOrderLineItem
import com.ampairs.ecom.domain.model.Storefront
import com.ampairs.ecom.exception.CartExpiredException
import com.ampairs.ecom.exception.EmptyCartException
import com.ampairs.ecom.event.EcomOrderEventPublisher
import com.ampairs.ecom.exception.InvalidDeliveryAddressException
import com.ampairs.ecom.repository.CustomerAddressRepository
import com.ampairs.ecom.repository.EcomCartRepository
import com.ampairs.ecom.repository.EcomOrderLineItemRepository
import com.ampairs.ecom.repository.EcomOrderRepository
import com.ampairs.core.config.Constants
import com.ampairs.core.utils.Helper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class CheckoutService(
    private val cartRepository: EcomCartRepository,
    private val orderRepository: EcomOrderRepository,
    private val orderLineItemRepository: EcomOrderLineItemRepository,
    private val addressRepository: CustomerAddressRepository,
    private val orderEventPublisher: EcomOrderEventPublisher,
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
        // Unique business reference for the order (the column is unique + non-null). Generated here
        // because BaseDomain.prePersist only fills uid — an unset ref inserts "" and collides.
        order.ecomOrderRef = Helper.generateUniqueId("ECO", Constants.ID_LENGTH)
        // Orders await merchant review. The default (PLACED) is a dead state — confirmOrder/
        // editLineItems require PENDING_MERCHANT_REVIEW and advanceStatus has no transition out of
        // PLACED, so a PLACED order can never progress.
        order.status = EcomOrderStatus.PENDING_MERCHANT_REVIEW
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

        val orderWithItems = orderRepository.findByEcomOrderRef(savedOrder.ecomOrderRef) ?: savedOrder
        orderEventPublisher.publishOrderPlaced(orderWithItems)

        return orderWithItems
    }

    private fun resolveDeliveryAddress(request: CheckoutRequest, customerId: String): Map<String, Any> {
        request.deliveryAddressId?.let { addressId ->
            // The address id is client-generated and authoritative; a saved address MUST exist for
            // it. Missing means the address was never pushed/synced — fail with a clear 400 rather
            // than NPE'ing on the (absent) inline address fallback below.
            val saved = addressRepository.findByCustomerIdAndUid(customerId, addressId)
                ?: throw InvalidDeliveryAddressException("Delivery address not found: $addressId")
            return saved.toAddressMap()
        }
        val dto = request.deliveryAddress
            ?: throw InvalidDeliveryAddressException("A delivery address is required to place the order")
        return buildMap {
            put("addressLine1", dto.addressLine1)
            put("addressLine2", dto.addressLine2 ?: "")
            put("city", dto.city)
            put("state", dto.state)
            put("pinCode", dto.pinCode)
            put("country", dto.country)
            put("phone", dto.phone ?: "")
            dto.latitude?.let { put("latitude", it) }
            dto.longitude?.let { put("longitude", it) }
        }
    }

    private fun CustomerAddress.toAddressMap(): Map<String, Any> = buildMap {
        put("addressLine1", addressLine1)
        put("addressLine2", addressLine2 ?: "")
        put("city", city)
        put("state", state)
        put("pinCode", pinCode)
        put("country", country)
        put("phone", phone ?: "")
        latitude?.let { put("latitude", it) }
        longitude?.let { put("longitude", it) }
    }
}

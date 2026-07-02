package com.ampairs.ecom.service

import com.ampairs.ecom.domain.dto.CheckoutRequest
import com.ampairs.ecom.domain.enums.CartStatus
import com.ampairs.ecom.domain.enums.EcomOrderStatus
import com.ampairs.ecom.domain.model.CustomerAddress
import com.ampairs.ecom.domain.model.EcomOrder
import com.ampairs.ecom.domain.model.EcomOrderLineItem
import com.ampairs.ecom.domain.model.Storefront
import com.ampairs.core.service.EcomCustomerService
import com.ampairs.ecom.exception.CartExpiredException
import com.ampairs.ecom.exception.EcomNotLinkedException
import com.ampairs.ecom.exception.EmptyCartException
import com.ampairs.ecom.event.EcomOrderEventPublisher
import com.ampairs.ecom.exception.InvalidDeliveryAddressException
import com.ampairs.ecom.repository.CustomerAddressRepository
import com.ampairs.ecom.repository.EcomCartRepository
import com.ampairs.ecom.repository.EcomOrderLineItemRepository
import com.ampairs.ecom.repository.EcomOrderRepository
import com.ampairs.core.config.Constants
import com.ampairs.core.domain.dto.MoneyDto
import com.ampairs.core.utils.Helper
import com.ampairs.pricing.domain.dto.CartLineInput
import com.ampairs.pricing.domain.dto.OfferApplicationRequest
import com.ampairs.pricing.service.OfferApplicationService
import com.ampairs.pricing.util.PricingJson
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

@Service
class CheckoutService(
    private val cartRepository: EcomCartRepository,
    private val orderRepository: EcomOrderRepository,
    private val orderLineItemRepository: EcomOrderLineItemRepository,
    private val addressRepository: CustomerAddressRepository,
    private val orderEventPublisher: EcomOrderEventPublisher,
    private val offerApplicationService: OfferApplicationService,
    private val ecomCustomerService: EcomCustomerService,
) {

    private val pricingCurrency = "INR"

    @Transactional
    fun checkout(
        sessionToken: String,
        request: CheckoutRequest,
        customerId: String,
        customerEmail: String,
        customerName: String,
        customerPhone: String?,
        storefront: Storefront,
    ): EcomOrder {
        // A storefront buyer may only order for a distributor the workspace owner has linked them to.
        // Resolve that CRM account up front — an unlinked buyer is blocked with a clear message rather
        // than silently creating an account. Tenant context is set by the controller.
        val resolvedCustomerId = ecomCustomerService.resolveLinkedCustomerId(
            ecomUserId = customerId,
            phone = customerPhone,
            name = customerName,
            email = customerEmail,
            requestedCustomerId = request.customerId,
        ) ?: throw EcomNotLinkedException(
            "Your account is not linked to any distributor. Please contact the business owner to get linked before placing an order."
        )

        val cart = cartRepository.findBySessionToken(sessionToken)
            ?: throw CartExpiredException("Cart not found: $sessionToken")

        // Idempotency: a cart is single-use. If it was already converted (double-tap / retried
        // request after a lost success response), return the order created the first time instead
        // of placing a duplicate.
        if (cart.status == CartStatus.CONVERTED) {
            orderRepository.findBySourceCartToken(sessionToken)?.let { return it }
            throw CartExpiredException("Cart already checked out: $sessionToken")
        }

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
        order.sourceCartToken = sessionToken
        order.storefrontId = storefront.uid
        order.workspaceId = storefront.ownerId
        order.customerId = customerId
        order.customerName = customerName
        order.customerEmail = customerEmail
        order.customerPhone = customerPhone ?: ""
        order.placedAt = Instant.now()
        order.deliveryAddress = deliveryAddress
        order.notes = request.notes

        val cartItems = cart.cartItems
        order.subtotal = cartItems.fold(java.math.BigDecimal.ZERO) { acc, item ->
            acc + item.unitPrice.multiply(java.math.BigDecimal(item.quantity))
        }

        // Apply eligible promotions server-side (single-sourced engine; the app mirrors it offline).
        // Lines are already price-resolved; the engine only computes the discount on top.
        val offerResult = offerApplicationService.apply(
            OfferApplicationRequest(
                channel = storefront.defaultChannel,
                customerId = customerId,
                pincode = order.deliveryAddress["pinCode"] as? String,
                currency = pricingCurrency,
                lines = cartItems.map { item ->
                    CartLineInput(
                        productId = item.managementProductId,
                        quantity = BigDecimal(item.quantity),
                        unitPriceMinor = item.resolvedUnitPriceMinor
                            ?: MoneyDto.of(item.unitPrice, pricingCurrency)?.amountMinor ?: 0L,
                    )
                },
            )
        )
        val discountMinor = offerResult.totalDiscount.amountMinor
        order.promotionDiscountMinor = discountMinor.takeIf { it > 0 }
        order.appliedPromotionsJson = offerResult.appliedOffers
            .takeIf { it.isNotEmpty() }
            ?.let { PricingJson.write(it) }
        order.totalAmount = (order.subtotal - BigDecimal.valueOf(discountMinor).movePointLeft(2)).max(BigDecimal.ZERO)

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
            // Carry the price-resolution snapshot from cart → order line (honored even if a list is later edited).
            li.resolvedUnitPriceMinor = item.resolvedUnitPriceMinor
            li.currency = item.currency
            li.priceSource = item.priceSource
            li.matchedPriceListUid = item.matchedPriceListUid
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
        // Carry the resolved (already-linked) CRM account so ingestion attaches the management order
        // to it directly — no re-resolution/auto-create on the order side.
        orderEventPublisher.publishOrderPlaced(orderWithItems, resolvedCustomerId)

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

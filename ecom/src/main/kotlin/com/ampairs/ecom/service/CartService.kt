package com.ampairs.ecom.service

import com.ampairs.ecom.config.Constants
import com.ampairs.ecom.domain.dto.CartItemRequest
import com.ampairs.ecom.domain.enums.CartStatus
import com.ampairs.ecom.domain.model.EcomCart
import com.ampairs.ecom.domain.model.EcomCartItem
import com.ampairs.ecom.exception.CartExpiredException
import com.ampairs.ecom.exception.InsufficientStockException
import com.ampairs.ecom.exception.ProductUnavailableException
import com.ampairs.ecom.repository.EcomCartItemRepository
import com.ampairs.ecom.repository.EcomCartRepository
import com.ampairs.ecom.repository.EcomListedProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class CartService(
    private val cartRepository: EcomCartRepository,
    private val cartItemRepository: EcomCartItemRepository,
    private val listedProductRepository: EcomListedProductRepository,
) {

    @Transactional
    fun createCart(storefrontId: String, customerId: String?): EcomCart {
        val cart = EcomCart()
        cart.storefrontId = storefrontId
        cart.customerId = customerId
        cart.sessionToken = UUID.randomUUID().toString()
        cart.expiresAt = if (customerId != null) {
            Instant.now().plus(Constants.AUTH_SESSION_TTL_DAYS, ChronoUnit.DAYS)
        } else {
            Instant.now().plus(Constants.CART_SESSION_TTL_HOURS, ChronoUnit.HOURS)
        }
        return cartRepository.save(cart)
    }

    @Transactional(readOnly = true)
    fun getCart(sessionToken: String): EcomCart {
        val cart = cartRepository.findBySessionToken(sessionToken)
            ?: throw CartExpiredException("Cart not found: $sessionToken")
        if (cart.status != CartStatus.ACTIVE || cart.expiresAt.isBefore(Instant.now())) {
            throw CartExpiredException("Cart has expired or is no longer active")
        }
        return cart
    }

    @Transactional
    fun addOrUpdateItem(sessionToken: String, request: CartItemRequest): EcomCart {
        val cart = getCart(sessionToken)
        val product = listedProductRepository.findByUid(request.listedProductId)
            ?: throw ProductUnavailableException("Product not found: ${request.listedProductId}")

        if (!product.isVisible) {
            throw ProductUnavailableException("Product is not available: ${product.name}")
        }
        if (request.quantity > product.stockQuantity) {
            throw InsufficientStockException(
                "Requested quantity ${request.quantity} exceeds available stock ${product.stockQuantity}",
                product.stockQuantity,
            )
        }

        val existing = cartItemRepository.findByCartIdAndListedProductId(cart.uid, product.uid)
        if (existing != null) {
            existing.quantity = request.quantity
            existing.unitPrice = product.price
            cartItemRepository.save(existing)
        } else {
            val item = EcomCartItem()
            item.cartId = cart.uid
            item.listedProductId = product.uid
            item.managementProductId = product.managementProductId
            item.productName = product.name
            item.unitPrice = product.price
            item.quantity = request.quantity
            item.primaryImageUrl = product.imageUrls.firstOrNull()
            cartItemRepository.save(item)
        }

        return cartRepository.findBySessionToken(sessionToken)!!
    }

    @Transactional
    fun removeItem(sessionToken: String, itemId: String): EcomCart {
        val cart = getCart(sessionToken)
        cartItemRepository.findByCartId(cart.uid).firstOrNull { it.uid == itemId }
            ?.let { cartItemRepository.delete(it) }
        return cartRepository.findBySessionToken(sessionToken)!!
    }

    @Transactional
    fun clearCart(sessionToken: String): EcomCart {
        val cart = getCart(sessionToken)
        cartItemRepository.deleteByCartId(cart.uid)
        return cartRepository.findBySessionToken(sessionToken)!!
    }

    @Transactional
    fun claimGuestCart(sessionToken: String, customerId: String, storefrontId: String): EcomCart {
        val guestCart = cartRepository.findBySessionToken(sessionToken)
            ?: throw CartExpiredException("Guest cart not found: $sessionToken")
        if (guestCart.status != CartStatus.ACTIVE || guestCart.expiresAt.isBefore(Instant.now())) {
            throw CartExpiredException("Guest cart has expired")
        }

        val customerCart = cartRepository.findByCustomerIdAndStorefrontIdAndStatus(customerId, storefrontId, CartStatus.ACTIVE)
            ?: run {
                val newCart = EcomCart()
                newCart.storefrontId = storefrontId
                newCart.customerId = customerId
                newCart.sessionToken = java.util.UUID.randomUUID().toString()
                newCart.expiresAt = Instant.now().plus(Constants.AUTH_SESSION_TTL_DAYS, java.time.temporal.ChronoUnit.DAYS)
                cartRepository.save(newCart)
            }

        val guestItems = cartItemRepository.findByCartId(guestCart.uid)
        val customerItems = cartItemRepository.findByCartId(customerCart.uid).associateBy { it.listedProductId }.toMutableMap()

        for (guestItem in guestItems) {
            val existing = customerItems[guestItem.listedProductId]
            val product = listedProductRepository.findByUid(guestItem.listedProductId)
            val maxQty = product?.stockQuantity ?: guestItem.quantity

            if (existing != null) {
                existing.quantity = minOf(existing.quantity + guestItem.quantity, maxQty)
                cartItemRepository.save(existing)
            } else {
                val newItem = EcomCartItem()
                newItem.cartId = customerCart.uid
                newItem.listedProductId = guestItem.listedProductId
                newItem.managementProductId = guestItem.managementProductId
                newItem.productName = guestItem.productName
                newItem.unitPrice = guestItem.unitPrice
                newItem.quantity = minOf(guestItem.quantity, maxQty)
                newItem.primaryImageUrl = guestItem.primaryImageUrl
                cartItemRepository.save(newItem)
            }
        }

        guestCart.status = CartStatus.MERGED
        cartRepository.save(guestCart)

        customerCart.expiresAt = Instant.now().plus(Constants.AUTH_SESSION_TTL_DAYS, java.time.temporal.ChronoUnit.DAYS)
        cartRepository.save(customerCart)

        return cartRepository.findBySessionToken(customerCart.sessionToken)!!
    }
}

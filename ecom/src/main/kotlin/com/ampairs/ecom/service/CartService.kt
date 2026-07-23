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
import com.ampairs.ecom.repository.StorefrontRepository
import com.ampairs.core.domain.dto.MoneyDto
import com.ampairs.core.domain.enums.SalesChannel
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.ecom.domain.model.EcomListedProduct
import com.ampairs.pricing.domain.dto.PriceResolutionRequest
import com.ampairs.pricing.service.PricingResolutionService
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class CartService(
    private val cartRepository: EcomCartRepository,
    private val cartItemRepository: EcomCartItemRepository,
    private val listedProductRepository: EcomListedProductRepository,
    private val storefrontRepository: StorefrontRepository,
    private val pricingResolutionService: PricingResolutionService,
) {

    private val pricingCurrency = "INR"

    /**
     * Resolve the effective unit price for a cart line via the pricing engine (in-process), honoring
     * the storefront's channel + the cart's customer. Falls back to the catalog price when no list
     * matches → identical to pre-009 behavior for unconfigured merchants.
     */
    private fun EcomCartItem.applyResolvedPrice(product: EcomListedProduct, quantity: Int, customerId: String?) {
        val channel = storefrontRepository.findByOwnerId(TenantContextHolder.getCurrentTenant() ?: "")?.defaultChannel
            ?: SalesChannel.RETAIL
        val resolution = pricingResolutionService.resolve(
            PriceResolutionRequest(
                channel = channel,
                productId = product.managementProductId,
                quantity = BigDecimal(quantity),
                customerId = customerId,
                fallbackUnitPriceMinor = MoneyDto.of(product.price, pricingCurrency)?.amountMinor,
                currency = pricingCurrency,
            )
        )
        unitPrice = MoneyDto(resolution.effectiveUnitPrice.amountMinor, resolution.effectiveUnitPrice.currency).toBigDecimal()
        resolvedUnitPriceMinor = resolution.effectiveUnitPrice.amountMinor
        currency = resolution.effectiveUnitPrice.currency
        priceSource = resolution.source.name
        matchedPriceListUid = resolution.matchedPriceListUid
    }

    @PersistenceContext
    private lateinit var entityManager: EntityManager

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

    /**
     * Write-path cart load: does NOT initialize the read-only `cartItems` collection, so item
     * mutations don't conflict with the cart's managed collection on autoflush.
     */
    private fun getCartForWrite(sessionToken: String): EcomCart {
        val cart = cartRepository.findFirstBySessionToken(sessionToken)
            ?: throw CartExpiredException("Cart not found: $sessionToken")
        if (cart.status != CartStatus.ACTIVE || cart.expiresAt.isBefore(Instant.now())) {
            throw CartExpiredException("Cart has expired or is no longer active")
        }
        return cart
    }

    /** Flush pending item changes and detach everything so the response read is clean. */
    private fun reloadCart(sessionToken: String): EcomCart {
        entityManager.flush()
        entityManager.clear()
        return cartRepository.findBySessionToken(sessionToken)
            ?: throw CartExpiredException("Cart not found after update: $sessionToken")
    }

    @Transactional
    fun addOrUpdateItem(sessionToken: String, request: CartItemRequest): EcomCart {
        val cart = getCartForWrite(sessionToken)
        val product = listedProductRepository.findByUid(request.listedProductId)
            ?: throw ProductUnavailableException("Product not found: ${request.listedProductId}")

        val existing = cartItemRepository.findByCartIdAndListedProductId(cart.uid, product.uid)

        // quantity 0 (or below) means "remove this line".
        if (request.quantity <= 0) {
            existing?.let { cartItemRepository.delete(it) }
            return reloadCart(sessionToken)
        }

        if (!product.isVisible) {
            throw ProductUnavailableException("Product is not available: ${product.name}")
        }
        if (request.quantity > product.stockQuantity) {
            throw InsufficientStockException(
                "Requested quantity ${request.quantity} exceeds available stock ${product.stockQuantity}",
                product.stockQuantity,
            )
        }

        if (existing != null) {
            existing.quantity = request.quantity
            existing.mrpAtAdd = product.mrp
            existing.applyResolvedPrice(product, request.quantity, cart.customerId)
            cartItemRepository.save(existing)
        } else {
            val item = EcomCartItem()
            item.cartId = cart.uid
            item.listedProductId = product.uid
            item.managementProductId = product.managementProductId
            item.productName = product.name
            item.brand = product.brand
            item.unit = product.unit
            item.mrpAtAdd = product.mrp
            item.quantity = request.quantity
            item.primaryImageUrl = product.imageUrls.firstOrNull()
            item.applyResolvedPrice(product, request.quantity, cart.customerId)
            cartItemRepository.save(item)
        }

        return reloadCart(sessionToken)
    }

    @Transactional
    fun removeItem(sessionToken: String, itemId: String): EcomCart {
        val cart = getCartForWrite(sessionToken)
        cartItemRepository.findByCartId(cart.uid).firstOrNull { it.uid == itemId }
            ?.let { cartItemRepository.delete(it) }
        return reloadCart(sessionToken)
    }

    @Transactional
    fun clearCart(sessionToken: String): EcomCart {
        val cart = getCartForWrite(sessionToken)
        cartItemRepository.deleteByCartId(cart.uid)
        return reloadCart(sessionToken)
    }

    @Transactional
    fun claimGuestCart(sessionToken: String, customerId: String, storefrontId: String): EcomCart {
        val guestCart = cartRepository.findFirstBySessionToken(sessionToken)
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
                newItem.brand = guestItem.brand
                newItem.unit = guestItem.unit
                newItem.unitPrice = guestItem.unitPrice
                newItem.mrpAtAdd = guestItem.mrpAtAdd
                newItem.quantity = minOf(guestItem.quantity, maxQty)
                newItem.primaryImageUrl = guestItem.primaryImageUrl
                cartItemRepository.save(newItem)
            }
        }

        guestCart.status = CartStatus.MERGED
        cartRepository.save(guestCart)

        customerCart.expiresAt = Instant.now().plus(Constants.AUTH_SESSION_TTL_DAYS, java.time.temporal.ChronoUnit.DAYS)
        cartRepository.save(customerCart)

        return reloadCart(customerCart.sessionToken)
    }
}

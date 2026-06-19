package com.ampairs.ecom.service

import com.ampairs.ecom.domain.dto.CartItemRequest
import com.ampairs.ecom.domain.enums.CartStatus
import com.ampairs.ecom.domain.model.EcomCart
import com.ampairs.ecom.domain.model.EcomCartItem
import com.ampairs.ecom.domain.model.EcomListedProduct
import com.ampairs.ecom.exception.CartExpiredException
import com.ampairs.ecom.exception.InsufficientStockException
import com.ampairs.ecom.exception.ProductUnavailableException
import com.ampairs.ecom.repository.EcomCartItemRepository
import com.ampairs.ecom.repository.EcomCartRepository
import com.ampairs.ecom.repository.EcomListedProductRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import jakarta.persistence.EntityManager
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(MockitoExtension::class)
class CartServiceTest {

    @Mock private lateinit var cartRepository: EcomCartRepository
    @Mock private lateinit var cartItemRepository: EcomCartItemRepository
    @Mock private lateinit var listedProductRepository: EcomListedProductRepository
    // Field-injected @PersistenceContext EntityManager (write paths flush+clear before re-reading).
    @Mock private lateinit var entityManager: EntityManager

    private lateinit var cartService: CartService

    @BeforeEach
    fun setup() {
        cartService = CartService(cartRepository, cartItemRepository, listedProductRepository)
        // @InjectMocks uses constructor injection only, so the @PersistenceContext field isn't set;
        // inject the EntityManager mock explicitly.
        ReflectionTestUtils.setField(cartService, "entityManager", entityManager)
    }

    // ── createCart ────────────────────────────────────────────────────────────

    @Test
    fun `createCart for guest sets 24h TTL`() {
        val saved = makeCart("c1")
        whenever(cartRepository.save(any<EcomCart>())).thenReturn(saved)

        val captor = argumentCaptor<EcomCart>()
        cartService.createCart("sf1", null)
        verify(cartRepository).save(captor.capture())

        val ttlHours = ChronoUnit.HOURS.between(Instant.now(), captor.firstValue.expiresAt)
        assertEquals(23L, ttlHours) // between 23 and 24 depending on execution timing
    }

    @Test
    fun `createCart for authenticated customer sets 30d TTL`() {
        val saved = makeCart("c1")
        whenever(cartRepository.save(any<EcomCart>())).thenReturn(saved)

        val captor = argumentCaptor<EcomCart>()
        cartService.createCart("sf1", "cust-1")
        verify(cartRepository).save(captor.capture())

        val ttlDays = ChronoUnit.DAYS.between(Instant.now(), captor.firstValue.expiresAt)
        assertEquals(29L, ttlDays)
    }

    // ── getCart ───────────────────────────────────────────────────────────────

    @Test
    fun `getCart returns active cart`() {
        val cart = makeCart("c1")
        whenever(cartRepository.findBySessionToken("tok")).thenReturn(cart)

        val result = cartService.getCart("tok")
        assertEquals("c1", result.uid)
    }

    @Test
    fun `getCart throws CartExpiredException when not found`() {
        whenever(cartRepository.findBySessionToken("missing")).thenReturn(null)
        assertThrows<CartExpiredException> { cartService.getCart("missing") }
    }

    @Test
    fun `getCart throws CartExpiredException when cart is expired`() {
        val cart = makeCart("c1").apply {
            expiresAt = Instant.now().minusSeconds(3600)
        }
        whenever(cartRepository.findBySessionToken("tok")).thenReturn(cart)
        assertThrows<CartExpiredException> { cartService.getCart("tok") }
    }

    @Test
    fun `getCart throws CartExpiredException when status is not ACTIVE`() {
        val cart = makeCart("c1").apply { status = CartStatus.CONVERTED }
        whenever(cartRepository.findBySessionToken("tok")).thenReturn(cart)
        assertThrows<CartExpiredException> { cartService.getCart("tok") }
    }

    // ── addOrUpdateItem ───────────────────────────────────────────────────────

    @Test
    fun `addOrUpdateItem adds new item when not present`() {
        val cart = makeCart("c1")
        val product = makeProduct("p1", stock = 10)
        val updatedCart = makeCart("c1")
        val request = CartItemRequest(listedProductId = "p1", quantity = 2)

        whenever(cartRepository.findFirstBySessionToken("tok")).thenReturn(cart)
        whenever(listedProductRepository.findByUid("p1")).thenReturn(product)
        whenever(cartItemRepository.findByCartIdAndListedProductId("c1", "p1")).thenReturn(null)
        whenever(cartItemRepository.save(any<EcomCartItem>())).thenReturn(makeCartItem("i1", "c1", "p1"))
        whenever(cartRepository.findBySessionToken("tok")).thenReturn(updatedCart)

        val result = cartService.addOrUpdateItem("tok", request)
        verify(cartItemRepository).save(any<EcomCartItem>())
        assertNotNull(result)
    }

    @Test
    fun `addOrUpdateItem updates quantity when item already in cart`() {
        val cart = makeCart("c1")
        val product = makeProduct("p1", stock = 10)
        val existingItem = makeCartItem("i1", "c1", "p1").apply { quantity = 1 }
        val request = CartItemRequest(listedProductId = "p1", quantity = 5)

        whenever(cartRepository.findFirstBySessionToken("tok")).thenReturn(cart)
        whenever(listedProductRepository.findByUid("p1")).thenReturn(product)
        whenever(cartItemRepository.findByCartIdAndListedProductId("c1", "p1")).thenReturn(existingItem)
        whenever(cartItemRepository.save(existingItem)).thenReturn(existingItem)
        whenever(cartRepository.findBySessionToken("tok")).thenReturn(cart)

        cartService.addOrUpdateItem("tok", request)
        assertEquals(5, existingItem.quantity)
        verify(cartItemRepository).save(existingItem)
    }

    @Test
    fun `addOrUpdateItem throws ProductUnavailableException when product missing`() {
        val cart = makeCart("c1")
        whenever(cartRepository.findFirstBySessionToken("tok")).thenReturn(cart)
        whenever(listedProductRepository.findByUid("missing")).thenReturn(null)

        assertThrows<ProductUnavailableException> {
            cartService.addOrUpdateItem("tok", CartItemRequest("missing", 1))
        }
        verify(cartItemRepository, never()).save(any<EcomCartItem>())
    }

    @Test
    fun `addOrUpdateItem throws ProductUnavailableException when product not visible`() {
        val cart = makeCart("c1")
        val product = makeProduct("p1", stock = 10).apply { isVisible = false }
        whenever(cartRepository.findFirstBySessionToken("tok")).thenReturn(cart)
        whenever(listedProductRepository.findByUid("p1")).thenReturn(product)

        assertThrows<ProductUnavailableException> {
            cartService.addOrUpdateItem("tok", CartItemRequest("p1", 1))
        }
    }

    @Test
    fun `addOrUpdateItem throws InsufficientStockException when quantity exceeds stock`() {
        val cart = makeCart("c1")
        val product = makeProduct("p1", stock = 3)
        whenever(cartRepository.findFirstBySessionToken("tok")).thenReturn(cart)
        whenever(listedProductRepository.findByUid("p1")).thenReturn(product)

        assertThrows<InsufficientStockException> {
            cartService.addOrUpdateItem("tok", CartItemRequest("p1", 10))
        }
    }

    // ── removeItem ────────────────────────────────────────────────────────────

    @Test
    fun `removeItem deletes matching item from cart`() {
        val cart = makeCart("c1")
        val item = makeCartItem("i1", "c1", "p1")
        cart.cartItems.add(item)

        whenever(cartRepository.findFirstBySessionToken("tok")).thenReturn(cart)
        whenever(cartRepository.findBySessionToken("tok")).thenReturn(cart)
        whenever(cartItemRepository.findByCartId("c1")).thenReturn(mutableListOf(item))

        cartService.removeItem("tok", "i1")
        verify(cartItemRepository).delete(item)
    }

    @Test
    fun `removeItem is no-op when item id not found`() {
        val cart = makeCart("c1")
        val item = makeCartItem("i1", "c1", "p1")

        whenever(cartRepository.findFirstBySessionToken("tok")).thenReturn(cart)
        whenever(cartRepository.findBySessionToken("tok")).thenReturn(cart)
        whenever(cartItemRepository.findByCartId("c1")).thenReturn(mutableListOf(item))

        cartService.removeItem("tok", "non-existent")
        verify(cartItemRepository, never()).delete(any())
    }

    // ── clearCart ─────────────────────────────────────────────────────────────

    @Test
    fun `clearCart deletes all items and returns cart`() {
        val cart = makeCart("c1")
        whenever(cartRepository.findFirstBySessionToken("tok")).thenReturn(cart)
        whenever(cartRepository.findBySessionToken("tok")).thenReturn(cart)

        cartService.clearCart("tok")
        verify(cartItemRepository).deleteByCartId("c1")
    }

    // ── claimGuestCart ────────────────────────────────────────────────────────

    @Test
    fun `claimGuestCart throws CartExpiredException when guest cart not found`() {
        whenever(cartRepository.findFirstBySessionToken("guest-tok")).thenReturn(null)
        assertThrows<CartExpiredException> {
            cartService.claimGuestCart("guest-tok", "cust-1", "sf1")
        }
    }

    @Test
    fun `claimGuestCart throws CartExpiredException when guest cart expired`() {
        val expired = makeCart("guest").apply {
            expiresAt = Instant.now().minusSeconds(3600)
        }
        whenever(cartRepository.findFirstBySessionToken("guest-tok")).thenReturn(expired)
        assertThrows<CartExpiredException> {
            cartService.claimGuestCart("guest-tok", "cust-1", "sf1")
        }
    }

    @Test
    fun `claimGuestCart merges guest items into existing customer cart`() {
        val guestCart = makeCart("guest-c")
        val guestItem = makeCartItem("gi1", "guest-c", "p1").apply { quantity = 2 }
        guestCart.cartItems.add(guestItem)

        val customerCart = makeCart("cust-c").apply { sessionToken = "cust-tok" }

        whenever(cartRepository.findFirstBySessionToken("guest-tok")).thenReturn(guestCart)
        whenever(cartRepository.findByCustomerIdAndStorefrontIdAndStatus("cust-1", "sf1", CartStatus.ACTIVE))
            .thenReturn(customerCart)
        whenever(cartItemRepository.findByCartId("guest-c")).thenReturn(mutableListOf(guestItem))
        whenever(cartItemRepository.findByCartId("cust-c")).thenReturn(mutableListOf())
        whenever(listedProductRepository.findByUid("p1")).thenReturn(makeProduct("p1", stock = 10))
        whenever(cartRepository.save(any<EcomCart>())).thenAnswer { it.arguments[0] as EcomCart }
        whenever(cartItemRepository.save(any<EcomCartItem>())).thenAnswer { it.arguments[0] as EcomCartItem }
        whenever(cartRepository.findBySessionToken("cust-tok")).thenReturn(customerCart)

        val result = cartService.claimGuestCart("guest-tok", "cust-1", "sf1")
        assertNotNull(result)
        assertEquals(CartStatus.MERGED, guestCart.status)
    }

    @Test
    fun `claimGuestCart creates new customer cart when none exists`() {
        val guestCart = makeCart("guest-c")
        val newCustomerCart = makeCart("new-cust-c").apply { sessionToken = "new-tok" }

        whenever(cartRepository.findFirstBySessionToken("guest-tok")).thenReturn(guestCart)
        whenever(cartRepository.findByCustomerIdAndStorefrontIdAndStatus("cust-1", "sf1", CartStatus.ACTIVE))
            .thenReturn(null)
        whenever(cartRepository.save(any<EcomCart>())).thenReturn(newCustomerCart)
        whenever(cartItemRepository.findByCartId("guest-c")).thenReturn(mutableListOf())
        whenever(cartItemRepository.findByCartId("new-cust-c")).thenReturn(mutableListOf())
        whenever(cartRepository.findBySessionToken("new-tok")).thenReturn(newCustomerCart)

        val result = cartService.claimGuestCart("guest-tok", "cust-1", "sf1")
        assertNotNull(result)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun makeCart(uid: String) = EcomCart().apply {
        this.uid = uid
        sessionToken = "tok"
        storefrontId = "sf1"
        status = CartStatus.ACTIVE
        expiresAt = Instant.now().plus(24, ChronoUnit.HOURS)
    }

    private fun makeProduct(uid: String, stock: Int) = EcomListedProduct().apply {
        this.uid = uid
        managementProductId = "mgmt-$uid"
        name = "Product $uid"
        price = BigDecimal("100.00")
        stockQuantity = stock
        isVisible = true
        imageUrls = listOf("https://cdn.example.com/$uid.jpg")
    }

    private fun makeCartItem(uid: String, cartId: String, productId: String) = EcomCartItem().apply {
        this.uid = uid
        this.cartId = cartId
        listedProductId = productId
        managementProductId = "mgmt-$productId"
        productName = "Product $productId"
        unitPrice = BigDecimal("100.00")
        quantity = 1
    }
}

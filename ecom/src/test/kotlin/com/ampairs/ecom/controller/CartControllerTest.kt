package com.ampairs.ecom.controller

import com.ampairs.AmpairsApplication
import com.ampairs.ecom.domain.dto.CartItemRequest
import com.ampairs.ecom.domain.dto.CartResponse
import com.ampairs.ecom.domain.dto.asCartResponse
import com.ampairs.ecom.domain.enums.CartStatus
import com.ampairs.ecom.domain.enums.StorefrontStatus
import com.ampairs.ecom.domain.model.EcomCart
import com.ampairs.ecom.domain.model.Storefront
import com.ampairs.ecom.exception.CartExpiredException
import com.ampairs.ecom.exception.InsufficientStockException
import com.ampairs.ecom.exception.ProductUnavailableException
import com.ampairs.ecom.exception.StorefrontNotFoundException
import com.ampairs.ecom.kafka.EcomCatalogKafkaConsumer
import com.ampairs.ecom.kafka.EcomOrderKafkaProducer
import com.ampairs.ecom.kafka.EcomOrderStatusKafkaConsumer
import com.ampairs.ecom.service.CartService
import com.ampairs.ecom.service.StorefrontService
import com.ampairs.workspace.service.WorkspaceMemberService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

@SpringBootTest(classes = [AmpairsApplication::class])
@ActiveProfiles("test")
@Transactional
class CartControllerTest {

    @Autowired private lateinit var webApplicationContext: WebApplicationContext
    @Autowired private lateinit var objectMapper: ObjectMapper

    private lateinit var mockMvc: MockMvc

    @field:MockitoBean private lateinit var cartService: CartService
    @field:MockitoBean private lateinit var storefrontService: StorefrontService
    @field:MockitoBean private lateinit var workspaceMemberService: WorkspaceMemberService
    @field:MockitoBean private lateinit var ecomOrderKafkaProducer: EcomOrderKafkaProducer
    @field:MockitoBean private lateinit var ecomCatalogKafkaConsumer: EcomCatalogKafkaConsumer
    @field:MockitoBean private lateinit var ecomOrderStatusKafkaConsumer: EcomOrderStatusKafkaConsumer

    @BeforeEach
    fun setUp() {
        whenever(workspaceMemberService.isWorkspaceMember(any())).thenReturn(true)
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

    // ── POST /api/v1/store/{slug}/cart ────────────────────────────────────────

    @Test
    @DisplayName("POST /store/{slug}/cart - creates cart (no customer id)")
    @WithMockUser(roles = ["USER"])
    fun `should create cart without customer id`() {
        val storefront = makeStorefront("sf-1", "ws-1", "my-shop")
        val cart = makeCart("c-1", storefrontId = "sf-1")
        whenever(storefrontService.getPublishedStorefrontBySlug("my-shop")).thenReturn(storefront)
        whenever(cartService.createCart(eq("sf-1"), any())).thenReturn(cart)

        mockMvc.perform(post("/v1/store/my-shop/cart"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.session_token").value("tok-1"))
    }

    @Test
    @DisplayName("POST /store/{slug}/cart - creates authenticated cart with customer id")
    @WithMockUser(username = "cust-1", roles = ["USER"])
    fun `should create authenticated cart with customer id`() {
        val storefront = makeStorefront("sf-1", "ws-1", "my-shop")
        val cart = makeCart("c-1", storefrontId = "sf-1", customerId = "cust-1")
        whenever(storefrontService.getPublishedStorefrontBySlug("my-shop")).thenReturn(storefront)
        whenever(cartService.createCart(eq("sf-1"), any())).thenReturn(cart)

        mockMvc.perform(post("/v1/store/my-shop/cart"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    @DisplayName("POST /store/{slug}/cart - returns 404 when storefront not found")
    @WithMockUser(roles = ["USER"])
    fun `should return 404 when storefront slug not found`() {
        whenever(storefrontService.getPublishedStorefrontBySlug("unknown-slug"))
            .thenThrow(StorefrontNotFoundException("No published storefront with slug 'unknown-slug'"))

        mockMvc.perform(post("/v1/store/unknown-slug/cart"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
    }

    // ── GET /api/v1/store/{slug}/cart/{sessionToken} ──────────────────────────

    @Test
    @DisplayName("GET /store/{slug}/cart/{token} - returns active cart")
    @WithMockUser(roles = ["USER"])
    fun `should get active cart by session token`() {
        val storefront = makeStorefront("sf-1", "ws-1", "my-shop")
        val cart = makeCart("c-1", storefrontId = "sf-1")
        whenever(storefrontService.getPublishedStorefrontBySlug("my-shop")).thenReturn(storefront)
        whenever(cartService.getCart("tok-1")).thenReturn(cart)

        mockMvc.perform(get("/v1/store/my-shop/cart/tok-1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.uid").value("c-1"))
    }

    @Test
    @DisplayName("GET /store/{slug}/cart/{token} - returns 404 when cart expired")
    @WithMockUser(roles = ["USER"])
    fun `should return 404 when cart is expired`() {
        val storefront = makeStorefront("sf-1", "ws-1", "my-shop")
        whenever(storefrontService.getPublishedStorefrontBySlug("my-shop")).thenReturn(storefront)
        whenever(cartService.getCart("expired-tok"))
            .thenThrow(CartExpiredException("Cart has expired"))

        mockMvc.perform(get("/v1/store/my-shop/cart/expired-tok"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
    }

    // ── POST /api/v1/store/{slug}/cart/{token}/items ──────────────────────────

    @Test
    @DisplayName("POST /cart/{token}/items - adds item to cart")
    @WithMockUser(roles = ["USER"])
    fun `should add item to cart`() {
        val storefront = makeStorefront("sf-1", "ws-1", "my-shop")
        val cart = makeCart("c-1", storefrontId = "sf-1")
        whenever(storefrontService.getPublishedStorefrontBySlug("my-shop")).thenReturn(storefront)
        whenever(cartService.addOrUpdateItem(eq("tok-1"), any())).thenReturn(cart)

        val request = CartItemRequest(listedProductId = "lp-1", quantity = 2)
        mockMvc.perform(
            post("/v1/store/my-shop/cart/tok-1/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    @DisplayName("POST /cart/{token}/items - returns 422 on insufficient stock")
    @WithMockUser(roles = ["USER"])
    fun `should return 422 when stock is insufficient`() {
        val storefront = makeStorefront("sf-1", "ws-1", "my-shop")
        whenever(storefrontService.getPublishedStorefrontBySlug("my-shop")).thenReturn(storefront)
        whenever(cartService.addOrUpdateItem(eq("tok-1"), any()))
            .thenThrow(InsufficientStockException("Not enough stock", availableQuantity = 3))

        val request = CartItemRequest(listedProductId = "lp-1", quantity = 50)
        mockMvc.perform(
            post("/v1/store/my-shop/cart/tok-1/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    @DisplayName("POST /cart/{token}/items - returns 409 when product unavailable")
    @WithMockUser(roles = ["USER"])
    fun `should return 409 when product is unavailable`() {
        val storefront = makeStorefront("sf-1", "ws-1", "my-shop")
        whenever(storefrontService.getPublishedStorefrontBySlug("my-shop")).thenReturn(storefront)
        whenever(cartService.addOrUpdateItem(eq("tok-1"), any()))
            .thenThrow(ProductUnavailableException("Product is not available"))

        val request = CartItemRequest(listedProductId = "lp-hidden", quantity = 1)
        mockMvc.perform(
            post("/v1/store/my-shop/cart/tok-1/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    @DisplayName("POST /cart/{token}/items - validates quantity >= 0")
    @WithMockUser(roles = ["USER"])
    fun `should return 400 for negative quantity`() {
        val storefront = makeStorefront("sf-1", "ws-1", "my-shop")
        whenever(storefrontService.getPublishedStorefrontBySlug("my-shop")).thenReturn(storefront)

        // quantity 0 is valid now (means "remove the line"); only negative is rejected.
        mockMvc.perform(
            post("/v1/store/my-shop/cart/tok-1/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"listed_product_id":"lp-1","quantity":-1}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
    }

    // ── DELETE /api/v1/store/{slug}/cart/{token}/items/{itemId} ───────────────

    @Test
    @DisplayName("DELETE /cart/{token}/items/{itemId} - removes item from cart")
    @WithMockUser(roles = ["USER"])
    fun `should remove item from cart`() {
        val storefront = makeStorefront("sf-1", "ws-1", "my-shop")
        val cart = makeCart("c-1", storefrontId = "sf-1")
        whenever(storefrontService.getPublishedStorefrontBySlug("my-shop")).thenReturn(storefront)
        whenever(cartService.removeItem("tok-1", "item-1")).thenReturn(cart)

        mockMvc.perform(delete("/v1/store/my-shop/cart/tok-1/items/item-1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    // ── DELETE /api/v1/store/{slug}/cart/{token} ──────────────────────────────

    @Test
    @DisplayName("DELETE /cart/{token} - clears all items from cart")
    @WithMockUser(roles = ["USER"])
    fun `should clear cart`() {
        val storefront = makeStorefront("sf-1", "ws-1", "my-shop")
        val emptyCart = makeCart("c-1", storefrontId = "sf-1")
        whenever(storefrontService.getPublishedStorefrontBySlug("my-shop")).thenReturn(storefront)
        whenever(cartService.clearCart("tok-1")).thenReturn(emptyCart)

        mockMvc.perform(delete("/v1/store/my-shop/cart/tok-1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items").isArray)
    }

    // ── POST /api/v1/store/{slug}/cart/{token}/claim ──────────────────────────

    @Test
    @DisplayName("POST /cart/{token}/claim - claims guest cart when authenticated")
    @WithMockUser(username = "cust-1", roles = ["USER"])
    fun `should claim guest cart for authenticated user`() {
        val storefront = makeStorefront("sf-1", "ws-1", "my-shop")
        val merged = makeCart("c-2", storefrontId = "sf-1", customerId = "cust-1")
        whenever(storefrontService.getPublishedStorefrontBySlug("my-shop")).thenReturn(storefront)
        whenever(cartService.claimGuestCart(eq("guest-tok"), any(), eq("sf-1"))).thenReturn(merged)

        mockMvc.perform(post("/v1/store/my-shop/cart/guest-tok/claim"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    @DisplayName("POST /cart/{token}/claim - requires authentication")
    fun `should reject unauthenticated cart claim`() {
        mockMvc.perform(post("/v1/store/my-shop/cart/guest-tok/claim"))
            .andExpect(status().isUnauthorized)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun makeStorefront(uid: String, ownerId: String, slug: String) = Storefront().apply {
        this.uid = uid
        this.ownerId = ownerId
        this.slug = slug
        name = "Shop $uid"
        status = StorefrontStatus.PUBLISHED
    }

    private fun makeCart(uid: String, storefrontId: String, customerId: String? = null) = EcomCart().apply {
        this.uid = uid
        this.storefrontId = storefrontId
        this.customerId = customerId
        sessionToken = "tok-1"
        status = CartStatus.ACTIVE
        expiresAt = Instant.now().plus(24, ChronoUnit.HOURS)
    }
}

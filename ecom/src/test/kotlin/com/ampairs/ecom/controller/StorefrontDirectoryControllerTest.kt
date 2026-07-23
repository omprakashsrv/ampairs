package com.ampairs.ecom.controller

import com.ampairs.AmpairsApplication
import com.ampairs.ecom.domain.enums.StorefrontAccessMode
import com.ampairs.ecom.domain.enums.StorefrontStatus
import com.ampairs.ecom.domain.model.Storefront
import com.ampairs.ecom.kafka.EcomCatalogKafkaConsumer
import com.ampairs.ecom.kafka.EcomOrderKafkaProducer
import com.ampairs.ecom.kafka.EcomOrderStatusKafkaConsumer
import com.ampairs.ecom.repository.StorefrontRepository
import com.ampairs.workspace.service.WorkspaceMemberService
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext

/**
 * Integration test for the public storefront directory (GET /api/v1/storefronts).
 *
 * Uses the real [StorefrontService] + [StorefrontRepository] against the in-memory H2
 * database (ddl-auto create-drop), so the native cross-tenant query is exercised for real.
 * The endpoint is public — none of these requests carry auth or an X-Workspace-ID header.
 */
@SpringBootTest(classes = [AmpairsApplication::class])
@ActiveProfiles("test")
@Transactional
class StorefrontDirectoryControllerTest {

    @Autowired private lateinit var webApplicationContext: WebApplicationContext
    @Autowired private lateinit var storefrontRepository: StorefrontRepository
    @Autowired private lateinit var entityManager: EntityManager

    private lateinit var mockMvc: MockMvc

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

    // ── PUBLISHED-only filtering ──────────────────────────────────────────────

    @Test
    @DisplayName("GET /storefronts - returns only PUBLISHED storefronts, excludes DRAFT/UNPUBLISHED")
    fun `should return only published storefronts`() {
        persist("alpha-shop", "Alpha Shop", StorefrontStatus.PUBLISHED)
        persist("beta-shop", "Beta Shop", StorefrontStatus.PUBLISHED)
        persist("draft-shop", "Draft Shop", StorefrontStatus.DRAFT)
        persist("gone-shop", "Gone Shop", StorefrontStatus.UNPUBLISHED)

        mockMvc.perform(get("/v1/storefronts"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.total_elements").value(2))
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.content[0].slug").value("alpha-shop")) // ORDER BY name ASC
            .andExpect(jsonPath("$.data.content[1].slug").value("beta-shop"))
            .andExpect(jsonPath("$.data.content[0].status").value("PUBLISHED"))
    }

    // ── q filter: name + slug, case-insensitive ───────────────────────────────

    @Test
    @DisplayName("GET /storefronts?q= - filters by name, case-insensitive")
    fun `should filter by name case insensitive`() {
        persist("acme-tools", "Acme Tools", StorefrontStatus.PUBLISHED)
        persist("green-grocer", "Green Grocer", StorefrontStatus.PUBLISHED)

        mockMvc.perform(get("/v1/storefronts").param("q", "acme"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.total_elements").value(1))
            .andExpect(jsonPath("$.data.content[0].slug").value("acme-tools"))
    }

    @Test
    @DisplayName("GET /storefronts?q= - filters by slug, case-insensitive")
    fun `should filter by slug case insensitive`() {
        persist("acme-tools", "Widgets Inc", StorefrontStatus.PUBLISHED)
        persist("green-grocer", "Green Grocer", StorefrontStatus.PUBLISHED)

        // Matches the SLUG (acme-tools) even though the name is "Widgets Inc", and uppercased query.
        mockMvc.perform(get("/v1/storefronts").param("q", "ACME"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.total_elements").value(1))
            .andExpect(jsonPath("$.data.content[0].slug").value("acme-tools"))
    }

    @Test
    @DisplayName("GET /storefronts - blank/absent q returns all published")
    fun `should return all published when q blank`() {
        persist("a-shop", "A Shop", StorefrontStatus.PUBLISHED)
        persist("b-shop", "B Shop", StorefrontStatus.PUBLISHED)

        mockMvc.perform(get("/v1/storefronts").param("q", "   "))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.total_elements").value(2))
    }

    // ── pagination envelope ───────────────────────────────────────────────────

    @Test
    @DisplayName("GET /storefronts - pagination envelope fields are correct")
    fun `should expose correct pagination fields`() {
        persist("shop-a", "Shop A", StorefrontStatus.PUBLISHED)
        persist("shop-b", "Shop B", StorefrontStatus.PUBLISHED)
        persist("shop-c", "Shop C", StorefrontStatus.PUBLISHED)

        // First page of size 2.
        mockMvc.perform(get("/v1/storefronts").param("page", "0").param("size", "2"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(2))
            .andExpect(jsonPath("$.data.total_elements").value(3))
            .andExpect(jsonPath("$.data.total_pages").value(2))
            .andExpect(jsonPath("$.data.first").value(true))
            .andExpect(jsonPath("$.data.last").value(false))
            .andExpect(jsonPath("$.data.content.length()").value(2))

        // Last page.
        mockMvc.perform(get("/v1/storefronts").param("page", "1").param("size", "2"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.page").value(1))
            .andExpect(jsonPath("$.data.first").value(false))
            .andExpect(jsonPath("$.data.last").value(true))
            .andExpect(jsonPath("$.data.content.length()").value(1))
    }

    @Test
    @DisplayName("GET /storefronts - size is capped at 50")
    fun `should cap page size at 50`() {
        persist("only-shop", "Only Shop", StorefrontStatus.PUBLISHED)

        mockMvc.perform(get("/v1/storefronts").param("size", "5000"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.size").value(50))
    }

    // ── access_mode ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /storefronts - exposes access_mode so clients can badge restricted stores")
    fun `should expose access mode`() {
        persist("open-shop", "Open Shop", StorefrontStatus.PUBLISHED, accessMode = StorefrontAccessMode.PUBLIC)
        persist("vip-shop", "VIP Shop", StorefrontStatus.PUBLISHED, accessMode = StorefrontAccessMode.RESTRICTED)

        mockMvc.perform(get("/v1/storefronts"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.total_elements").value(2))
            .andExpect(jsonPath("$.data.content[0].access_mode").value("PUBLIC"))
            .andExpect(jsonPath("$.data.content[1].access_mode").value("RESTRICTED"))
    }

    // ── brand color round-trip ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /storefronts - includes brand_color_argb")
    fun `should include brand color argb`() {
        persist("themed-shop", "Themed Shop", StorefrontStatus.PUBLISHED, brandColor = 4279650378L)

        mockMvc.perform(get("/v1/storefronts"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].brand_color_argb").value(4279650378L))
    }

    // ── public access (no auth, no workspace header) ──────────────────────────

    @Test
    @DisplayName("GET /storefronts - is reachable without authentication or workspace header")
    fun `should be public`() {
        // No @WithMockUser, no X-Workspace-ID header — must still succeed.
        mockMvc.perform(get("/v1/storefronts"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun persist(
        slug: String,
        name: String,
        status: StorefrontStatus,
        accessMode: StorefrontAccessMode = StorefrontAccessMode.PUBLIC,
        brandColor: Long? = null,
    ): Storefront {
        // No tenant context is set in this test, so Hibernate resolves the "default" tenant for the
        // session; @TenantId requires the entity's owner_id to match, so we assign it explicitly.
        // owner_id is irrelevant to the directory query — it is native and intentionally cross-tenant.
        val saved = storefrontRepository.save(Storefront().apply {
            this.slug = slug
            this.name = name
            this.status = status
            this.accessMode = accessMode
            this.ownerId = "default"
            this.brandColorArgb = brandColor
        })
        entityManager.flush() // make the row visible to the subsequent native query
        return saved
    }
}

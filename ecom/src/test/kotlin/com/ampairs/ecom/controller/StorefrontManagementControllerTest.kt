package com.ampairs.ecom.controller

import com.ampairs.AmpairsApplication
import com.ampairs.ecom.domain.dto.StorefrontRequest
import com.ampairs.ecom.domain.dto.StorefrontUpdateRequest
import com.ampairs.ecom.domain.dto.TaxonomyImageRequest
import com.ampairs.ecom.domain.enums.StorefrontStatus
import com.ampairs.ecom.domain.enums.TaxonomyType
import com.ampairs.ecom.domain.model.EcomTaxonomyImage
import com.ampairs.ecom.domain.model.Storefront
import com.ampairs.ecom.exception.StorefrontAlreadyExistsException
import com.ampairs.ecom.exception.StorefrontNotFoundException
import com.ampairs.ecom.exception.StorefrontSlugConflictException
import com.ampairs.ecom.kafka.EcomCatalogKafkaConsumer
import com.ampairs.ecom.kafka.EcomOrderKafkaProducer
import com.ampairs.ecom.kafka.EcomOrderStatusKafkaConsumer
import com.ampairs.ecom.repository.EcomTaxonomyImageRepository
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@SpringBootTest(classes = [AmpairsApplication::class])
@ActiveProfiles("test")
@Transactional
class StorefrontManagementControllerTest {

    @Autowired private lateinit var webApplicationContext: WebApplicationContext
    @Autowired private lateinit var objectMapper: ObjectMapper

    private lateinit var mockMvc: MockMvc

    @field:MockitoBean private lateinit var storefrontService: StorefrontService
    @field:MockitoBean private lateinit var taxonomyImageRepository: EcomTaxonomyImageRepository
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

    // ── POST /api/v1/ecom/management/storefront ───────────────────────────────

    @Test
    @DisplayName("POST /storefront - creates storefront for authenticated user")
    @WithMockUser(username = "ws-1", roles = ["USER"])
    fun `should create storefront`() {
        val request = StorefrontRequest(name = "My Shop", slug = "my-shop")
        whenever(storefrontService.createStorefront(any(), eq("ws-1"))).thenReturn(makeStorefront("sf-1", "ws-1", "my-shop"))

        mockMvc.perform(
            post("/v1/ecom/management/storefront")
                .header("X-Workspace-ID", "ws-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.slug").value("my-shop"))
    }

    @Test
    @DisplayName("POST /storefront - requires authentication")
    fun `should reject unauthenticated create storefront`() {
        val request = StorefrontRequest(name = "My Shop", slug = "my-shop")
        mockMvc.perform(
            post("/v1/ecom/management/storefront")
                .header("X-Workspace-ID", "ws-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("POST /storefront - returns 409 when slug is taken")
    @WithMockUser(username = "ws-1", roles = ["USER"])
    fun `should return conflict when slug already taken`() {
        whenever(storefrontService.createStorefront(any(), eq("ws-1")))
            .thenThrow(StorefrontSlugConflictException("Slug 'my-shop' is already taken"))

        mockMvc.perform(
            post("/v1/ecom/management/storefront")
                .header("X-Workspace-ID", "ws-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"My Shop","slug":"my-shop"}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    @DisplayName("POST /storefront - returns 409 when workspace already has a storefront")
    @WithMockUser(username = "ws-1", roles = ["USER"])
    fun `should return conflict when workspace already has storefront`() {
        whenever(storefrontService.createStorefront(any(), eq("ws-1")))
            .thenThrow(StorefrontAlreadyExistsException("Workspace already has a storefront"))

        mockMvc.perform(
            post("/v1/ecom/management/storefront")
                .header("X-Workspace-ID", "ws-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"My Shop","slug":"my-shop"}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    @DisplayName("POST /storefront - validates slug format")
    @WithMockUser(username = "ws-1", roles = ["USER"])
    fun `should reject invalid slug format`() {
        mockMvc.perform(
            post("/v1/ecom/management/storefront")
                .header("X-Workspace-ID", "ws-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"My Shop","slug":"My Invalid Slug!!"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
    }

    // ── GET /api/v1/ecom/management/storefront ────────────────────────────────

    @Test
    @DisplayName("GET /storefront - returns storefront for workspace")
    @WithMockUser(username = "ws-1", roles = ["USER"])
    fun `should get storefront`() {
        whenever(storefrontService.getStorefront("ws-1")).thenReturn(makeStorefront("sf-1", "ws-1", "my-shop"))

        mockMvc.perform(
            get("/v1/ecom/management/storefront")
                .header("X-Workspace-ID", "ws-1")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.uid").value("sf-1"))
    }

    @Test
    @DisplayName("GET /storefront - returns 404 when no storefront exists")
    @WithMockUser(username = "ws-1", roles = ["USER"])
    fun `should return 404 when storefront not found`() {
        whenever(storefrontService.getStorefront("ws-1"))
            .thenThrow(StorefrontNotFoundException("No storefront found"))

        mockMvc.perform(
            get("/v1/ecom/management/storefront")
                .header("X-Workspace-ID", "ws-1")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
    }

    // ── PUT /api/v1/ecom/management/storefront ────────────────────────────────

    @Test
    @DisplayName("PUT /storefront - updates storefront")
    @WithMockUser(username = "ws-1", roles = ["USER"])
    fun `should update storefront`() {
        val updated = makeStorefront("sf-1", "ws-1", "my-shop").apply { name = "Updated Shop" }
        whenever(storefrontService.updateStorefront(any(), eq("ws-1"))).thenReturn(updated)

        mockMvc.perform(
            put("/v1/ecom/management/storefront")
                .header("X-Workspace-ID", "ws-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(StorefrontUpdateRequest(name = "Updated Shop")))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Updated Shop"))
    }

    // ── PUT /api/v1/ecom/management/storefront/publish ────────────────────────

    @Test
    @DisplayName("PUT /storefront/publish - publishes storefront")
    @WithMockUser(username = "ws-1", roles = ["USER"])
    fun `should publish storefront`() {
        val published = makeStorefront("sf-1", "ws-1", "my-shop").apply {
            status = StorefrontStatus.PUBLISHED
            publishedAt = Instant.now()
        }
        whenever(storefrontService.publishStorefront("ws-1")).thenReturn(published)

        mockMvc.perform(
            put("/v1/ecom/management/storefront/publish")
                .header("X-Workspace-ID", "ws-1")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
    }

    // ── PUT /api/v1/ecom/management/storefront/unpublish ──────────────────────

    @Test
    @DisplayName("PUT /storefront/unpublish - unpublishes storefront")
    @WithMockUser(username = "ws-1", roles = ["USER"])
    fun `should unpublish storefront`() {
        val unpublished = makeStorefront("sf-1", "ws-1", "my-shop").apply {
            status = StorefrontStatus.UNPUBLISHED
            unpublishedAt = Instant.now()
        }
        whenever(storefrontService.unpublishStorefront("ws-1")).thenReturn(unpublished)

        mockMvc.perform(
            put("/v1/ecom/management/storefront/unpublish")
                .header("X-Workspace-ID", "ws-1")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("UNPUBLISHED"))
    }

    // ── GET /api/v1/ecom/management/taxonomy ──────────────────────────────────

    @Test
    @DisplayName("GET /taxonomy - lists taxonomy images for storefront")
    @WithMockUser(username = "ws-1", roles = ["USER"])
    fun `should list taxonomy images`() {
        val sf = makeStorefront("sf-1", "ws-1", "my-shop")
        val image = makeTaxonomyImage("img-1", "sf-1", TaxonomyType.CATEGORY, "Electronics")
        whenever(storefrontService.getStorefront("ws-1")).thenReturn(sf)
        whenever(taxonomyImageRepository.findByStorefrontIdOrderBySortOrderAsc("sf-1")).thenReturn(listOf(image))

        mockMvc.perform(
            get("/v1/ecom/management/taxonomy")
                .header("X-Workspace-ID", "ws-1")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].name").value("Electronics"))
    }

    @Test
    @DisplayName("GET /taxonomy - filters by type when ?type= is provided")
    @WithMockUser(username = "ws-1", roles = ["USER"])
    fun `should filter taxonomy images by type`() {
        val sf = makeStorefront("sf-1", "ws-1", "my-shop")
        val image = makeTaxonomyImage("img-1", "sf-1", TaxonomyType.BRAND, "Nike")
        whenever(storefrontService.getStorefront("ws-1")).thenReturn(sf)
        whenever(taxonomyImageRepository.findByStorefrontIdAndTaxonomyTypeOrderBySortOrderAsc("sf-1", TaxonomyType.BRAND))
            .thenReturn(listOf(image))

        mockMvc.perform(
            get("/v1/ecom/management/taxonomy")
                .header("X-Workspace-ID", "ws-1")
                .param("type", "BRAND")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].name").value("Nike"))
            .andExpect(jsonPath("$.data[0].type").value("BRAND"))
    }

    // ── PUT /api/v1/ecom/management/taxonomy ──────────────────────────────────

    @Test
    @DisplayName("PUT /taxonomy - upserts taxonomy image")
    @WithMockUser(username = "ws-1", roles = ["USER"])
    fun `should upsert taxonomy image`() {
        val sf = makeStorefront("sf-1", "ws-1", "my-shop")
        val image = makeTaxonomyImage("img-1", "sf-1", TaxonomyType.CATEGORY, "Electronics")
        whenever(storefrontService.getStorefront("ws-1")).thenReturn(sf)
        whenever(taxonomyImageRepository.findByStorefrontIdAndTaxonomyTypeAndName("sf-1", TaxonomyType.CATEGORY, "Electronics"))
            .thenReturn(null)
        whenever(taxonomyImageRepository.save(any<EcomTaxonomyImage>())).thenReturn(image)

        val request = TaxonomyImageRequest(
            type = TaxonomyType.CATEGORY,
            name = "Electronics",
            imageUrl = "https://cdn.example.com/electronics.jpg",
            sortOrder = 1,
        )
        mockMvc.perform(
            put("/v1/ecom/management/taxonomy")
                .header("X-Workspace-ID", "ws-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Electronics"))
    }

    // ── DELETE /api/v1/ecom/management/taxonomy/{type}/{name} ─────────────────

    @Test
    @DisplayName("DELETE /taxonomy/{type}/{name} - deletes taxonomy image")
    @WithMockUser(username = "ws-1", roles = ["USER"])
    fun `should delete taxonomy image`() {
        val sf = makeStorefront("sf-1", "ws-1", "my-shop")
        val image = makeTaxonomyImage("img-1", "sf-1", TaxonomyType.CATEGORY, "Electronics")
        whenever(storefrontService.getStorefront("ws-1")).thenReturn(sf)
        whenever(taxonomyImageRepository.findByStorefrontIdAndTaxonomyTypeAndName("sf-1", TaxonomyType.CATEGORY, "Electronics"))
            .thenReturn(image)

        mockMvc.perform(
            delete("/v1/ecom/management/taxonomy/CATEGORY/Electronics")
                .header("X-Workspace-ID", "ws-1")
        )
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("DELETE /taxonomy/{type}/{name} - returns 404 when image not found")
    @WithMockUser(username = "ws-1", roles = ["USER"])
    fun `should return 404 when taxonomy image not found`() {
        val sf = makeStorefront("sf-1", "ws-1", "my-shop")
        whenever(storefrontService.getStorefront("ws-1")).thenReturn(sf)
        whenever(taxonomyImageRepository.findByStorefrontIdAndTaxonomyTypeAndName("sf-1", TaxonomyType.CATEGORY, "Missing"))
            .thenReturn(null)

        mockMvc.perform(
            delete("/v1/ecom/management/taxonomy/CATEGORY/Missing")
                .header("X-Workspace-ID", "ws-1")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun makeStorefront(uid: String, ownerId: String, slug: String) = Storefront().apply {
        this.uid = uid
        this.ownerId = ownerId
        this.slug = slug
        name = "Shop $uid"
        status = StorefrontStatus.DRAFT
        createdAt = Instant.now()
        updatedAt = Instant.now()
    }

    private fun makeTaxonomyImage(uid: String, storefrontId: String, type: TaxonomyType, name: String) =
        EcomTaxonomyImage().apply {
            this.uid = uid
            this.storefrontId = storefrontId
            ownerId = "ws-1"
            taxonomyType = type
            this.name = name
            imageUrl = "https://cdn.example.com/$name.jpg"
            sortOrder = 1
        }
}

package com.ampairs.ecom.service

import com.ampairs.ecom.domain.dto.StorefrontRequest
import com.ampairs.ecom.domain.dto.StorefrontUpdateRequest
import com.ampairs.ecom.domain.enums.StorefrontStatus
import com.ampairs.ecom.domain.model.Storefront
import com.ampairs.ecom.exception.StorefrontAlreadyExistsException
import com.ampairs.ecom.exception.StorefrontNotFoundException
import com.ampairs.ecom.exception.StorefrontSlugConflictException
import com.ampairs.ecom.repository.StorefrontRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class StorefrontServiceTest {

    @Mock private lateinit var storefrontRepository: StorefrontRepository

    private lateinit var storefrontService: StorefrontService

    @BeforeEach
    fun setup() {
        storefrontService = StorefrontService(storefrontRepository)
    }

    // ── createStorefront ──────────────────────────────────────────────────────

    @Test
    fun `createStorefront succeeds when slug is unique and workspace has no storefront`() {
        val request = StorefrontRequest(name = "My Shop", slug = "my-shop")
        val saved = makeStorefront("sf1", "ws1", "my-shop")

        whenever(storefrontRepository.existsBySlug("my-shop")).thenReturn(false)
        whenever(storefrontRepository.findByOwnerId("ws1")).thenReturn(null)
        whenever(storefrontRepository.save(any<Storefront>())).thenReturn(saved)

        val result = storefrontService.createStorefront(request, "ws1")
        assertEquals("sf1", result.uid)
        verify(storefrontRepository).save(any<Storefront>())
    }

    @Test
    fun `createStorefront throws StorefrontSlugConflictException when slug already taken`() {
        whenever(storefrontRepository.existsBySlug("taken-slug")).thenReturn(true)

        assertThrows<StorefrontSlugConflictException> {
            storefrontService.createStorefront(StorefrontRequest(name = "Shop", slug = "taken-slug"), "ws1")
        }
    }

    @Test
    fun `createStorefront throws StorefrontAlreadyExistsException when workspace has storefront`() {
        whenever(storefrontRepository.existsBySlug("new-slug")).thenReturn(false)
        whenever(storefrontRepository.findByOwnerId("ws1")).thenReturn(makeStorefront("sf1", "ws1", "existing"))

        assertThrows<StorefrontAlreadyExistsException> {
            storefrontService.createStorefront(StorefrontRequest(name = "Shop", slug = "new-slug"), "ws1")
        }
    }

    // ── getStorefront ─────────────────────────────────────────────────────────

    @Test
    fun `getStorefront returns storefront for workspace`() {
        val sf = makeStorefront("sf1", "ws1", "my-shop")
        whenever(storefrontRepository.findByOwnerId("ws1")).thenReturn(sf)

        val result = storefrontService.getStorefront("ws1")
        assertEquals("sf1", result.uid)
    }

    @Test
    fun `getStorefront throws StorefrontNotFoundException when none exists`() {
        whenever(storefrontRepository.findByOwnerId("ws-missing")).thenReturn(null)
        assertThrows<StorefrontNotFoundException> { storefrontService.getStorefront("ws-missing") }
    }

    // ── updateStorefront ──────────────────────────────────────────────────────

    @Test
    fun `updateStorefront applies changes and saves`() {
        val existing = makeStorefront("sf1", "ws1", "my-shop")
        val request = StorefrontUpdateRequest(name = "Updated Shop", description = "New desc")
        val saved = makeStorefront("sf1", "ws1", "my-shop").apply { name = "Updated Shop" }

        whenever(storefrontRepository.findByOwnerId("ws1")).thenReturn(existing)
        whenever(storefrontRepository.save(any<Storefront>())).thenReturn(saved)

        val result = storefrontService.updateStorefront(request, "ws1")
        assertEquals("Updated Shop", result.name)
    }

    // ── getPublishedStorefrontBySlug ──────────────────────────────────────────

    @Test
    fun `getPublishedStorefrontBySlug returns published storefront`() {
        val sf = makeStorefront("sf1", "ws1", "my-shop").apply { status = StorefrontStatus.PUBLISHED }
        whenever(storefrontRepository.findBySlugAndStatus("my-shop", StorefrontStatus.PUBLISHED.name)).thenReturn(sf)

        val result = storefrontService.getPublishedStorefrontBySlug("my-shop")
        assertEquals(StorefrontStatus.PUBLISHED, result.status)
    }

    @Test
    fun `getPublishedStorefrontBySlug throws StorefrontNotFoundException when not found`() {
        whenever(storefrontRepository.findBySlugAndStatus("draft-shop", StorefrontStatus.PUBLISHED.name)).thenReturn(null)
        assertThrows<StorefrontNotFoundException> {
            storefrontService.getPublishedStorefrontBySlug("draft-shop")
        }
    }

    // ── publishStorefront ─────────────────────────────────────────────────────

    @Test
    fun `publishStorefront sets status to PUBLISHED and clears unpublishedAt`() {
        val sf = makeStorefront("sf1", "ws1", "my-shop").apply { status = StorefrontStatus.DRAFT }
        whenever(storefrontRepository.findByOwnerId("ws1")).thenReturn(sf)
        whenever(storefrontRepository.save(sf)).thenReturn(sf)

        storefrontService.publishStorefront("ws1")

        assertEquals(StorefrontStatus.PUBLISHED, sf.status)
        assertNotNull(sf.publishedAt)
        assertNull(sf.unpublishedAt)
        verify(storefrontRepository).save(sf)
    }

    // ── unpublishStorefront ───────────────────────────────────────────────────

    @Test
    fun `unpublishStorefront sets status to UNPUBLISHED`() {
        val sf = makeStorefront("sf1", "ws1", "my-shop").apply { status = StorefrontStatus.PUBLISHED }
        whenever(storefrontRepository.findByOwnerId("ws1")).thenReturn(sf)
        whenever(storefrontRepository.save(sf)).thenReturn(sf)

        storefrontService.unpublishStorefront("ws1")

        assertEquals(StorefrontStatus.UNPUBLISHED, sf.status)
        assertNotNull(sf.unpublishedAt)
        verify(storefrontRepository).save(sf)
    }

    // ── findStorefrontIdByWorkspaceId ─────────────────────────────────────────

    @Test
    fun `findStorefrontIdByWorkspaceId returns uid when found`() {
        val sf = makeStorefront("sf1", "ws1", "my-shop")
        whenever(storefrontRepository.findByOwnerId("ws1")).thenReturn(sf)

        assertEquals("sf1", storefrontService.findStorefrontIdByWorkspaceId("ws1"))
    }

    @Test
    fun `findStorefrontIdByWorkspaceId returns null when not found`() {
        whenever(storefrontRepository.findByOwnerId("ws-missing")).thenReturn(null)

        assertNull(storefrontService.findStorefrontIdByWorkspaceId("ws-missing"))
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun makeStorefront(uid: String, ownerId: String, slug: String) = Storefront().apply {
        this.uid = uid
        this.ownerId = ownerId
        this.slug = slug
        name = "Shop $uid"
        status = StorefrontStatus.DRAFT
    }
}

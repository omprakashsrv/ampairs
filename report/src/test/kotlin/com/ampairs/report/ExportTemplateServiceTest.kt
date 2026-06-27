package com.ampairs.report

import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.report.domain.dto.ExportFilterDto
import com.ampairs.report.domain.dto.ExportTemplateRequest
import com.ampairs.report.domain.model.ExportTemplate
import com.ampairs.report.repository.ExportTemplateRepository
import com.ampairs.report.service.ExportTemplateServiceImpl
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

/**
 * Service-level coverage for the Export Template `/sync` upsert + feed logic
 * (spec task T034a). Exercises the UID-keyed bulk-upsert (create vs update),
 * the JSON round-trip of selected columns + filters, and the lastSync fallback
 * to the full feed. A full MockMvc + Testcontainers contract test (tenant
 * isolation, soft-deleted rows in the feed) is the remaining required slice.
 */
@ExtendWith(MockitoExtension::class)
class ExportTemplateServiceTest {

    @Mock
    private lateinit var repository: ExportTemplateRepository

    @Mock
    private lateinit var entityChangePublisher: EntityChangePublisher

    private lateinit var service: ExportTemplateServiceImpl

    @BeforeEach
    fun setUp() {
        service = ExportTemplateServiceImpl(repository, jacksonObjectMapper(), entityChangePublisher)
    }

    @Test
    fun `bulkUpsert creates a new template when uid is absent and round-trips columns and filters`() {
        whenever(repository.save(any<ExportTemplate>())).thenAnswer { it.getArgument(0) }

        val request = ExportTemplateRequest(
            uid = null,
            moduleKey = "customer",
            name = "Customers with phones",
            selectedColumns = listOf("uid", "name", "phone"),
            filters = listOf(ExportFilterDto(columnKey = "active", op = "isActive", value = "true")),
            defaultFormat = "CSV",
            defaultLocation = "CLIENT",
        )

        val response = service.bulkUpsert(listOf(request)).single()

        assertEquals("customer", response.moduleKey)
        assertEquals(listOf("uid", "name", "phone"), response.selectedColumns)
        assertEquals(1, response.filters.size)
        assertEquals("active", response.filters.first().columnKey)
        assertEquals("isActive", response.filters.first().op)
        verify(repository).save(any<ExportTemplate>())
        verify(entityChangePublisher).created(any(), any())
    }

    @Test
    fun `bulkUpsert updates an existing template matched by uid (no duplicate)`() {
        val existing = ExportTemplate().apply {
            uid = "EXT-1"
            moduleKey = "customer"
            name = "Old name"
        }
        whenever(repository.findByUid("EXT-1")).thenReturn(existing)
        whenever(repository.save(any<ExportTemplate>())).thenAnswer { it.getArgument(0) }

        val request = ExportTemplateRequest(uid = "EXT-1", moduleKey = "customer", name = "New name")

        val response = service.bulkUpsert(listOf(request)).single()

        assertEquals("EXT-1", response.uid)
        assertEquals("New name", response.name)
        verify(repository).save(existing)
        verify(entityChangePublisher).updated(any(), any())
    }

    @Test
    fun `getTemplatesAfterSync uses the full feed when lastSync is blank`() {
        val page: Page<ExportTemplate> = PageImpl(
            listOf(ExportTemplate().apply { uid = "EXT-1"; moduleKey = "customer"; name = "T" })
        )
        whenever(repository.findAllForSync(any())).thenReturn(page)

        val result = service.getTemplatesAfterSync(null, PageRequest.of(0, 100))

        assertEquals(1, result.content.size)
        assertEquals("EXT-1", result.content.first().uid)
        assertTrue(result.content.first().selectedColumns.isEmpty())
    }
}

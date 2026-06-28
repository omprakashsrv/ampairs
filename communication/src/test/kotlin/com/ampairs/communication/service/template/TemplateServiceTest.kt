package com.ampairs.communication.service.template

import com.ampairs.communication.domain.dto.TemplateAggregateRequest
import com.ampairs.communication.domain.dto.TemplateVariantRequest
import com.ampairs.communication.domain.model.MessageTemplate
import com.ampairs.communication.domain.model.TemplateVariant
import com.ampairs.communication.repository.MessageTemplateRepository
import com.ampairs.communication.repository.TemplateVariantRepository
import com.ampairs.communication.service.TemplateVersionConflictException
import com.ampairs.core.sync.EntityChangePublisher
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TemplateServiceTest {

    private val templateRepo: MessageTemplateRepository = mock()
    private val variantRepo: TemplateVariantRepository = mock()
    private val publisher: EntityChangePublisher = mock()
    private val service = TemplateService(templateRepo, variantRepo, publisher)

    init {
        whenever(templateRepo.save(any<MessageTemplate>())).thenAnswer { it.arguments[0] }
        whenever(variantRepo.save(any<TemplateVariant>())).thenAnswer { it.arguments[0] }
    }

    private fun aggregate(baseVersion: Int, vararg variantUids: String) = TemplateAggregateRequest(
        uid = "CTPL1", code = "INV", baseVersion = baseVersion,
        variants = variantUids.map { TemplateVariantRequest(uid = it, channel = "EMAIL", locale = "en", subject = "s", htmlBody = "b") },
    )

    @Test
    fun `delete-by-absence soft-deletes variants not in the pushed aggregate`() {
        whenever(templateRepo.findByUid("CTPL1")).thenReturn(null)
        // existing variant V2 not present in the incoming aggregate (only V1)
        whenever(variantRepo.findByTemplateUid("CTPL1")).thenReturn(
            listOf(TemplateVariant().apply { uid = "V2"; templateUid = "CTPL1"; active = true })
        )
        whenever(variantRepo.findByUid("V1")).thenReturn(null)

        service.bulkUpsert(listOf(aggregate(1, "V1")))

        val captor = argumentCaptor<TemplateVariant>()
        verify(variantRepo, org.mockito.kotlin.atLeastOnce()).save(captor.capture())
        // V2 was deactivated (delete-by-absence)
        assertTrue(captor.allValues.any { it.uid == "V2" && !it.active })
    }

    @Test
    fun `stale base_version is rejected with a conflict`() {
        whenever(templateRepo.findByUid("CTPL1")).thenReturn(MessageTemplate().apply { uid = "CTPL1"; code = "INV"; baseVersion = 5 })
        assertThrows(TemplateVersionConflictException::class.java) {
            service.bulkUpsert(listOf(aggregate(3, "V1")))
        }
        verify(templateRepo, never()).save(any())
    }
}

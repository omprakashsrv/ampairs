package com.ampairs.agent.service

import com.ampairs.agent.domain.dto.asResponses
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Pure unit test — the catalog is static, so no Spring context is needed. */
class AiModelCatalogServiceTest {

    private val service = AiModelCatalogService()

    @Test
    fun `catalog is non-empty and ids are unique`() {
        val models = service.listModels()
        assertTrue(models.isNotEmpty(), "catalog must not be empty")
        val ids = models.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "model ids must be unique")
    }

    @Test
    fun `every model has a litertlm fileName, positive size and ram, and an https source`() {
        service.listModels().forEach { m ->
            assertTrue(m.fileName.endsWith(".litertlm"), "${m.id} fileName must be .litertlm")
            assertTrue(m.sizeBytes > 0, "${m.id} sizeBytes must be > 0")
            assertTrue(m.requiredRamMb > 0, "${m.id} requiredRamMb must be > 0")
            assertTrue(m.platforms.isNotEmpty(), "${m.id} must target at least one platform")
            assertTrue(m.sourceUrl.startsWith("https://"), "${m.id} sourceUrl must be https")
        }
    }

    @Test
    fun `findById resolves known ids and returns null for unknown`() {
        val first = service.listModels().first()
        assertNotNull(service.findById(first.id))
        assertNull(service.findById("does-not-exist"))
    }

    @Test
    fun `manifest dto never leaks the upstream sourceUrl`() {
        // asResponses() is the public projection; it intentionally drops sourceUrl.
        val responses = service.listModels().asResponses()
        assertEquals(service.listModels().size, responses.size)
        assertFalse(
            responses.any { it.toString().contains("huggingface", ignoreCase = true) },
            "manifest response must not expose the upstream source",
        )
    }
}

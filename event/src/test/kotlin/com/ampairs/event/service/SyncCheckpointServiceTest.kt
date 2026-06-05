package com.ampairs.event.service

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.sync.SyncCheckpointContributor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant

class SyncCheckpointServiceTest {

    @AfterEach
    fun tearDown() = TenantContextHolder.clearTenantContext()

    private fun contributor(map: Map<String, Instant?>) = object : SyncCheckpointContributor {
        override fun checkpoints(): Map<String, Instant?> = map
    }

    @Test
    fun `merges contributors and keeps the latest timestamp per entity`() {
        TenantContextHolder.setCurrentTenant("ws-1")
        val older = Instant.parse("2026-01-01T00:00:00Z")
        val newer = Instant.parse("2026-02-01T00:00:00Z")

        val service = SyncCheckpointService(
            listOf(
                contributor(mapOf("customer" to older, "product" to null)),
                contributor(mapOf("customer" to newer, "tax" to older)),
            )
        )

        val result = service.checkpoints()

        assertEquals(newer, result["customer"], "later contributor timestamp should win")
        assertEquals(older, result["tax"])
        assertNull(result["product"], "null checkpoint means no rows — must stay null")
    }

    @Test
    fun `a failing contributor is skipped, not fatal`() {
        TenantContextHolder.setCurrentTenant("ws-1")
        val ts = Instant.parse("2026-03-01T00:00:00Z")

        val service = SyncCheckpointService(
            listOf(
                object : SyncCheckpointContributor {
                    override fun checkpoints(): Map<String, Instant?> = error("boom")
                },
                contributor(mapOf("unit" to ts)),
            )
        )

        val result = service.checkpoints()
        assertEquals(ts, result["unit"])
    }

    @Test
    fun `requires a workspace context`() {
        val service = SyncCheckpointService(emptyList())
        assertThrows(IllegalStateException::class.java) { service.checkpoints() }
    }
}

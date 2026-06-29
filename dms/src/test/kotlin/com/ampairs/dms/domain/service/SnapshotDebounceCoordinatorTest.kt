package com.ampairs.dms.domain.service

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SnapshotDebounceCoordinatorTest {

    private val window = 5L * 60L * 1000L
    private val coordinator = SnapshotDebounceCoordinator(window)

    @Test
    fun `first rebuild for a distributor always runs`() {
        assertTrue(coordinator.shouldRebuild("DIST-1", 1_000L))
    }

    @Test
    fun `a second rebuild within the window is coalesced away`() {
        assertTrue(coordinator.shouldRebuild("DIST-1", 1_000L))
        assertFalse(coordinator.shouldRebuild("DIST-1", 1_000L + window - 1)) // just inside window
    }

    @Test
    fun `a rebuild at or past the window runs again`() {
        assertTrue(coordinator.shouldRebuild("DIST-1", 1_000L))
        assertTrue(coordinator.shouldRebuild("DIST-1", 1_000L + window)) // window elapsed
    }

    @Test
    fun `distributors are coalesced independently`() {
        assertTrue(coordinator.shouldRebuild("DIST-1", 1_000L))
        assertTrue(coordinator.shouldRebuild("DIST-2", 1_000L)) // different distributor, not blocked
        assertFalse(coordinator.shouldRebuild("DIST-1", 2_000L))
    }
}

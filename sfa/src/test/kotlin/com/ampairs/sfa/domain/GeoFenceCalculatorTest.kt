package com.ampairs.sfa.domain

import com.ampairs.sfa.domain.enums.GeoFenceStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GeoFenceCalculatorTest {

    @Test
    fun `null distance is NO_LOCATION`() {
        assertEquals(GeoFenceStatus.NO_LOCATION, GeoFenceCalculator.classify(null, 200.0))
    }

    @Test
    fun `within radius is IN_RADIUS`() {
        assertEquals(GeoFenceStatus.IN_RADIUS, GeoFenceCalculator.classify(150.0, 200.0))
        assertEquals(GeoFenceStatus.IN_RADIUS, GeoFenceCalculator.classify(200.0, 200.0))
    }

    @Test
    fun `beyond radius is OUT_OF_RADIUS`() {
        assertEquals(GeoFenceStatus.OUT_OF_RADIUS, GeoFenceCalculator.classify(250.0, 200.0))
    }

    @Test
    fun `haversine distance is roughly correct`() {
        // ~111 km per degree of latitude near the equator.
        val d = GeoFenceCalculator.distanceMeters(0.0, 0.0, 1.0, 0.0)
        assertTrue(d in 110_000.0..112_000.0, "expected ~111km but was $d")
    }
}

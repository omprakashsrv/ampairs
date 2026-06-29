package com.ampairs.sfa.domain

import com.ampairs.sfa.domain.enums.GeoFenceStatus
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure geo-fence helper. Classifies a visit's proximity to its outlet for *reporting only* —
 * it never blocks check-in (FR-016a). No location ⇒ NO_LOCATION; within radius ⇒ IN_RADIUS;
 * else OUT_OF_RADIUS.
 */
object GeoFenceCalculator {

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /** Classify from a precomputed distance (metres). Null distance ⇒ NO_LOCATION. */
    fun classify(distanceMeters: Double?, radiusMeters: Double): GeoFenceStatus = when {
        distanceMeters == null -> GeoFenceStatus.NO_LOCATION
        distanceMeters <= radiusMeters -> GeoFenceStatus.IN_RADIUS
        else -> GeoFenceStatus.OUT_OF_RADIUS
    }

    /** Great-circle (haversine) distance in metres between two lat/lng points. */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}

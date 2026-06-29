package com.ampairs.sfa.domain.enums

/** Outcome a rep records at the retailer counter. */
enum class VisitOutcome {
    PRODUCTIVE,
    NO_ORDER,
    OUTLET_CLOSED,
}

/**
 * Captured (informational) proximity classification of a visit's location to the outlet.
 * Never gates check-in — an OUT_OF_RADIUS / NO_LOCATION visit is still recorded (FR-016a).
 */
enum class GeoFenceStatus {
    IN_RADIUS,
    OUT_OF_RADIUS,
    NO_LOCATION,
}

/** Lifecycle of a rep's daily attendance row (check-in → check-out / auto-close). */
enum class AttendanceStatus {
    OPEN,
    CLOSED,
    AUTO_CLOSED,
}

/** Reconciliation state of a planned stop against authored visits (drives adherence). */
enum class PlannedVisitStatus {
    PENDING,
    VISITED,
    MISSED,
}

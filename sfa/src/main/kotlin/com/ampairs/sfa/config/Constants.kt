package com.ampairs.sfa.config

/**
 * UID prefixes + tunable defaults for the SFA (field-sales automation) module.
 * UIDs are client-generated on the mobile device; the prefix is used only when the
 * server has to mint one (BaseDomain.prePersist) for server-authored rows.
 */
object Constants {
    const val BEAT_PREFIX = "BEAT"
    const val BEAT_OUTLET_PREFIX = "BTO"
    const val JOURNEY_PLAN_PREFIX = "PJP"
    const val PLANNED_VISIT_PREFIX = "PLV"
    const val VISIT_PREFIX = "VIS"
    const val ATTENDANCE_PREFIX = "ATT"
    const val FIELD_ORDER_PREFIX = "FOR"
    const val LEAVE_PREFIX = "LEV"
    const val VISIT_SURVEY_PREFIX = "VSR"

    /** Hours past a rep's check-in after which an un-closed attendance is auto-closed (Phase 8b). */
    const val ATTENDANCE_AUTO_CLOSE_AFTER_HOURS = 16L

    /** Default geo-fence radius (metres) used to flag (never block) out-of-radius check-ins. */
    const val DEFAULT_GEO_FENCE_RADIUS_METERS = 200.0
}

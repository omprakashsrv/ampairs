package com.ampairs.sfa.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.sfa.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * A structured store-visit survey captured offline against a [Visit]. Answers are stored as a JSON
 * blob (`responses`) keyed by the survey question key — the template itself lives in the `form` module
 * (EntityType.VISIT_SURVEY, a follow-up). Rides the offline `/sync` contract.
 */
@Entity
@Table(
    name = "visit_survey_responses",
    indexes = [
        Index(name = "idx_visit_survey_owner", columnList = "owner_id"),
        Index(name = "idx_visit_survey_visit", columnList = "visit_uid"),
        Index(name = "idx_visit_survey_updated_at", columnList = "updated_at"),
    ],
)
class VisitSurveyResponse : OwnableBaseDomain() {

    @Column(name = "visit_uid", nullable = false, length = 40)
    var visitUid: String = ""

    @Column(name = "rep_member_uid", length = 40)
    var repMemberUid: String? = null

    /** JSON map of survey question key → answer value. */
    @Column(name = "responses", columnDefinition = "TEXT")
    var responses: String? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.VISIT_SURVEY_PREFIX
}

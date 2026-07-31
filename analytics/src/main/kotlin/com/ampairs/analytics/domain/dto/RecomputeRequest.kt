package com.ampairs.analytics.domain.dto

import jakarta.validation.constraints.NotNull
import java.time.LocalDate

/** Request to rebuild the materialized KPI summary for an inclusive business-date range. */
data class RecomputeRequest(
    @field:NotNull
    val fromDate: LocalDate? = null,
    @field:NotNull
    val toDate: LocalDate? = null,
)

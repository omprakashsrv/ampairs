package com.ampairs.cb_maintenance.domain.model

enum class TicketStatus { OPEN, ASSIGNED, IN_PROGRESS, ON_HOLD, RESOLVED, CLOSED }

enum class FrequencyUnit { DAY, WEEK, MONTH, YEAR }

enum class PmEntrySource { SCHEDULED, AD_HOC }

enum class PmEntryStatus { DUE, OVERDUE, ASSIGNED, IN_PROGRESS, DONE, VERIFIED, SKIPPED }

/** One checklist line result, stored as part of a `PmEntry.checklistResult` JSON list. */
data class ChecklistItemResult(
    val item: String = "",
    val passed: Boolean = true,
    val note: String? = null,
)

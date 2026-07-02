package com.ampairs.core.domain.enums

/**
 * Sales channel a price list / promotion / order is scoped to.
 *
 * Shared, workspace-agnostic enum so ecom / order / invoice / pricing / promotion can all reference
 * it without cross-feature coupling. Extensible (DISTRIBUTOR / B2B_MARKETPLACE later).
 */
enum class SalesChannel {
    RETAIL,
    WHOLESALE,
}

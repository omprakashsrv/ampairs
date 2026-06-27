package com.ampairs.communication.domain.dto

/** One aggregated usage bucket — channel × credential × billing mode (× cost category). */
data class UsageRow(
    val channel: String,
    val credentialUid: String?,
    val providerAccountRef: String?,
    val billingMode: String,
    val costCategory: String?,
    val messageCount: Long,
    val costUnits: Long,
)

/** Usage/billing report for a period. PLATFORM rows are what the client is billed for. */
data class UsageReportResponse(
    val from: String,
    val to: String,
    val rows: List<UsageRow>,
    val totalMessages: Long,
    val totalCostUnits: Long,
    val platformBillableMessages: Long,
)

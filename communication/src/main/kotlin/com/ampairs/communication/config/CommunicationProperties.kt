package com.ampairs.communication.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Module configuration (env-driven). The credential encryption key is the AES-GCM master key for
 * per-workspace provider secrets — supplied via the environment (`COMM_CRED_ENCRYPTION_KEY`),
 * never committed.
 */
@ConfigurationProperties(prefix = "communication")
data class CommunicationProperties(
    val scheduler: Scheduler = Scheduler(),
    val campaign: Campaign = Campaign(),
) {
    data class Scheduler(
        val enabled: Boolean = true,
        val tickSeconds: Long = 60,
    )

    data class Campaign(
        val defaultThrottlePerMinute: Int = 60,
    )
}

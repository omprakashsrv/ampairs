package com.ampairs.subscription.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Enable Spring's scheduled task execution capability.
 *
 * Scheduled jobs run in subscription module:
 * - Subscription downgrade processing (hourly)
 * - Monthly usage counter reset (1st of month)
 * - Renewal reminders (daily)
 * - Trial expiry (hourly)
 * - Usage metrics cleanup (monthly)
 *
 * Jobs can be disabled via application.yml:
 * subscription.scheduled-jobs.enabled=false
 */
@Configuration
@EnableScheduling
class SchedulingConfig

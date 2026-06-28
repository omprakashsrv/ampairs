package com.ampairs.communication.domain.enums

/** Delivery channels. Mirrors notification's NotificationChannel value set. */
enum class Channel { EMAIL, SMS, WHATSAPP, PUSH }

/** Transactional bypasses promotional opt-out/quiet-hours; promotional is gated. */
enum class MessageCategory { TRANSACTIONAL, PROMOTIONAL }

/** What caused a send. */
enum class TriggerType { EVENT, MANUAL, SCHEDULE, CAMPAIGN }

/** How an audience is targeted; resolved to concrete recipients at send time. */
enum class AudienceType { SINGLE, LIST, SEGMENT }

/** Per-message delivery status — MONOTONIC (never regresses to a less-progressed state). */
enum class DeliveryStatus { QUEUED, SENT, DELIVERED, READ, FAILED, SKIPPED, EXHAUSTED }

/** Why a per-recipient/channel send was skipped (recorded, never a hard failure of the request). */
enum class SkipReason { NO_ADDRESS, OPTED_OUT, SUPPRESSED, QUIET_HOURS_EXPIRED, NO_VARIANT, NO_CREDENTIAL }

/** Recurrence cadence for a schedule. */
enum class Frequency { DAILY, WEEKLY, MONTHLY }

/** Promotional campaign lifecycle. */
enum class CampaignStatus { DRAFT, SCHEDULED, RUNNING, PAUSED, DONE }

/** Address-level suppression reason. */
enum class SuppressionReason { HARD_BOUNCE, COMPLAINT, UNSUBSCRIBE }

/**
 * Billing attribution for a sent message.
 * - CLIENT_OWN: sent on the workspace's own credential — the client's provider cost.
 * - PLATFORM:   sent on the shared platform credential — billable to the client.
 */
enum class BillingMode { CLIENT_OWN, PLATFORM }

/** Validity state of a workspace provider credential. */
enum class CredentialStatus { UNVERIFIED, VALID, INVALID, EXPIRED }

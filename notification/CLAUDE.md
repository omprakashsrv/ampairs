# notification module

Multi-channel notification delivery — SMS (MSG91 primary, AWS SNS fallback), push (Firebase).

## Key entity
- `NotificationQueue` — recipient, message, channel, status (PENDING/SENT/FAILED/RETRYING/EXHAUSTED), retryCount, providerUsed, providerResponse

## OTP delivery
`OtpNotificationService` is called by auth module. In test profile, OTP is fixed to `123456` — no SMS sent.

## Env vars
`SMS_PRIMARY_PROVIDER`, `MSG91_AUTH_KEY`, `MSG91_TEMPLATE_ID`, `AWS_SNS_ENABLED`

## Base path
`/notification/v1/**`

## Migrations
`V1.0.4`

## Full docs
`docs/modules/notification.md`

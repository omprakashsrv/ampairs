# notification module

Multi-channel notification delivery with provider failover, retry policies, and queue management. Currently supports SMS and push notifications; email and WhatsApp are planned.

## Channels

| Channel | Providers | Status |
|---------|-----------|--------|
| SMS | MSG91 (primary), AWS SNS (fallback) | Active |
| Push | Firebase FCM | Active (via auth module) |
| Email | SMTP | Planned |
| WhatsApp | Twilio | Planned |

## REST Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/notification/v1/stats` | Notification queue statistics |
| GET | `/notification/v1/sms/stats` | SMS-specific statistics |
| POST | `/notification/v1/send/immediate` | Send immediate notification |
| POST | `/notification/v1/test` | Send test notification |
| POST | `/notification/v1/sms/test` | Send test SMS |

## Key Entity

### NotificationQueue

```kotlin
class NotificationQueue : BaseDomain() {   // typealias: SmsQueue
    val recipient: String              // phone number or email
    val message: String
    val channel: NotificationChannel   // SMS, EMAIL, PUSH, WHATSAPP
    val status: NotificationStatus     // PENDING, SENT, FAILED, RETRYING, EXHAUSTED
    val retryCount: Int
    val maxRetries: Int                // default 3
    val scheduledAt: Instant
    val lastAttemptAt: Instant?
    val providerUsed: String?          // MSG91, AWS_SNS, etc.
    val providerMessageId: String?
    val errorMessage: String?
    val providerResponse: String?      // raw JSON from provider
}
```

## SMS Configuration

Primary vs fallback is controlled by `SMS_PRIMARY_PROVIDER`:

```bash
SMS_PRIMARY_PROVIDER=MSG91   # MSG91 or AWS_SNS

# MSG91
MSG91_AUTH_KEY=xxx
MSG91_TEMPLATE_ID=xxx
MSG91_SENDER_ID=AMPAIR

# AWS SNS (fallback)
AWS_ACCESS_KEY_ID=xxx
AWS_SECRET_ACCESS_KEY=xxx
AWS_REGION=ap-south-1
```

## OTP Delivery

OTP SMS is handled by `OtpNotificationService` in this module, called by the `auth` module. Template is managed by `SmsTemplateService`.

In test profile (`SPRING_PROFILES_ACTIVE=test`), OTP is fixed to `123456` — no SMS is sent.

## Batch Processing

```bash
NOTIFICATION_BATCH_SIZE=10           # process N notifications per batch
NOTIFICATION_RETRY_DELAY_MINUTES=5  # wait between retry attempts
NOTIFICATION_CLEANUP_DAYS=30        # purge delivered/exhausted records after N days
NOTIFICATION_PARALLEL_THREADS=5     # concurrent notification threads
```

## Database Migrations

| File | Description |
|------|-------------|
| `V1.0.4__create_notification_module_tables.sql` | notification_queue table |

## Package Structure

```
com.ampairs.notification
├── config/         — NotificationConfig
├── controller/     — NotificationController
├── model/          — NotificationQueue (SmsQueue typealias)
├── provider/       — NotificationProvider (interface), NotificationChannel/Status (enums)
│   └── sms/        — Msg91SmsProvider, AwsSnsSmsProvider, SmsNotificationProvider
├── repository/     — NotificationQueueRepository
├── service/        — NotificationService, NotificationDatabaseService,
│                     OtpNotificationService
└── template/       — SmsTemplateService
```

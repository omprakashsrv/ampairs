# Notification Module

## Overview
The notification module queues and dispatches outbound messages for a workspace. It currently focuses on SMS delivery with pluggable providers (MSG91 primary, AWS SNS fallback), keeps a persistent queue for retries, exposes health/statistics endpoints, and offers “immediate send” utilities used by admin tooling.

## Architecture
### Package Structure
```
com.ampairs.notification/
├── config/         # Task executor, scheduler, and property configuration
├── controller/     # REST APIs under /notification/v1
├── model/          # `NotificationQueue` entity and supporting models
├── provider/       # Provider SPI + SMS implementations (MSG91, AWS SNS)
├── repository/     # `NotificationQueueRepository`
└── service/        # Notification orchestration, DB helpers, scheduled workers
```

## Core Responsibilities
- Queue notifications with tenant context via `NotificationService.queueNotification*`.
- Persist status, retry counts, and schedule in `NotificationQueue`.
- Dispatch notifications in batches using a scheduled task (every 30 seconds) and a dedicated executor.
- Fail over from MSG91 to AWS SNS when the primary provider reports errors.
- Expose statistics for dashboards and tooling via `NotificationService.getNotificationStatistics`.
- Offer immediate-send helpers for support tooling or urgent delivery flows.

## Feature Highlights
- Tenant-aware queueing (`ownerId`) so notifications can be partitioned per workspace.
- Configurable batch size, retry delay, cleanup window, and primary provider (via `notification.*` properties).
- Parallel processing using an executor injected via `@Qualifier("notificationTaskExecutor")`.
- Retry logic that re-queues failed notifications and tracks `NotificationStatus` (PENDING → RETRYING → SENT/EXHAUSTED).
- Optional delayed scheduling by specifying a delay when queueing.
- Legacy convenience methods (`queueSms`, `/sms/*` endpoints) preserved for older clients.

## API Highlights
| Endpoint | Description |
|----------|-------------|
| `GET /notification/v1/stats` | Aggregate queue statistics (overall counts, totals per channel). |
| `GET /notification/v1/sms/stats` | SMS-specific stats for backward compatibility. |
| `POST /notification/v1/test?recipient=&message=&channel=` | Queue a test notification (defaults to SMS). |
| `POST /notification/v1/sms/test?phoneNumber=&message=` | Legacy helper that queues an SMS. |
| `POST /notification/v1/send/immediate` | Attempt to send immediately, returning provider result metadata. |

The controller returns `ApiResponse<T>` for stats and raw map payloads for test helpers to keep compatibility with existing clients.

## Integration Points
- **Core** – Uses tenant context helpers and shared API envelopes; scheduled processing uses Spring scheduling provided by the platform.
- **Workspace/Auth** – Notification endpoints require JWT auth; workspace headers determine ownership for queue entries.
- **External Providers** – MSG91 and AWS SNS drivers honour property-based configuration for credentials and sender IDs.
- **Event Module** – Downstream services can listen to notification audits if additional instrumentation is added.

## Build & Test
```bash
# From ampairs-backend/
./gradlew :notification:build
./gradlew :notification:test
```

Run `./gradlew :ampairs_service:bootRun` to exercise the notification APIs and scheduled workers with the full service.

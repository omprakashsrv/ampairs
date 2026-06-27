# communication module

Generic, channel-agnostic messaging orchestration — **transactional**, **recurring**, and
**promotional** sends over email, SMS, WhatsApp, and push. Orchestrates only; actual per-message
delivery is delegated to the `notification` module. Workspace-scoped (`OwnableBaseDomain` +
`X-Workspace-ID`). Spec/plan: `specs/015-generic-communication-module/`.

## Architecture

```
event (Invoice/Order created)  ─┐
POST /communication/v1/requests ─┼─→ communication (templates, audience, schedules, campaigns,
ScheduleSweeper (recurring)    ─┤      consent, usage)  ──enqueue──→ notification (queue, retry,
CampaignRunner (promotional)   ─┘                                    providers, credentials)
                                          ▲ delivery-status event ◀────────────┘
```

- **Send engine** (`service/send/CommunicationDispatchService`): selects the channel/locale variant,
  renders (logic-less `{{var}}`), skips no-address / no-variant / **suppressed** recipients, enqueues
  via `notification.NotificationDispatchService`, and (on the returned delivery event) updates the log
  + writes the **usage ledger** row. Idempotent per `dedup_key`.
- **Triggers**: transactional (`event` `@EventListener` + manual API), recurring (`ScheduleSweeper`,
  business-tz, at-most-once via the occurrence ledger), promotional (`CampaignRunner`, consent/quiet
  hours/throttle).
- **Billing**: every send is attributed to a workspace credential + `billing_mode` (CLIENT_OWN vs
  PLATFORM) and aggregated by `GET /communication/v1/usage`.

## Base path & endpoints (`/communication/v1`)

| Endpoint | Purpose |
|---|---|
| `…/templates/sync` | aggregate `/sync` (header + variants; delete-by-absence; base_version) |
| `…/templates/{code}/preview` | render with sample data |
| `…/bindings/sync`, `…/schedules/sync`, `…/campaigns/sync`, `…/preferences/sync` | standard `/sync` |
| `…/logs/sync` | pull-only delivery status |
| `…/requests` | manual/transactional send |
| `…/campaigns/{uid}/start\|pause\|resume` | campaign lifecycle |
| `…/credentials`, `…/credentials/{uid}/validate` | workspace provider credentials (write-only secrets) |
| `…/usage` | per-channel × credential × billing-mode report |
| `…/unsubscribe` 🌐 | public token-scoped opt-out |

## Migrations
`communication` V1.0.105 (12 tables), V1.0.109 (schedule timezone). `notification` V1.0.106 (queue
columns), V1.0.108 (`workspace_channel_credential`). `customer` V1.0.107 (`locale`).

## Config (env)
`COMM_CRED_ENCRYPTION_KEY` (AES-GCM master key for credential secrets), `notification.email.*` (platform
SMTP), `notification.whatsapp.*` (Cloud API defaults), `communication.scheduler.tick-seconds`,
`communication.campaign.default-throttle-per-minute`, `communication.unsubscribe.secret`.

## Cross-module
Consumes `customer` (`CustomerContactProvider` for audiences), `event` (domain events), `notification`
(dispatch + credentials). Enriched `InvoiceCreatedEvent`/`OrderCreatedEvent` with `customerId`.

## Status (spec 015)
Backend US1–US4 + US6 + foundation implemented and unit-tested. Pending: mobile `feature/communication`
(US5, `ampairs-app` repo), live Flyway validation against a DB (T007).

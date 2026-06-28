# communication module

Generic communication orchestration — multi-channel (email/SMS/WhatsApp/push) transactional,
recurring, and promotional messaging. Orchestrates; delegates actual delivery to the `notification`
module via `NotificationDispatchService`. Workspace-scoped (`OwnableBaseDomain` + `X-Workspace-ID`).

Spec/plan: `specs/015-generic-communication-module/`.

## What it owns
- Templates (`message_template` + `message_template_variant`) — aggregate `/sync`, server-rendered
  HTML email + plain-text alt, logic-less `{{var}}` rendering (`TemplateRenderer`).
- Sends (`communication_request` → `communication_log` fan-out), usage ledger (`communication_usage`).
- Event→template bindings (`event_template_binding`) for transactional triggers.
- Schedules / campaigns / preferences / suppression / config (later phases).

## Base path
`/communication/v1/**` — templates(aggregate)/bindings/schedules/campaigns/preferences `/sync`;
logs `/sync` pull-only; `POST /requests` (manual send); `/templates/{code}/preview`.

## Dispatch bridge
`communication` → `notification.NotificationDispatchService.enqueue(DispatchRequest)` → notification
queue/providers. Delivery feedback returns via `NotificationDeliveryUpdatedEvent` →
`NotificationDeliveryListener` (updates the log + writes the usage row; status is monotonic).

## Migrations
`V1.0.105` (communication tables) + notification `V1.0.106` (queue columns).

## Implementation status (spec 015)
**Backend complete** (US1–US4, US6 + foundation): templates/preview, manual + transactional sends
(event-driven via `customerId`), Email + WhatsApp providers, per-workspace credentials (AES-GCM,
write-only) + resolver, usage/billing report, recurring schedules (business-tz sweeper, at-most-once),
promotional campaigns (consent gate, quiet hours, throttle, suppression), preferences `/sync`, public
unsubscribe, provider webhooks. All unit-tested.
Pending: mobile `feature/communication` (US5, `ampairs-app` repo); live Flyway validation against a
DB (T007); security allow-list for the public unsubscribe/webhook paths is wired in
`ampairs_service` `SecurityConfiguration.PUBLIC_PATHS`.

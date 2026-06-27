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

## Implementation status (MVP, spec 015 Phase A)
Done: module + schema (both vendors) + core entities/repos + renderer + template/binding/log `/sync`
+ preview + manual send (`POST /requests`) + dispatch engine + delivery listener + usage ledger +
transactional listener wiring. Pending: live Email/WhatsApp providers + per-workspace credential
foundation (T014–T019), recurring sweeper (US2), campaigns/consent (US3), and the customer
contact-resolution provider that the transactional listener's recipient resolution depends on.

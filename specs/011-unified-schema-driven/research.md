# Phase 0 Research: Unified Schema-Driven Dynamic Forms

All Technical Context entries are known from the existing codebases; no `NEEDS CLARIFICATION`
remained after `/speckit.clarify`. This document records the **design decisions** that the plan and
contracts depend on.

---

## D1 — Storage unification: physically merge into one table, keep two as legacy adapters

**Decision**: Create a single `form_field` table (with a `source` discriminator `STANDARD|CUSTOM`) and
a new `form_section` table. Backfill existing `field_config` and `attribute_definition` rows into
`form_field`. The two old tables remain in place but are **no longer written by new code**; the legacy
`/sync` endpoints become read/write adapters over `form_field` (mapping `source=STANDARD` ↔ field-config
shape, `source=CUSTOM` ↔ attribute shape) so older app builds keep working.

**Rationale**: One table = one DTO, one DAO, one sync feed, one UI card — the core goal. Physically
unifying (vs a logical view over two tables) removes the duplication permanently and lets the unified
`/sync` feed and renderer treat every field identically. Backfill preserves all workspace customizations
(SC-008, FR-025).

**Alternatives rejected**:
- *Logical unification (DB view over two tables)*: keeps the split storage and its drift/maintenance cost;
  soft-delete and section FKs become awkward across two tables.
- *Big-bang cutover (drop legacy feeds immediately)*: breaks every app build older than this release,
  violating FR-026. The adapter approach is the only safe incremental path.

---

## D2 — Soft-delete via `active: Boolean` (in-band delete on `/sync`)

**Decision**: `form_field` and `form_section` carry `active: Boolean = true` (soft-delete flag) plus
`updated_at: Instant`. The unified `/sync` pull feed **includes** `active = false` rows; push carries
them in-band; the client hard-deletes locally on pull. This closes the documented "known gap".

**Rationale**: Matches the canonical offline-sync contract exactly (guide §Rules 3–4) and the rest of the
syncable entities, so the client's generic `SyncDelegate` drives it unchanged. `active` (not a new
`status` enum) matches the app's existing soft-delete idiom (`active = false, synced = false`).

**Alternatives rejected**: `status = DELETED` string column — heavier than needed and inconsistent with
sibling entities. Hard delete only — cannot propagate deletions (the original bug).

**Constraint from spec**: STANDARD fields cannot be deleted (Assumptions); only CUSTOM fields and
sections may be soft-deleted. Enforced in the service.

---

## D3 — Standard Field Registry SPI replaces hardcoded seeding

**Decision**: Define `StandardFieldProvider` (interface in `form` module) with
`entityType(): EntityType` and `standardFields(): List<StandardFieldSpec>`. Each domain module
(`customer`, `product`, `order`, `invoice`, `workspace`/business) provides a `@Component`
implementation describing its built-in fields. `FormFieldRegistry` aggregates all providers; the service
uses it to (a) seed/merge defaults for a workspace and (b) validate that any `STANDARD` field's
`fieldName` and `entityType` actually exist. Deletes the ~700-line hardcoded seeding in `ConfigService`
and the app's 530-line `DefaultFormConfigs.kt`.

**Rationale**: One authoritative source per domain (FR-020/021), owned by the domain itself (module
boundaries: cross-module via public service interface, no cross-repository access). Adding a field later
is a one-line provider change and merges into existing configs without wiping customizations (FR-022).

**Alternatives rejected**: Reflection over JPA entities — fragile, can't express labels/sections/order
intent. Central hardcoded list in `form` — recreates the coupling we're removing.

---

## D4 — Dynamic choice options via named source bindings

**Decision**: A CHOICE field's options come from a `ChoiceOptionSource`: either `STATIC` (an
admin-entered `enumValues` list) or `DYNAMIC` (a `sourceKey` string, e.g. `customer_types`,
`tax_codes`, `units`). On the app, a `DynamicOptionProvider` registry maps `sourceKey →
Flow<List<Option>>`; each owning domain registers its providers (Metro multibinding). The backend stores
only the binding; it does not resolve dynamic options (the client owns that live data).

**Rationale**: The codebase rule "load dropdowns from repositories, never hardcoded enums" plus existing
forms (customer types/groups, tax codes, units) make dynamic options mandatory for the renderer to
*replace* real forms (SC-001/007). Storing a `sourceKey` keeps the schema declarative and offline-safe;
resolution happens client-side against already-synced data.

**Alternatives rejected**: Backend resolves and embeds option values — couples `form` to every domain's
data, breaks offline freshness, violates module boundaries. Static-only — can't subsume real forms.

---

## D5 — Custom-widget escape hatch

**Decision**: Add a `CUSTOM` field data type carrying a `widgetKey` (e.g. `image_gallery`, `address`,
`location`, `business_hours`). The renderer looks up `widgetKey` in a `CustomFieldWidget` registry
(Metro multibinding) that domains populate with native composables. Visibility, required, order, and
section for these fields stay config-driven; only the input control is domain-supplied. The renderer
still assembles the whole form (no domain appends fields outside it).

**Rationale**: Makes SC-001 ("zero hand-built layouts") literally achievable for forms that contain
image galleries, structured addresses, maps, and business hours, without forcing those into generic
types. Keeps the renderer generic and the complex controls reusable.

**Alternatives rejected**: Exclude complex fields (Story-5/SC-001 unmet for several domains); decompose
into primitive fields (loses purpose-built UX, e.g. map picker).

---

## D6 — Shared, typed validation model

**Decision**: `ValidationRule` is a typed, serializable model — `Required`, `LengthRange(min,max)`,
`NumberRange(min,max)`, `Format(EMAIL|PHONE|GSTIN|PAN|...)`, `AllowedChoices`. Stored as structured JSON
(not free-form strings). A `ValidationEngine` with identical rule semantics exists on both sides: the app
renderer enforces inline at entry; the backend re-validates on push (defense in depth). The admin editor
builds rules via guided controls (FR-004/FR-013) — no raw regex/JSON typing for everyday rules (an
advanced `Format` may expose a curated pattern picker, not free text).

**Rationale**: One rule vocabulary shared by client and server prevents drift between what the editor
offers, what the form enforces, and what the backend accepts. Typed rules are testable (FR-002/004) and
keep validation params out of opaque JSON blobs.

**Alternatives rejected**: Free-form regex/expression strings (current state) — un-guidable in UI,
unsafe, untestable. Client-only validation — backend would accept invalid pushes from old/buggy clients.

---

## D7 — App storage & sync delegate (fresh schema, two feeds, one checkpoint)

**Decision**: `FormDatabase` is introduced with the unified `form_field`/`form_section` schema directly
(fresh setup — the old `entity_field_configs`/`entity_attribute_definitions` tables are removed, no row
copy; see D1). `FormSyncDelegate` drives **two feeds — `sections/sync` then `fields/sync` — under one
`SyncEntity.FORM` checkpoint** (= `max(updatedAt)` across both tables, what `FormCheckpointContributor`
already computes). Soft-delete aware, batches of 100 per feed, local-unsynced-wins. Repository stays
local-only (`markPendingPush(FORM)`); `getConfigSchema` read remains the allowed UI-invoked exception.

**Why two feeds, not one record stream**: sections and fields are genuinely different shapes; a
discriminated union record forces half-null payloads and an awkward `record_type`. Two clean canonical
feeds under one logical entity is the pattern the form module already used (it synced two feeds), keeps
each DTO clean, and lets each feed be a plain `updated_at >= last_sync` paged query. Pagination uses a
stable `(updated_at, uid)` sort so equal-timestamp bulk saves don't skip rows at page boundaries.
**Section-detail updates** only bump the section row (fields reference it by `uid` and re-group on the
client) — see contract §1.

**Alternatives rejected**: *Single discriminated `/schema/sync` feed* — half-null mega-record, ugly and
error-prone. *Two `SyncEntity` checkpoints (one per feed)* — unnecessary; one `FORM` checkpoint over both
tables is simpler and already supported. *Wipe-and-repull on every change* — loses unsynced local edits.
(Note: "two feeds" here means sections vs fields — NOT the old standard-vs-custom split, which is gone.)

---

## D8 — Rollout sequencing

**Decision**: Customer is the reference implementation (full renderer + registry + dynamic options +
at least one custom widget — the image gallery). Then product, order, invoice, business. Each domain is
shippable independently; the backend serves all entity types regardless of which app screens have
migrated, and legacy adapters cover un-migrated clients.

**Rationale**: Proves the model and renderer end-to-end on the richest existing form before broad
rollout (FR-023/026); de-risks via one vertical slice.

---

## Open items deferred to `/speckit.tasks` / implementation

- Exact next Flyway version number — resolve via `./gradlew :ampairs_service:flywayInfo` at task time.
- Whether business-hours/address widgets are extracted into reusable composables now or reused in place.
- Removal timing of the legacy `/sync` adapters (a follow-up cleanup feature once all clients update).

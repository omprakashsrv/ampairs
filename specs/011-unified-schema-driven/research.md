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

## D2 — Deletions propagate by aggregate replacement (no soft-delete column)

**Decision**: Because the schema syncs as a whole aggregate (D9), there is **no `active`/soft-delete
column** on any form row. A removed field/section is simply **absent** from the next aggregate; on pull
the client replaces its local schema for that entityType and drops absent members; on push the server
replaces the aggregate (upsert present, delete absent) in one transaction. This closes the documented
"known gap" (deletes didn't round-trip) trivially.

**Rationale**: At aggregate grain, delete-by-absence is simpler and more robust than per-row soft-delete
+ in-band delete + "include deleted rows" feeds. It removes a whole class of machinery and the
field/section ordering and dangling-reference concerns.

**Alternatives rejected**: Row-level soft-delete (`active` flag, in-band delete) — the canonical row-level
pattern; correct, but unnecessary once the aggregate is the transfer unit, and it carried the ordering /
dangling-ref complexity. (This reverses an earlier draft that added an `active` column.)

**Constraint from spec**: STANDARD fields cannot be removed (Assumptions); only CUSTOM fields and
non-`General` sections may be removed. Enforced as an aggregate invariant in the service.

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

**Decision**: A `CHOICE` / `MULTI_CHOICE` field's options come from a `ChoiceOptionSource`: either `STATIC` (an
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

## D7 — App storage & sync delegate (fresh schema, single aggregate feed)

**Decision**: `FormDatabase` is introduced with the unified `form_schema`/`form_section`/`form_field`
schema directly (fresh setup — old tables removed, no row copy; see D1). `FormSyncDelegate` drives the
**single aggregate feed** under one `SyncEntity.FORM` checkpoint (= `max(updated_at)` across aggregates).
**Pull replaces** each local aggregate — local members absent from the server copy are deleted; **push**
sends the dirty aggregate(s) with `base_version`; on a version conflict the delegate re-pulls, re-applies
the local edits, and retries. Dirtiness is tracked **per entityType aggregate** (no per-row `synced`).
Repository stays local-only (`markPendingPush(FORM)`); `getConfigSchema` read remains the allowed
UI-invoked exception. Persistence is relational; the aggregate is assembled/serialized at the sync
boundary.

**Why one aggregate feed**: the schema is one DDD aggregate, so it transfers as one unit (D9). This
removes the soft-delete machinery, the sections-before-fields ordering, and the dangling-`section_uid`
window — the whole aggregate always arrives together.

**Alternatives rejected**: *Two row-level feeds (sections + fields)* — preserves field-level merge but
needs soft-delete, ordering, and dangling tolerance; unnecessary once the aggregate is the transfer unit.
*Single discriminated record stream* — half-null mega-record. *Wipe-and-repull on every change* — loses
unsynced local edits (the replace is per-aggregate, and local unsynced aggregates win until pushed).

---

## D8 — Rollout sequencing

**Decision**: Customer is the reference implementation (full renderer + registry + dynamic options +
at least one custom widget — the image gallery). Then product, order, invoice, business. Each domain is
shippable independently; the backend serves all entity types regardless of which app screens have
migrated (fresh setup, clean cutover — no legacy adapters).

**Rationale**: Proves the model and renderer end-to-end on the richest existing form before broad
rollout (FR-023/026); de-risks via one vertical slice.

---

## D9 — `FormSchema` DDD aggregate is the consistency AND the sync unit

**Decision**: Model the per-`(workspace, entityType)` schema as a **`FormSchema` aggregate root**:
`Section` owns `Field`, every field belongs to exactly one section (mandatory ownership; a seeded default
`General` section), and all invariants (unique keys, order, section-not-empty-on-delete, essential-not-
hideable, CHOICE/MULTI_CHOICE/CUSTOM rules) are enforced at the aggregate boundary — on the editor save
**and** on every push. **The aggregate is also the transfer unit**: one `FormSchema` record per
entityType (uid = entityType) over a single `/config/schema/sync` GET+POST. Persistence stays relational
(`form_schema` header with `version` + `form_section` + `form_field`), assembled into/from the aggregate
at the sync/read boundary.

**Rationale**: True to DDD — the aggregate is the natural domain, editing, consistency, **and** transfer
boundary. One feed instead of four endpoints; deletions are free (absence, D2); no orphan/dangling fields
by construction; no ordering concerns. Concurrency is whole-form last-write-wins guarded by an optimistic
`version` stamp: a stale push (client `base_version` < server `version`) is rejected so the client
re-pulls and re-applies — no silent lost update. For an admin-only, low-frequency config this is the right
trade vs. field-level merge.

**Alternatives rejected**: *Aggregate for editing but row-level distribution* (two feeds) — preserves
field-level merge but reintroduces soft-delete, ordering, and dangling tolerance; rejected for the extra
machinery once concurrent same-form editing was deemed rare. *No aggregate (flat rows)* — scatters
invariant enforcement and allows orphan fields. (This supersedes an earlier draft that kept row-level
distribution; FR-018 is correspondingly relaxed to aggregate-level LWW + optimistic version.)

---

## Open items deferred to `/speckit.tasks` / implementation

- Exact next Flyway version number — resolve via `./gradlew :ampairs_service:flywayInfo` at task time.
- Whether business-hours/address widgets are extracted into reusable composables now or reused in place.
- Removal timing of the legacy `/sync` adapters (a follow-up cleanup feature once all clients update).

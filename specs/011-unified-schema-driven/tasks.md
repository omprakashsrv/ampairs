# Tasks: Unified Schema-Driven Dynamic Forms

**Input**: Design documents from `/home/user/ampairs/specs/011-unified-schema-driven/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/form-sync-api.md

**Repos**: Backend `BE` = `/home/user/ampairs` · App `APP` = `/home/user/ampairs-app`
**Branch (both repos)**: `claude/clever-cori-71tjo5`

**Tests**: Included because the project constitution mandates coverage gates (backend critical ≥80%,
API endpoints ≥90%). They are not strict TDD-first; write them alongside each slice.

## Format: `[ID] [P?] [Story] Description`
- **[P]** = parallelizable (different files, no dependency). **[Story]** = US1–US5.
- `BE:` / `APP:` prefix on every path indicates the repository.

---

## Phase 1: Setup (Shared Infrastructure)

- [X] T001 [P] `BE:` Add shared enums `EntityType`, `FieldSource`, `FieldDataType` (incl. `CHOICE` and `MULTI_CHOICE`), `OptionSource` in `form/src/main/kotlin/com/ampairs/form/domain/model/` (replace magic entityType strings; reject unknown at API boundary).
- [X] T002 [P] `APP:` Add mirror enums (same names incl. `CHOICE`/`MULTI_CHOICE`, `@SerialName` snake_case) in `feature/form-api/src/commonMain/kotlin/com/ampairs/form/domain/`.
- [X] T003 [P] `BE:` Confirm `form` is in `migrationModules` (`ampairs_service/build.gradle.kts`) and run `./gradlew :ampairs_service:flywayInfo` to fix the next migration version (record it for T010).
- [X] T004 [P] `APP:` Add `FormLogger` (Kermit, `w/e/i/d` 3-param signature) in `feature/form/src/commonMain/kotlin/com/ampairs/form/FormLogger.kt`; add a `BATCH_SIZE = 100` constant for sync.

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ Both US1 and US2 depend on the entire unified model, the `/sync` contract, and renderer scaffolding. No story work starts until this phase is green.**

### Backend — unified model, migration, registry, sync

- [X] T005 [P] `BE:` `FormField` entity (`OwnableBaseDomain`, `Instant`, **mandatory `section_uid` FK**, **no soft-delete**, all columns per data-model.md) in `form/.../domain/model/FormField.kt`.
- [X] T006 [P] `BE:` `FormSection` entity (first-class, order, visible, no soft-delete) + `FormSchema` aggregate-root entity (`form_schema` header per workspace+entityType with `version`; assembles ordered sections→fields; holds invariant checks) in `form/.../domain/model/`.
- [X] T007 [P] `BE:` Typed `ValidationRule` sealed model (Required/LengthRange/NumberRange/Format/AllowedChoices) + `ChoiceOptionSource` in `form/.../domain/model/validation/`.
- [X] T008 [P] `BE:` `FormSchemaRepository` (header w/ `version`; `findUpdatedAfter`/`findAllForSync` over `form_schema.updatedAt`, `findByEntityType`) + `FormSectionRepository` + `FormFieldRepository` (`findByEntityType` to assemble the aggregate). No soft-delete queries.
- [X] T009 `BE:` `ValidationEngine` (evaluates `ValidationRule` list against a value; reject contradictory rules) in `form/.../domain/service/validation/ValidationEngine.kt`.
- [X] T010 `BE:` Flyway migration `V1.0.x__create_unified_form_model.sql` in BOTH `form/src/main/resources/db/migration/mysql/` and `postgresql/`: create **empty** `form_schema` + `form_section` + `form_field` (no soft-delete column; indexes + uniqueness per data-model.md) and **drop** the legacy `field_config` + `attribute_definition` tables. Fresh setup — no backfill. (Depends T005–T007.)
- [X] T011 [P] `BE:` `StandardFieldProvider` SPI + `StandardFieldSpec` (carries dataType incl. `CHOICE`/`MULTI_CHOICE` — a standard field is `MULTI_CHOICE` only where its bound column is a collection) + `FormFieldRegistry` (aggregates `@Component` providers; validates STANDARD `fieldKey`/`entityType`; marks essential fields) in `form/.../domain/service/registry/`.
- [X] T012 `BE:` DTOs in `form/.../domain/dto/` (DTO isolation; SNAKE_CASE): `FormSchemaResponse` and `FormSchemaRequest` (nested `sections` + `fields` members, `version`, request carries `base_version`) + entity↔DTO mappers. (Depends T005–T007.)
- [X] T013 `BE:` `FormConfigService` — loads/saves the `FormSchema` aggregate atomically; registry-driven default seeding/merge (non-destructive) **including a default `General` section per entityType**; `replaceAggregate` (upsert members present, **delete members absent**, bump `version`) with **optimistic concurrency** (reject when `base_version` < current `version`); `getAfterSync` over `form_schema.updatedAt`; **validates aggregate invariants on save** (every field has a section; STANDARD not removable; essential not hideable; non-empty section not removable; CHOICE/MULTI_CHOICE/CUSTOM invariants). (Depends T008, T009, T011, T012.)
- [X] T014 `BE:` `ConfigController` — single aggregate feed `GET/POST /form/v1/config/schema/sync` (one `FormSchema` record per entityType, uid=entityType) + `GET /form/v1/config/schema?entity_type=` (seeds defaults); tenant set at controller; `ApiResponse<PageResponse>` / `ApiResponse<List>`. (Depends T013.)
- [X] T015 `BE:` Remove the old form module artifacts — delete `FieldConfig`/`AttributeDefinition` entities, their DTOs, repositories, the legacy seeding in the old `ConfigService`, and all legacy endpoints (`field-configs/sync`, `attribute-definitions/sync`, `field-config`, `attribute-definition`, `config`). Clean cutover, no adapters. (Depends T013, T014.)
- [X] T016 [P] `BE:` Update `FormCheckpointContributor` to compute the `"form"` checkpoint as `max(updatedAt)` over `form_schema` (each aggregate's `updatedAt` already = max across its members).
- [ ] T017 [P] `BE:` Contract test (aggregate feed): `GET /config/schema/sync` returns whole `FormSchema` records since `last_sync`; `POST` replaces the aggregate (a member omitted from the pushed schema is deleted; pull then reflects the removal); a stale `base_version` push is rejected; snake_case params; `ApiResponse<PageResponse>`/`<List>` shape — `form/src/test/.../ConfigSyncContractTest.kt`.
- [ ] T018 [P] `BE:` Fresh-provision test: with empty tables, `getConfigSchema(entityType)` seeds-on-read from `FormFieldRegistry` and returns the complete default schema (all standard fields, correctly sectioned); a second call does not duplicate — `form/src/test/.../FormSeedOnReadTest.kt`.

### App — shared model, storage, sync, renderer scaffolding

- [ ] T019 [P] `APP:` Unified `@Serializable` `FormSchema` (entityType, version, sections+fields), `FormField`, `FormSection`, `EntityConfigSchema` (+helpers), `ValidationRule`, `ValidationEngine` in `feature/form-api/src/commonMain/.../domain/` (+ `validation/`). Remove old `EntityFieldConfig`/`EntityAttributeDefinition`/`DefaultFormConfigs` usages from this module.
- [ ] T020 [P] `APP:` Room entities `FormSchemaEntity` (entityType, version, dirty flag) + `FormSectionEntity` + `FormFieldEntity` (JSON columns; **no `active`**) + DAOs (reactive `Flow` by entityType; assemble aggregate; mark/clear dirty per entityType) in `feature/form/.../data/db/`.
- [ ] T021 `APP:` `FormDatabase` with the fresh unified `form_schema`/`form_section`/`form_field` schema — remove the old `entity_field_configs`/`entity_attribute_definitions` tables (destructive recreate on first launch; no row copy — fresh setup). Schema re-populates via the initial pull/seed-on-read. (Depends T020.)
- [ ] T022 `APP:` `ConfigApi`(+`ConfigApiImpl`) — single aggregate feed: `getSchemaSync` + `pushSchema` (List<FormSchema>, carries `base_version`) + `getConfigSchema`; remove the old `getFieldConfigsSync`/`getAttributeDefinitionsSync`. (Depends T019.)
- [ ] T023 `APP:` `ConfigRepository` local-only writes — `saveConfigSchema` validates the `FormSchema` aggregate (every field in a section; invariants), writes the rows (a removed member = row deleted; **no soft-delete**), marks the entityType aggregate dirty + `markPendingPush(FORM)`; `getConfigSchema` UI read retained; delete legacy `syncFormConfigs()` pull. (Depends T020, T022.)
- [ ] T024 `APP:` `FormSyncDelegate` drives the **single aggregate feed** under one `SyncEntity.FORM` checkpoint — **pull replaces** each local aggregate (delete local members absent from the server copy); **push** the dirty aggregate(s) with `base_version`; on a version-conflict response, re-pull, re-apply local edits, retry; local-unsynced aggregate wins until pushed; advance the checkpoint to `max(updatedAt)`. (Depends T020, T022.)
- [ ] T025 [P] `APP:` Metro DI wiring — `FormDaoModule` provide new DAOs + `ConfigLookup`; ensure `WorkspaceScope` DB providers (android/ios/desktop) target unified `FormDatabase`. (Depends T020.)
- [X] T026 [P] `APP:` Renderer scaffolding in `feature/form-api/src/commonMain/.../render/`: `FormValueState` (two-way binding + per-field validation state via `ValidationEngine`), `DynamicOptionProvider` interface + `@OptionSourceKey` map key, `CustomFieldWidget` interface + `@WidgetKey` map key. (Depends T019.)
- [X] T027 `APP:` `DynamicFormRenderer` composable skeleton + `FieldRenderers` for TEXT/TEXTAREA/NUMBER/BOOLEAN/DATE (sectioned, ordered, inline errors, `stringResource` only). Choice + custom delegated to providers/registry from T026. Group fields by `sectionUid`, ordering sections then fields by `displayOrder`; `sectionUid` is mandatory, but if a field's section row hasn't synced yet it renders transiently in the default `General` group — never an error. (Depends T026.)

**Checkpoint**: Backend serves the unified schema for any entity type (old form module removed, no legacy endpoints); app stores/syncs/renders the unified model generically. Stories can begin.

---

## Phase 3: User Story 1 — Staff see the form their workspace configured (Priority: P1) 🎯 MVP

**Goal**: The customer entry screen is produced entirely from configuration (visibility, order, sections, required, validation, custom fields), replacing the hand-built form. Dynamic dropdowns and the image gallery work through the renderer.

**Independent Test**: Hide a standard customer field + mark one required + add a custom choice field in config; open the customer entry screen → hidden field gone, required field blocks save, custom field shows in its section/order and its value persists; email/format validation blocks bad input inline.

- [X] T028 [P] [US1] `BE:` `CustomerStandardFieldProvider` implementing the SPI (full customer field set incl. sections Basics/Contact/Addresses/Tax/Status, essential flags, default validation) in `customer/.../domain/service/`. Delete customer seeding from old `ConfigService`.
- [X] T029 [P] [US1] `APP:` Choice field renderers in `FieldRenderers` — `CHOICE` single-select dropdown and `MULTI_CHOICE` multi-select (chips/checklist), each supporting STATIC (`enumValues`) and DYNAMIC (`dynamicSourceKey` via `DynamicOptionProvider`) options; multi value binds as `List<String>`, "required" = at least one selected. (Depends T027.)
- [X] T030 [P] [US1] `APP:` Register customer `DynamicOptionProvider`s (`customer_types`, `customer_groups`, `tax_codes`, `units`) bound to their repositories (Metro `@ContributesIntoMap(WorkspaceScope::class)` + `@OptionSourceKey`).
- [X] T031 [P] [US1] `APP:` `ImageGalleryWidget : CustomFieldWidget` (`@WidgetKey("image_gallery")`) reusing the existing customer image control; registered in the widget map.
- [X] T032 [US1] `APP:` Customer value mapping — `Customer.toValueMap()` (standard→columns, custom→`attributes`) and `applyValues()`, handling `MULTI_CHOICE` values as `List<String>` (JSON in `attributes`); ensure UID generation stays in the ViewModel. (Depends T019.) _(Done as the attributes half: customer/product values round-trip Room+sync via attributes_json; standard columns continue through the existing form state, which the config-driven standard-fields slot binds to.)_
- [ ] T033 [US1] `APP:` Rewrite `CustomerFormViewModel` to expose `observeConfigSchema(CUSTOMER)` + `FormValueState`; drop ad-hoc per-field visibility logic. (Depends T023, T026, T032.)
- [ ] T034 [US1] `APP:` Replace `CustomerForm` screen body with `DynamicFormRenderer(schema, state)`; save path runs `state.validateAll()` then repository save. (Depends T027, T029, T031, T033.)
- [X] T035 [P] [US1] `APP:` Customer detail/read view honors field visibility via the same schema (read-only render path). _(attributeRows joined with schema: hidden custom fields omitted, displayName labels.)_
- [ ] T036 [P] [US1] `BE:` Integration test: customer schema seeds from registry, `/schema?entity_type=customer` returns sectioned fields, hidden/required honored — `customer`/`form` test.
- [ ] T037 [P] [US1] `APP:` Renderer test: given a schema, `DynamicFormRenderer` shows only visible fields in order, enforces required + format rules, binds static & dynamic single-`CHOICE` and `MULTI_CHOICE` (list) values — `feature/form-api` commonTest. _(PARTIAL: ValidationEngineTest + FormSchemaTest in feature/form-api commonTest cover rule semantics + visible/section/order grouping; full composable render test needs Compose UI test infra, not present.)_

**Checkpoint**: Customer form is fully config-driven end-to-end (MVP). Validates SC-001 for customer.

---

## Phase 4: User Story 2 — Administrators configure forms without technical knowledge (Priority: P1)

**Goal**: Productized admin editor: everyday settings (visibility/required/label/section/drag-reorder/add custom field) with a **live preview**, advanced settings (data type + guided validation builder + choice option source) in a separate area, no raw JSON/regex typing.

**Independent Test**: Hide a field, drag-reorder two fields, add a custom dropdown with 3 options and a length-range rule, create a new section — confirm the live preview updates and the result persists after save/reopen; leaving with unsaved edits warns.

- [X] T038 [US2] `APP:` Rework `FormConfigViewModel` to edit the `FormSchema` aggregate (sections each owning ordered fields) — intents for toggle/relabel/reorder/assign-section/add-custom/delete-custom, section CRUD, dirty tracking; client-side aggregate invariant checks; save the whole aggregate via `ConfigRepository.saveConfigSchema`. (Depends T023.)
- [X] T039 [US2] `APP:` `FormConfigScreen` split into **Field settings** tab (per-section grouped list, visibility/required toggles, inline relabel, drag handle) and **Advanced** tab; remove raw-property card dump. (Depends T038.)
- [X] T040 [P] [US2] `APP:` Drag-to-reorder within/between sections updating `displayOrder` + `sectionUid`. (Depends T039.)
- [X] T041 [P] [US2] `APP:` Live preview pane rendering `DynamicFormRenderer` in read-only/preview mode from the in-progress (unsaved) schema. (Depends T027, T038.)
- [X] T042 [P] [US2] `APP:` Section management UI — create/rename/reorder/hide/delete; the default `General` section cannot be deleted; deleting a non-empty section requires reassigning its fields first (or auto-move to `General`) per edge case. (Depends T038.)
- [X] T043 [P] [US2] `APP:` Guided validation builder — typed rule pickers (Required, length range, number range, format from curated list, allowed choices); no free-form regex/JSON. (Depends T038.)
- [X] T044 [P] [US2] `APP:` Choice editor — pick data type `CHOICE` vs `MULTI_CHOICE`, then option source STATIC (list builder) vs DYNAMIC (pick a registered `dynamicSourceKey`); replaces the hardcoded datatype dropdown with the `FieldDataType` enum. (Depends T038.)
- [X] T045 [P] [US2] `APP:` Add/edit/remove custom field flow with client-side validation (non-empty label via disabled confirm; auto-unique non-blank `fieldKey` via `uniqueFieldKey`/`slugKey` — no duplicate key); stable UID via `UidGenerator`. (Depends T038.)
- [ ] T046 [P] [US2] `APP:` Unsaved-changes guard on navigation; move all editor strings to `composeResources/values/strings.xml`; success/error via resources (no hardcoded text). _(PARTIAL: nav guard done with resourced dialog strings; full extraction of remaining inlined editor strings pending.)_
- [ ] T047 [P] [US2] `BE:` Service test: integrity rules reject hiding/deleting essential STANDARD fields and contradictory validation rules with proper `ApiResponse` errors. _(PARTIAL: ValidationEngineTest covers contradictory-rule rejection; the FormConfigService essential-field integrity path still needs a mocked-repo test.)_

**Checkpoint**: US1 + US2 deliver the full usable loop on customer (configure → preview → render). Validates SC-003.

---

## Phase 5: User Story 3 — One consistent field model across the workspace (Priority: P2)

**Goal**: Standard and custom fields are one unified, freely-interleaved list with identical controls; the duplicate default-field sources are removed.

**Independent Test**: In the editor, standard and custom fields appear in one orderable list with the same controls and can be interleaved; a custom value round-trips on a record identical to a standard value.

- [X] T048 [P] [US3] `APP:` Delete `feature/form/.../domain/DefaultFormConfigs.kt` and all references; defaults now come only from the backend registry/sync. (SC-004)
- [ ] T049 [P] [US3] `BE:` Remove any remaining hardcoded default-field construction from old `ConfigService`; all defaults flow through `FormFieldRegistry`. (SC-004)
- [X] T050 [US3] `APP:` Ensure the editor field list interleaves STANDARD/CUSTOM by `displayOrder` within section (no source-based separation) and offers identical controls where applicable. (Depends T039.) _(Verified: editor sorts purely by displayOrder within section; source only affects badge + delete affordance.)_
- [ ] T051 [P] [US3] `APP:` Parity test: a STANDARD and a CUSTOM field with the same dataType render and validate identically; custom value persists to `attributes` and reloads. — commonTest.
- [ ] T052 [P] [US3] `BE:` Audit test asserting no entity-specific hardcoded field lists remain outside `StandardFieldProvider` implementations.

**Checkpoint**: The split-brain model is fully gone; one field abstraction everywhere. Validates SC-004.

---

## Phase 6: User Story 4 — Configuration changes sync everywhere and work offline (Priority: P2)

**Goal**: Additions/edits/deletions of fields and sections propagate to all devices; deletions retain stored custom values; forms render offline; concurrent edits reconcile.

**Independent Test**: Delete a custom field on device A → gone on device B after a sync cycle (stored values retained). Edit config while B is offline → form still renders; change applies on reconnect.

- [ ] T053 [US4] `APP:` Wire editor delete → remove the field/section from the aggregate → push (replace-aggregate); confirm pull on another device drops the absent member (delete-by-absence). (Depends T024, T038.) _(Wired by architecture: editor deleteField → aggregate save → replace push; backend deletes absent members; pull replaces local. Cross-device runtime verification pending.)_
- [ ] T054 [P] [US4] `APP:` Section removal propagation + reassignment-on-delete consistency across sync (removed section absent from the aggregate; its fields already reassigned). (Depends T024, T042.)
- [ ] T055 [P] [US4] `APP:` Verify offline render path uses last-synced schema (no network in `DynamicFormRenderer`/`getConfigSchema` cache fallback); add offline render test.
- [ ] T056 [P] [US4] `APP:` Conflict test — aggregate-level last-write-wins + optimistic `version`: a stale push is rejected, the delegate re-pulls, re-applies local edits, and retries; the local unsynced aggregate wins until pushed. — commonTest.
- [ ] T057 [P] [US4] `BE:` Sync test — a field/section omitted from a pushed `FormSchema` is deleted server-side and is absent on the next pull (deletion reaches other devices via absence). (Closes the documented known gap.)
- [ ] T058 [P] [US4] `BE:` Confirm deleting a CUSTOM field definition does not purge values stored in owning entities' `attributes` (retention test).

**Checkpoint**: Deletions propagate (SC-005); offline render verified (SC-006).

---

## Phase 7: User Story 5 — Every domain uses the same form system (Priority: P3)

**Goal**: Product, order, invoice, business all render from and are configurable through the unified system; their standard fields come from registries; remaining bespoke forms retired.

**Independent Test**: For each domain, the entry screen renders via `DynamicFormRenderer` and its config screen lists exactly the registry-defined standard fields.

- [X] T059 [P] [US5] `BE:` `ProductStandardFieldProvider` (+ remove product seeding) in `product/...`.
- [X] T060 [P] [US5] `BE:` `OrderStandardFieldProvider` (+ remove seeding) in `order/...`.
- [X] T061 [P] [US5] `BE:` `InvoiceStandardFieldProvider` (+ remove seeding) in `invoice/...`.
- [X] T062 [P] [US5] `BE:` `BusinessStandardFieldProvider` for `EntityType.BUSINESS` (+ remove seeding) in the **`workspace`** module (no separate `business` backend module exists).
- [X] T063 [P] [US5] `APP:` Address, location/map, and business-hours `CustomFieldWidget`s (`@WidgetKey`) registered for the domains that need them.
- [X] T064 [US5] `APP:` Rewire Product entry screen + ViewModel to `DynamicFormRenderer` (+ product dynamic option providers). (Depends T027, T059.) _(Product vertical: attributes round-trip (domain/API/entity/mappers/migration 8→9), formSchema+registries in VM, ConfigAttributesSection in the form, 4 option providers. Standard columns stay on the existing bespoke fields — same hybrid as customer.)_
- [ ] T065 [US5] `APP:` Rewire Order entry screen + ViewModel; add `onFormConfig` nav for order. (Depends T027, T060.) _(DEFERRED: order has no attributes column on backend or app — custom values cannot persist; the entry screen is a transactional document builder. Backend provider (T060) makes the order form configurable; rewire needs an attributes column first.)_
- [ ] T066 [US5] `APP:` Rewire Invoice entry screen + ViewModel; add `onFormConfig` nav for invoice. (Depends T027, T061.) _(DEFERRED: same as T065 — invoice lacks an attributes column end-to-end.)_
- [X] T067 [US5] `APP:` Rewire Business overview/custom-attributes screens to the unified renderer. (Depends T027, T062, T063.) _(BusinessCustomAttributes screen+VM migrated off legacy attributeDefinitions onto unified FormSchema + ConfigAttributesSection.)_
- [ ] T068 [P] [US5] `APP:` Per-domain smoke test: each entry screen renders from config and lists exactly registry fields.

**Checkpoint**: All five domains config-driven (SC-001/SC-007 across the board).

---

## Phase 8: Polish & Cross-Cutting

- [X] T069 [P] `BE:` Update `form/CLAUDE.md` + `docs/modules/form.md` for the unified model, sections, registry SPI, unified `/sync`.
- [X] T070 [P] `BE:` Update `docs/guides/offline-sync-contract.md` — document `form` as an **aggregate-grained** `/sync` resource (one `FormSchema` per entityType; uid=entityType; delete-by-absence; optimistic `version`), a documented nuance alongside `tax`/`file`; **remove the "known gap"** note (deletions now round-trip via absence).
- [X] T071 [P] `APP:` Update `feature/form` docs + `.claude/skills/offline-sync` Form note (single aggregate feed under one `SyncEntity.FORM` checkpoint; delete-by-absence; aggregate-level last-write-wins + optimistic version).
- [X] T072 `APP:` Replace any remaining silent JSON-parse `catch → emptyMap()` with `FormLogger` warnings; audit no hardcoded UI strings remain. _(FormSyncDelegate push catch now logs via FormLogger; renderer fallback text resourced. Editor sheet strings still inlined — see T046.)_
- [ ] T073 [P] `BE:` Coverage pass — bring `form` module to constitution gates (service ≥80%, endpoints ≥90%); `./gradlew :form:test ciBuild`.
- [ ] T074 `APP:` Compile-gate all targets: `./gradlew :feature:form:check androidApp:compileDebugKotlinAndroid shared:compileKotlinIosSimulatorArm64 desktopApp:compileKotlin`.
- [ ] T075 `APP:`+`BE:` Run `quickstart.md` validation end-to-end on customer; confirm SC-001..SC-008 checkpoints.
- [ ] T076 [P] `BE:`+`APP:` Confirm no legacy form artifacts remain — grep both repos for `FieldConfig`/`AttributeDefinition`/`field-configs`/`attribute-definitions`/`DefaultFormConfigs`; assert removed (legacy cutover done in T010/T015/T019/T021, not deferred).
- [ ] T077 [P] [US3] `BE:` FR-022 seed-on-read merge test: a workspace customizes a field (hide + relabel), then a new registry standard field is introduced → next `getConfigSchema` adds ONLY the new field, leaving the customized field's visibility/label/order/section/validation untouched. `form/src/test/.../FormSeedMergePreservationTest.kt`.
- [ ] T078 `BE:`+`APP:` SC-007 verification: measure bespoke per-domain form code (hand-built form composables/field wiring) before vs after across the five domains; record the reduction (target ≥70%) in `specs/011-unified-schema-driven/quickstart.md` validation notes. If the metric proves impractical to quantify exactly, record the qualitative outcome (which bespoke forms were retired) instead.

---

## Dependencies & Execution Order

- **Setup (P1)** → **Foundational (P2, blocks everything)** → **US1 (P3)** → **US2 (P4)** → **US3 (P5)** → **US4 (P6)** → **US5 (P7)** → **Polish (P8)**.
- US1 and US2 are both P1 and both depend only on Foundational — they can run in parallel by two developers after Phase 2, integrating on the customer screen.
- US3 depends on the editor (US2) for the interleaved-list assertion; US4 depends on the editor delete flow (US2) and the sync delegate (Foundational); US5 depends on the renderer (Foundational) + per-domain registries.
- Within Foundational: backend (T005–T018) and app (T019–T027) are largely independent tracks and can proceed in parallel; T010 depends on T005–T007; T013 depends on T008/T009/T011/T012; app T021/T023/T024 depend on T020/T022.

## Parallel Opportunities

- **Setup**: T001–T004 all [P].
- **Foundational**: backend track {T005,T006,T007,T008,T011,T016,T017,T018} and app track {T019,T020,T025,T026} run in parallel; converge at T013/T027.
- **US1**: T028,T029,T030,T031,T035,T036,T037 are [P] before the screen rewire (T033/T034) integrates them.
- **US2**: T040–T047 are [P] feature areas of the editor once T038/T039 land.
- **US5**: per-domain providers/widgets T059–T063,T068 are [P]; screen rewires T064–T067 are sequential per domain but independent across domains.

## Implementation Strategy

- **MVP** = Phase 1 + Phase 2 + Phase 3 (US1): the customer form fully config-driven, dynamic dropdowns + image gallery via the renderer. Stop, validate SC-001 on customer, demo.
- **Usable product** = + Phase 4 (US2): admins self-serve with live preview. Demo the full loop.
- **Cleanup & reliability** = + Phases 5–6 (US3/US4): unification complete, deletions/offline solid.
- **Standardization** = + Phase 7 (US5): all domains. Then Phase 8 polish.
- Commit per task/logical group on `claude/clever-cori-71tjo5` in each repo. Fresh setup with a clean cutover — the old form module/tables/endpoints are removed (T010/T015/T019/T021); old installs simply re-provision from defaults on update.

## Notes

- Backend and app live in separate repos — each task's `BE:`/`APP:` prefix tells you which; commit/push to `claude/clever-cori-71tjo5` in the corresponding repo.
- Legacy form removal is part of THIS feature (fresh setup, clean cutover) — not deferred.
- Keep UID generation in ViewModels; repositories local-only; the API lives only in `FormSyncDelegate` (+ the allowed UI `getConfigSchema` read).

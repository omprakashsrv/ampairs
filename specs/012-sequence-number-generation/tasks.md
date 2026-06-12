# Tasks: Sequence Number Generation Module (012)

**Input**: plan.md, spec.md, data-model.md, contracts/sequence-api.md, research.md
**Repos**: backend `/home/user/ampairs`, mobile `/home/user/ampairs-app` — both on branch `claude/jolly-gates-2mu6f0`
**Tests**: backend service-layer Mockito tests (module convention); mobile gate = 3-target compilation

## Phase 1: Setup (backend module skeleton)

- [ ] T001 Create `sequence/build.gradle.kts` from the `unit` module template (namespace/group unchanged, same dependency set) in `/home/user/ampairs/sequence/`
- [ ] T002 Register module: add `include("sequence")` to `settings.gradle.kts`; add `implementation(project(mapOf("path" to ":sequence")))` and `"sequence"` in `migrationModules` in `ampairs_service/build.gradle.kts`

## Phase 2: Foundational (schema + domain — blocks all stories)

- [ ] T003 [P] Entities `SequenceDefinition` + `SequenceScope` enum in `sequence/src/main/kotlin/com/ampairs/sequence/domain/model/SequenceDefinition.kt`; `SequenceAllocation` + `AllocationStatus` in `…/SequenceAllocation.kt` (per data-model.md; extend `OwnableBaseDomain`; uid prefixes `SQD`/`SQA`)
- [ ] T004 [P] Flyway migration `V1.0.83__create_sequence_module_tables.sql` under BOTH `sequence/src/main/resources/db/migration/mysql/` and `…/postgresql/`
- [ ] T005 [P] DTOs + extension functions in `…/domain/dto/SequenceDto.kt` (per contracts/sequence-api.md)
- [ ] T006 [P] Typed exceptions + `SequenceExceptionHandler` (`@RestControllerAdvice`, `BaseExceptionHandler`) in `…/exception/`
- [ ] T007 `SequenceFormatter` (single formatting source: `[prefix-]padded[-suffix]`, next-value rule incl. `start_value` raise) in `…/service/SequenceFormatter.kt`
- [ ] T008 Repositories: `SequenceDefinitionRepository` (incl. `@Lock(PESSIMISTIC_WRITE) findByUidForUpdate`, sync feed query, active-key lookups) and `SequenceAllocationRepository` in `…/repository/`

## Phase 3: US1 — Automatic numbers on entity creation (P1) + US4 resolution rules (P3, same code path)

- [ ] T009 `SequenceDefinitionService` + impl: `resolve(entityType, userId)` (USER → WORKSPACE → auto-provision default w/ standard prefixes), `next(entityType, userId)` atomic generation under row lock in `…/service/`
- [ ] T010 `POST /sequence/v1/definitions/next` in `SequenceDefinitionController` (`/sequence/v1/definitions`), caller user id via `AuthenticationHelper`

## Phase 4: US2 — Configure numbering schemes (P1)

- [ ] T011 Service: CRUD + `bulkUpsert` (uid-keyed; server counter authoritative; active-key uniqueness; reject counter lowering) + `preview(uid)` + `getAfterSync(lastSync, pageable)` (includes inactive rows)
- [ ] T012 Controller endpoints: `GET/POST /definitions/sync` (canonical params), `GET /definitions`, `POST /definitions`, `PUT /definitions/{uid}`, `GET /definitions/{uid}/preview`

## Phase 5: US3 — Block allocation for offline devices (P2)

- [ ] T013 `SequenceAllocationService` + impl: `allocate(entityType, deviceId, userId, blockSize)` under definition row lock (range math per data-model; refuse inactive), `report(list)` (forward-only clamp, EXHAUSTED flip), `listForDevice(deviceId, status?)` in `…/service/`
- [ ] T014 `SequenceAllocationController`: `POST /allocations`, `POST /allocations/report`, `GET /allocations` in `…/controller/`

## Phase 6: Backend tests + gate

- [ ] T015 [P] Mockito tests: formatter (padding/prefix/suffix/start-raise), resolution order, duplicate-active rejection, counter-lowering rejection, allocation range math + report clamping in `sequence/src/test/kotlin/com/ampairs/sequence/`
- [ ] T016 Gate: `./gradlew :sequence:build` green; commit + push backend

## Phase 7: Mobile foundation (`feature/sequence`)

- [ ] T017 Module skeleton: `feature/sequence/build.gradle.kts` (from `feature/unit` template, namespace `com.ampairs.sequence`), add `:feature:sequence` to `settings.gradle.kts`, `api(projects.feature.sequence)` in `shared/build.gradle.kts`
- [ ] T018 [P] Add `SEQUENCE("sequence")` to `data/sync/…/SyncEntity.kt`; add `sequenceUrl()` to `data/common/…/ApiUrlBuilder.kt`
- [ ] T019 Room: `SequenceDatabase` v1 + `SequenceDefinitionEntity`/`SequenceAllocationEntity` (format snapshot fields) + DAOs in `feature/sequence/src/commonMain/…/data/db/`
- [ ] T020 [P] Platform DI modules (Android `createAndroidDatabase<SequenceDatabase>`, iOS/Desktop `createDatabase<…>`), all `@SingleIn(WorkspaceScope::class)` + closable registry, + common DAO `@Provides` module

## Phase 8: Mobile US3 — offline-first client

- [ ] T021 `SequenceApi` + impl (`@SingleIn(AppScope)` Ktor): definitions sync GET/POST, `requestAllocation`, `reportAllocations`, `getAllocations`
- [ ] T022 Repositories: `SequenceRepository` (definitions, local-only + markPendingPush) and `SequenceAllocationRepository` (transactional local consume; on-demand grant via API — documented allowed exception R6)
- [ ] T023 `SequenceNumberProvider` (`@Inject`, cross-feature entry point) + `SequenceFormatter` + `SequenceNumberResult` (provisional flag per FR-011)
- [ ] T024 `SequenceSyncDelegate` `@ContributesIntoMap(WorkspaceScope)` + `@SyncEntityKey(SyncEntity.SEQUENCE)`: pull definitions (batched, local-unsynced wins, hard-delete server-inactive? — definitions keep inactive rows locally for history: upsert as-is), push unsynced definitions + consumption reports
- [ ] T025 Gate: `./gradlew androidApp:compileDebugKotlinAndroid shared:compileKotlinIosSimulatorArm64 desktopApp:compileKotlin`; commit + push mobile

## Phase 9: Polish

- [ ] T026 Update backend docs if needed (`docs/modules/` optional) and verify spec checklist; final commit/push both repos

## Dependencies

```
T001–T002 → T003–T008 → {US1: T009–T010} → {US2: T011–T012} → {US3: T013–T014} → T015–T016
T017–T020 → T021–T024 → T025 → T026
Backend contract (Phases 3–5) precedes mobile client (Phase 8).
```

## MVP

Backend Phases 1–4 (definitions + direct generation) constitute the MVP; Phase 5 + mobile add the offline/multi-device guarantee.

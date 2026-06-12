# Implementation Plan: Sequence Number Generation Module

**Branch**: `claude/jolly-gates-2mu6f0` (feature id `012-sequence-number-generation`) | **Date**: 2026-06-12 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/012-sequence-number-generation/spec.md`

## Summary

Centralized, workspace-tenant sequence numbering: a new backend `sequence` module owns `SequenceDefinition` (configurable scheme + atomic counter) and `SequenceAllocation` (exclusive device blocks), exposing the canonical `/sync` contract for definitions plus an off-contract allocation RPC. A new mobile `feature/sequence` module stores definitions + allocations in a workspace-scoped Room DB, exposes `SequenceNumberProvider` for ViewModels (offline-first local block consumption), and a `SequenceSyncDelegate` reports consumption / syncs definitions. Atomicity via pessimistic row lock on the definition counter (research R1); duplicates impossible across devices because granted ranges are disjoint.

## Technical Context

**Language/Version**: Backend Kotlin 2.3 / Java 21 / Spring Boot 4.0; Mobile Kotlin 2.4 KMP (Android/iOS/Desktop/Wasm)
**Primary Dependencies**: Backend Spring Data JPA, Flyway, Jackson (global snake_case); Mobile Room KMP 2.8.4, Ktor 3.5, Metro DI 1.1.1
**Storage**: MySQL + PostgreSQL (both migration vendors) on backend; Room (`workspace_{slug}_sequence.db`) on mobile
**Testing**: Backend JUnit5 + Mockito (`unit` module convention); Mobile compilation gates for all 3 targets
**Target Platform**: Spring Boot service + Android/iOS/Desktop apps
**Project Type**: Web service + mobile (two repos: `ampairs`, `ampairs-app`)
**Performance Goals**: <50ms next-number/grant at service layer; on-device generation with zero network calls
**Constraints**: offline-capable, zero duplicate numbers, counter monotonicity, multi-tenant isolation via `@TenantId`
**Scale/Scope**: 100k+ generations/day, thousands of concurrent users

## Constitution Check

| Gate | Status |
|---|---|
| I. `Instant` timestamps only, TIMESTAMPTZ on Postgres | PASS — entities inherit `BaseDomain` Instants; migrations use TIMESTAMP/TIMESTAMPTZ |
| II. DTO isolation, validation annotations | PASS — `domain/dto/` Request/Response + `asResponse()` extensions |
| III. Global snake_case, no `@JsonProperty` | PASS |
| IV. Multi-tenancy via `OwnableBaseDomain` + filter-set context | PASS — both entities extend `OwnableBaseDomain`; no service-level tenant mutation |
| V. `ApiResponse<T>` everywhere, `PageResponse` for pages | PASS |
| VI. Exceptions bubble to module `@RestControllerAdvice` | PASS — typed exceptions + `SequenceExceptionHandler` |
| VII. `@EntityGraph`/derived queries | PASS — flat entities, derived queries only; one `@Lock` query justified (R1) |
| Offline-sync canonical contract | PASS for definitions; allocations off-contract with documented justification (R3) |
| Mobile: Metro WorkspaceScope DB + closable registry + local-only repository | PASS — single allowed API exception documented (R6) |

No violations → Complexity Tracking not required.

## Project Structure

### Documentation (this feature)

```
specs/012-sequence-number-generation/
├── spec.md
├── plan.md              # this file
├── research.md          # decisions R1–R8
├── data-model.md
├── quickstart.md
├── contracts/sequence-api.md
└── tasks.md             # /speckit.tasks output
```

### Source Code

```
# Backend repo (ampairs)
sequence/
├── build.gradle.kts                          # copy of unit module template
└── src/main/kotlin/com/ampairs/sequence/
    ├── domain/model/SequenceDefinition.kt    # + SequenceScope, AllocationStatus enums
    ├── domain/model/SequenceAllocation.kt
    ├── domain/dto/SequenceDto.kt             # requests/responses + extensions
    ├── repository/SequenceDefinitionRepository.kt
    ├── repository/SequenceAllocationRepository.kt
    ├── service/SequenceFormatter.kt          # single formatting source of truth
    ├── service/SequenceDefinitionService(.Impl).kt
    ├── service/SequenceAllocationService(.Impl).kt
    ├── controller/SequenceDefinitionController.kt   # /sequence/v1/definitions(+/sync,/next,/preview)
    ├── controller/SequenceAllocationController.kt   # /sequence/v1/allocations(+/report)
    └── exception/…                            # typed exceptions + SequenceExceptionHandler
sequence/src/main/resources/db/migration/{mysql,postgresql}/V1.0.83__create_sequence_module_tables.sql
sequence/src/test/kotlin/com/ampairs/sequence/…   # Mockito service tests
settings.gradle.kts                            # + include("sequence")
ampairs_service/build.gradle.kts               # + project dep + migrationModules entry

# Mobile repo (ampairs-app)
feature/sequence/
├── build.gradle.kts                           # copy of feature/unit template
├── schemas/                                   # Room schema v1
└── src/
    ├── commonMain/kotlin/com/ampairs/sequence/
    │   ├── data/api/SequenceApi(.Impl).kt
    │   ├── data/db/SequenceDatabase.kt + entities + DAOs
    │   ├── data/repository/SequenceRepository.kt          # definitions, local-only
    │   ├── data/repository/SequenceAllocationRepository.kt # local consumption + on-demand grant (allowed exception)
    │   ├── domain/SequenceNumberProvider.kt   # cross-feature entry point
    │   ├── domain/SequenceFormatter.kt
    │   ├── di/SequenceModule.kt               # common DAO providers
    │   └── sync/SequenceSyncDelegate.kt       # @SyncEntityKey(SyncEntity.SEQUENCE)
    ├── androidMain/…/SequenceModule.android.kt
    ├── iosMain/…/SequenceModule.ios.kt
    └── desktopMain/…/SequenceModule.desktop.kt
data/sync/…/SyncEntity.kt                      # + SEQUENCE("sequence")
data/common/…/ApiUrlBuilder.kt                 # + sequenceUrl()
settings.gradle.kts                            # + :feature:sequence
shared/build.gradle.kts                        # + api(projects.feature.sequence)
```

**Structure Decision**: Two-repo delivery following each repo's module conventions; no UI screens on mobile in v1 (configuration is admin/web-side; mobile consumes). Wiring individual entity creation flows to the provider is deferred per spec assumption.

## Phase summaries

- **Phase 0** (research.md): all unknowns resolved — locking strategy, contract placement, device identity, provisional-number behavior, Flyway `V1.0.83`.
- **Phase 1** (data-model.md, contracts/): two backend tables + two Room tables with format snapshot on allocations; full endpoint + DTO contract.
- **Phase 2**: tasks via `/speckit.tasks`.

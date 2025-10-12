# Implementation Plan: Business Module

**Branch**: `003-business-module` | **Date**: 2025-10-10 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/003-business-module/spec.md`

## Execution Flow (/plan command scope)
```
1. Load feature spec from Input path
   → If not found: ERROR "No feature spec at {path}"
2. Fill Technical Context (scan for NEEDS CLARIFICATION)
   → Detect Project Type from context (web=frontend+backend, mobile=app+api)
   → Set Structure Decision based on project type
3. Evaluate Constitution Check section below
   → If violations exist: Document in Complexity Tracking
   → If no justification possible: ERROR "Simplify approach first"
   → Update Progress Tracking: Initial Constitution Check
4. Execute Phase 0 → research.md
   → If NEEDS CLARIFICATION remain: ERROR "Resolve unknowns"
5. Execute Phase 1 → contracts, data-model.md, quickstart.md, agent-specific template file (e.g., `CLAUDE.md` for Claude Code, `.github/copilot-instructions.md` for GitHub Copilot, or `GEMINI.md` for Gemini CLI).
6. Re-evaluate Constitution Check section
   → If new violations: Refactor design, return to Phase 1
   → Update Progress Tracking: Post-Design Constitution Check
7. Plan Phase 2 → Describe task generation approach (DO NOT create tasks.md)
8. STOP - Ready for /tasks command
```

**IMPORTANT**: The /plan command STOPS at step 7. Phases 2-4 are executed by other commands:
- Phase 2: /tasks command creates tasks.md
- Phase 3-4: Implementation execution (manual or via tools)

## Summary
Extract business configuration and profile data from the bloated Workspace entity into a dedicated Business module. This refactoring separates tenant management (Workspace) from business profile concerns (Business), improving maintainability and enabling future extensibility like multi-location support. The implementation creates a new business module following existing patterns (Customer, Product, Order, Invoice) with full API support and data migration.

**User-Provided Context**: Create business module which will store the business details inside the workspace. Currently it is part of workspace entity. Since there can be so many details of configurations of business, it is going to be difficult to manage with workspace entity. In business details we can have name, type, address details, location, opening hours, gst or tax details, contact details, owner name etc.

## Technical Context
**Language/Version**: Kotlin 1.9 + JVM 17
**Primary Dependencies**: Spring Boot 3.x, Spring Data JPA, Hibernate, Jakarta Validation, PostgreSQL driver
**Storage**: PostgreSQL 15+ with JSON support (for tax settings, operating days)
**Testing**: JUnit 5, Spring Boot Test, MockK, Testcontainers for integration tests
**Target Platform**: Linux server (Spring Boot application)
**Project Type**: web (backend API + future frontend/mobile integration)
**Performance Goals**: <50ms p95 for business profile queries, support 10,000+ businesses per database
**Constraints**: Multi-tenant architecture with @TenantId filtering, backward compatibility during migration, no cross-tenant data leakage
**Scale/Scope**: New backend module (~15-20 source files), 1 database table, 3 REST endpoints, data migration for existing workspaces

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Simplicity**:
- Projects: 1 (business module only - backend API)
- Using framework directly? YES (Spring Boot, Spring Data JPA - no wrappers)
- Single data model? NO - DTOs required per project guidelines (never expose entities)
- Avoiding patterns? NO - Repository pattern required by Spring Data JPA architecture

**Architecture**:
- EVERY feature as library? NO - Following existing modular monolith pattern (business module)
- Libraries listed: N/A - Using Spring Boot module architecture
- CLI per library: N/A - REST API only, no CLI
- Library docs: N/A - OpenAPI/Swagger for API documentation

**Testing (NON-NEGOTIABLE)**:
- RED-GREEN-Refactor cycle enforced? YES - contract tests first, then implementation
- Git commits show tests before implementation? YES
- Order: Contract→Integration→Unit strictly followed? YES
- Real dependencies used? YES (Testcontainers for PostgreSQL)
- Integration tests for: new module, contract changes, data migration? YES
- FORBIDDEN: Implementation before test, skipping RED phase - COMPLIANT

**Observability**:
- Structured logging included? YES (Spring Boot logging)
- Frontend logs → backend? N/A (backend-only feature)
- Error context sufficient? YES (ApiResponse with error details, trace IDs)

**Versioning**:
- Version number assigned? N/A (follows main application versioning)
- BUILD increments on every change? YES (CI/CD handles)
- Breaking changes handled? YES (migration plan, backward compatibility phase)

## Project Structure

### Documentation (this feature)
```
specs/[###-feature]/
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (/plan command)
├── data-model.md        # Phase 1 output (/plan command)
├── quickstart.md        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (repository root)
```
ampairs-backend/
├── business/                          # NEW MODULE
│   ├── build.gradle.kts
│   └── src/
│       ├── main/kotlin/com/ampairs/business/
│       │   ├── model/
│       │   │   ├── Business.kt        # Entity
│       │   │   └── enums/
│       │   │       └── BusinessType.kt
│       │   ├── model/dto/
│       │   │   ├── BusinessRequest.kt
│       │   │   ├── BusinessResponse.kt
│       │   │   └── BusinessExtensions.kt
│       │   ├── repository/
│       │   │   └── BusinessRepository.kt
│       │   ├── service/
│       │   │   └── BusinessService.kt
│       │   ├── controller/
│       │   │   └── BusinessController.kt
│       │   └── exception/
│       │       └── BusinessExceptionHandler.kt
│       ├── test/kotlin/com/ampairs/business/
│       │   ├── BusinessIntegrationTest.kt
│       │   └── BusinessApiTest.kt
│       └── resources/
│           └── db/migration/
│               ├── V1.0.0__create_businesses_table.sql
│               └── V1.0.1__migrate_workspace_business_data.sql
├── workspace/
│   └── ... (existing)
└── ...

ampairs-web/                           # FUTURE
└── src/app/business/                  # Future frontend implementation

ampairs-mp-app/                        # FUTURE
└── composeApp/src/commonMain/kotlin/com/ampairs/business/  # Future mobile
```

**Structure Decision**: Backend module only (Option 2 - Web backend). Frontend/mobile integration is future work.

## Phase 0: Outline & Research
1. **Extract unknowns from Technical Context** above:
   - For each NEEDS CLARIFICATION → research task
   - For each dependency → best practices task
   - For each integration → patterns task

2. **Generate and dispatch research agents**:
   ```
   For each unknown in Technical Context:
     Task: "Research {unknown} for {feature context}"
   For each technology choice:
     Task: "Find best practices for {tech} in {domain}"
   ```

3. **Consolidate findings** in `research.md` using format:
   - Decision: [what was chosen]
   - Rationale: [why chosen]
   - Alternatives considered: [what else evaluated]

**Output**: research.md with all NEEDS CLARIFICATION resolved

## Phase 1: Design & Contracts
*Prerequisites: research.md complete*

1. **Extract entities from feature spec** → `data-model.md`:
   - Entity name, fields, relationships
   - Validation rules from requirements
   - State transitions if applicable

2. **Generate API contracts** from functional requirements:
   - For each user action → endpoint
   - Use standard REST/GraphQL patterns
   - Output OpenAPI/GraphQL schema to `/contracts/`

3. **Generate contract tests** from contracts:
   - One test file per endpoint
   - Assert request/response schemas
   - Tests must fail (no implementation yet)

4. **Extract test scenarios** from user stories:
   - Each story → integration test scenario
   - Quickstart test = story validation steps

5. **Update agent file incrementally** (O(1) operation):
   - Run `/scripts/update-agent-context.sh [claude|gemini|copilot]` for your AI assistant
   - If exists: Add only NEW tech from current plan
   - Preserve manual additions between markers
   - Update recent changes (keep last 3)
   - Keep under 150 lines for token efficiency
   - Output to repository root

**Output**: data-model.md, /contracts/*, failing tests, quickstart.md, agent-specific file

## Phase 2: Task Planning Approach
*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:

1. **Module Setup Tasks** (Infrastructure)
   - Create Gradle module structure (build.gradle.kts)
   - Add dependencies (core, workspace, spring-boot-starter-web, etc.)
   - Configure module in settings.gradle.kts

2. **Database Tasks** (Schema & Migration)
   - Create V1.0.0__create_businesses_table.sql migration
   - Create V1.0.1__migrate_workspace_business_data.sql migration
   - Test migrations with sample data

3. **Entity & Enum Tasks** [P]
   - Create BusinessType enum
   - Create Business entity with @TenantId
   - Add BaseDomain extension

4. **DTO Tasks** [P]
   - Create BusinessResponse DTO
   - Create BusinessCreateRequest DTO with validation
   - Create BusinessUpdateRequest DTO
   - Create extension functions (asBusinessResponse, toBusiness)

5. **Repository Tasks**
   - Create BusinessRepository interface
   - Add custom queries (findByWorkspaceId)
   - Configure @EntityGraph if needed

6. **Service Tasks**
   - Create BusinessService with validation logic
   - Implement create, findByWorkspaceId, update methods
   - Add business rule enforcement

7. **Controller Tasks**
   - Create BusinessController with REST endpoints
   - Implement GET /api/v1/business
   - Implement POST /api/v1/business
   - Implement PUT /api/v1/business
   - Add @Valid annotations for DTO validation

8. **Exception Handler Tasks** [P]
   - Create BusinessNotFoundException
   - Create BusinessAlreadyExistsException
   - Create BusinessExceptionHandler

9. **Integration Test Tasks** (TDD - Must fail first)
   - Create BusinessIntegrationTest with Testcontainers
   - Test: Create business profile (POST)
   - Test: Retrieve business profile (GET)
   - Test: Update business profile (PUT)
   - Test: Duplicate creation prevention (409)
   - Test: Tenant isolation (no cross-workspace access)
   - Test: Validation scenarios

10. **Contract Test Tasks** (Verify API schema)
    - Test: API response matches OpenAPI schema
    - Test: Request validation matches spec
    - Test: Error responses match spec

11. **Migration Test Tasks**
    - Test: Data migrated from workspace table
    - Test: Foreign key constraints work
    - Test: Indexes created correctly

12. **Documentation Tasks**
    - Update CLAUDE.md with business module info
    - Add API examples to documentation
    - Update architecture diagrams

**Ordering Strategy**:
1. **Setup** (Tasks 1-2): Module & database structure first
2. **Models** (Tasks 3-4): Entities and DTOs (can be parallel)
3. **Integration Tests** (Task 9): TDD - Write failing tests BEFORE implementation
4. **Repository** (Task 5): Make repository tests pass
5. **Service** (Task 6): Make service tests pass
6. **Controller** (Task 7): Make API tests pass
7. **Exception Handling** (Task 8): Can be parallel with controller
8. **Contract Tests** (Task 10): Verify full API compliance
9. **Migration Tests** (Task 11): Verify data migration
10. **Documentation** (Task 12): Final step

**Estimated Output**: ~30-35 numbered, ordered tasks in tasks.md

**Parallel Execution Markers**:
- [P] indicates tasks that can run in parallel (independent files)
- Most entity/DTO creation can be parallel
- Tests must run after implementation

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Phase 3+: Future Implementation
*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)  
**Phase 4**: Implementation (execute tasks.md following constitutional principles)  
**Phase 5**: Validation (run tests, execute quickstart.md, performance validation)

## Complexity Tracking
*Fill ONLY if Constitution Check has violations that must be justified*

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| DTOs instead of single model | Project guideline: Never expose JPA entities directly | Security risk, tight coupling, harder to evolve API independently |
| Repository pattern | Spring Data JPA architecture requirement | Direct DB access violates framework patterns, loses transaction management |

**Justification**: Both "violations" are actually project requirements, not true complexity additions. The codebase mandates DTOs for API safety and uses Spring Data JPA which inherently includes the Repository pattern.


## Progress Tracking
*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command) ✅
- [x] Phase 1: Design complete (/plan command) ✅
- [x] Phase 2: Task planning complete (/plan command - describe approach only) ✅
- [x] Phase 3: Tasks generated (/tasks command) ✅
- [ ] Phase 4: Implementation complete - READY
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS ✅
- [x] Post-Design Constitution Check: PASS ✅
- [x] All NEEDS CLARIFICATION resolved ✅
- [x] Complexity deviations documented ✅

**Artifacts Generated**:
- [x] spec.md - Feature specification
- [x] plan.md - This implementation plan
- [x] research.md - Technical research and decisions
- [x] data-model.md - Entity and database design
- [x] contracts/business-api.yaml - OpenAPI specification
- [x] quickstart.md - Validation test scenarios
- [x] tasks.md - 35 executable tasks with dependencies

**Next Steps**:
1. ✅ ~~Run `/tasks` command to generate tasks.md~~ **COMPLETE**
2. Execute tasks T001-T035 in order (TDD approach)
3. Run quickstart.md validation scenarios
4. Performance testing (<50ms p95)

---
*Task breakdown complete. Ready for implementation (Phase 4).*
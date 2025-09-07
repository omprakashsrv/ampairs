# Implementation Plan: Unified Retail Management Platform

**Branch**: `001-you-are-retail` | **Date**: 2025-01-06 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-you-are-retail/spec.md`

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
Unified retail management platform for various store types (hardware, kirana, wholesale, jewelry) with multi-platform support (Android, iOS, web, desktop) and Spring Boot backend. Core requirements include multi-tenant workspaces, product management, inventory tracking, order processing, invoice generation, and customer management with real-time synchronization across platforms.

## Technical Context
**Language/Version**: Spring Boot 3.5.3 + Kotlin 2.2.0, Angular 20.1.0, Kotlin Multiplatform 2.2.10  
**Primary Dependencies**: Spring Security + OAuth2, Angular Material 3, Compose Multiplatform, Room 2.7.2  
**Storage**: JPA/Hibernate with multi-tenancy support, PostgreSQL (production), Room (mobile)  
**Testing**: JUnit 5 + Spring Test, Jasmine + Karma, KMP Test Framework  
**Target Platform**: Multi-platform (Web browsers, Android 24+, iOS 15+, Desktop JVM)
**Project Type**: web + mobile (existing codebase with backend, frontend, mobile apps)  
**Performance Goals**: <200ms API response time, <2s page load, <3s mobile app launch, 1000+ concurrent users  
**Constraints**: Multi-tenant data isolation, offline-capable mobile, real-time sync, GST compliance  
**Scale/Scope**: Enterprise retail platform, 8 backend modules, 20+ UI components, cross-platform synchronization

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Simplicity**:
- Projects: 3 (backend Spring Boot, frontend Angular, mobile KMP) ✅
- Using framework directly? YES (Spring Boot, Angular Material, Compose) ✅
- Single data model? NO - DTOs needed for API serialization ❌ JUSTIFIED
- Avoiding patterns? NO - Repository pattern exists, proven need for multi-tenancy ❌ JUSTIFIED

**Architecture**:
- EVERY feature as library? NO - Existing modular Spring Boot architecture ❌ JUSTIFIED
- Libraries listed: [core, auth, workspace, product, customer, order, invoice, notification]
- CLI per library: NO - REST API + UI architecture ❌ JUSTIFIED
- Library docs: NO - OpenAPI + component documentation ❌ JUSTIFIED

**Testing (NON-NEGOTIABLE)**:
- RED-GREEN-Refactor cycle enforced? MUST IMPLEMENT ❌ TODO
- Git commits show tests before implementation? MUST IMPLEMENT ❌ TODO
- Order: Contract→Integration→E2E→Unit strictly followed? MUST IMPLEMENT ❌ TODO
- Real dependencies used? YES (actual PostgreSQL, Room) ✅
- Integration tests for: new libraries, contract changes, shared schemas? MUST IMPLEMENT ❌ TODO
- FORBIDDEN: Implementation before test, skipping RED phase ❌ TODO

**Observability**:
- Structured logging included? YES (Logback with Spring Boot) ✅
- Frontend logs → backend? NEEDS IMPLEMENTATION ❌ TODO
- Error context sufficient? YES (comprehensive error codes) ✅

**Versioning**:
- Version number assigned? NEEDS IMPLEMENTATION ❌ TODO
- BUILD increments on every change? NEEDS IMPLEMENTATION ❌ TODO
- Breaking changes handled? NEEDS IMPLEMENTATION ❌ TODO

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
# Option 1: Single project (DEFAULT)
src/
├── models/
├── services/
├── cli/
└── lib/

tests/
├── contract/
├── integration/
└── unit/

# Option 2: Web application (when "frontend" + "backend" detected)
backend/
├── src/
│   ├── models/
│   ├── services/
│   └── api/
└── tests/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   └── services/
└── tests/

# Option 3: Mobile + API (when "iOS/Android" detected)
api/
└── [same as backend above]

ios/ or android/
└── [platform-specific structure]
```

**Structure Decision**: Option 2 + Mobile (existing multi-platform architecture with backend, frontend, mobile)

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
- Load `/templates/tasks-template.md` as base
- Generate tasks from Phase 1 design docs (contracts, data model, quickstart)
- Each contract → contract test task [P]
- Each entity → model creation task [P] 
- Each user story → integration test task
- Implementation tasks to make tests pass

**Ordering Strategy**:
- TDD order: Tests before implementation 
- Dependency order: Models before services before UI
- Mark [P] for parallel execution (independent files)

**Estimated Output**: 25-30 numbered, ordered tasks in tasks.md

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
| DTOs for serialization | API requires snake_case JSON while domain uses camelCase | Single model would require @JsonProperty on every field, polluting domain |
| Repository pattern | Multi-tenant data isolation, complex queries, audit trails | Direct JPA would duplicate tenant filtering, violate DRY principle |
| Modular architecture (not libraries) | Existing mature Spring Boot modules with domain boundaries | Library approach would require major refactoring, risk breaking existing functionality |
| REST API (not CLI per module) | Multi-platform clients (web, mobile) need HTTP API | CLI would not support web/mobile clients effectively |


## Progress Tracking
*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command)
- [x] Phase 1: Design complete (/plan command)
- [x] Phase 2: Task planning complete (/plan command - describe approach only)
- [ ] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS (with justified deviations documented)
- [x] Post-Design Constitution Check: PASS (no new violations introduced)
- [x] All NEEDS CLARIFICATION resolved
- [x] Complexity deviations documented

---
*Based on Constitution v2.1.1 - See `/memory/constitution.md`*
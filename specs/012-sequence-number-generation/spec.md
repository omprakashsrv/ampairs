# Feature Specification: Sequence Number Generation Module

**Feature Branch**: `012-sequence-number-generation` (developed on `claude/jolly-gates-2mu6f0`)
**Created**: 2026-06-12
**Status**: Draft
**Input**: User description: "Sequence Number Generation Module — centralized, configurable generation of unique sequential identifiers (e.g. INV-1001, PRD-501, CUS-2001, ORD-5401) for business entities (product, customer, order, invoice, future entities) in the Ampairs retail platform. Supports workspace-wide and user-specific sequence scopes; custom prefix/suffix, padding, increment step, start value, active flag; preview next number without consuming; reservation and consumption of numbers; consumed/voided numbers never reused; multi-device conflict-free allocation via server-assigned block/range allocation enabling offline generation without duplicates."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Automatic document numbers on entity creation (Priority: P1)

A workspace user creates an invoice (or order, customer, product). The system automatically assigns the next sequential, human-readable document number (e.g. `INV-1001`) according to the workspace's configured numbering scheme — without the user having to think about numbering at all, and without ever producing a duplicate, even when several users on several devices create records at the same time.

**Why this priority**: This is the core value of the module — consistent, unique, business-friendly numbering for every transactional document. Everything else (configuration, offline blocks, user scopes) exists to serve this.

**Independent Test**: With a default sequence configured for invoices, create three invoices from two different devices concurrently and verify the assigned numbers are unique, sequential within the workspace scope, and correctly formatted.

**Acceptance Scenarios**:

1. **Given** an active workspace sequence for invoices with prefix `INV`, current value 1250, increment 1, **When** a user creates an invoice, **Then** the invoice receives number `INV-1251` and the sequence advances to 1251.
2. **Given** two users creating invoices simultaneously, **When** both requests are processed, **Then** each receives a distinct number and no value is duplicated.
3. **Given** an entity type with no sequence configured, **When** a record is created, **Then** the system falls back to a sensible default sequence for that entity type (auto-provisioned with the entity's standard prefix, starting at 1).

---

### User Story 2 - Configure numbering schemes (Priority: P1)

A workspace administrator defines how each entity type is numbered: prefix and/or suffix, zero-padding length, starting number, increment step, and whether the sequence is shared by the whole workspace or maintained per user. They can deactivate a scheme or preview the next number that would be generated without consuming it.

**Why this priority**: Without configuration the module cannot reflect business numbering policies (GST/audit numbering, sales-rep-specific books). It is a prerequisite for predictable Story 1 behavior beyond defaults.

**Independent Test**: Create a sequence definition `INV`, padding 6, start 1; preview shows `INV-000001`; generate one number; preview now shows `INV-000002`.

**Acceptance Scenarios**:

1. **Given** an administrator, **When** they create a sequence definition (entity type, scope, prefix, suffix, padding, start value, increment step), **Then** the definition is persisted and used for all subsequent generations for that entity type and scope.
2. **Given** a definition with padding length 6, **When** a number is generated at value 7, **Then** the formatted result is `INV-000007`.
3. **Given** a definition, **When** the administrator previews the next number, **Then** the displayed value is the next value that would be generated and the sequence does NOT advance.
4. **Given** an inactive definition, **When** generation is attempted against it, **Then** generation falls back to default numbering behavior for that entity type (the inactive scheme is not used).
5. **Given** an existing definition, **When** the administrator updates formatting fields (prefix/suffix/padding), **Then** new numbers use the new format while already-issued numbers are unaffected.

---

### User Story 3 - Offline, multi-device generation without conflicts (Priority: P2)

A user works on a mobile device with intermittent connectivity. Their device holds a server-assigned block of numbers (e.g. 1001–1050) for each sequence it uses. While offline, the device issues numbers locally from its block. A second device holds a disjoint block (1051–1100), so the two can never collide. When a device runs low it requests a new block; on reconnect, consumption is reported to the server.

**Why this priority**: Required for the offline-first mobile app, but only meaningful once Stories 1–2 (server-side definitions and generation) exist.

**Independent Test**: Allocate blocks to two simulated devices, generate numbers on both while "offline", reconnect, and verify zero duplicates and correct server-side accounting of consumed ranges.

**Acceptance Scenarios**:

1. **Given** a device requesting an allocation for the invoice sequence, **When** the server grants it, **Then** the device receives an exclusive contiguous range and the server advances the sequence's high-water mark past that range.
2. **Given** two devices with allocations for the same sequence, **When** both generate numbers offline, **Then** all generated numbers are unique because the ranges are disjoint.
3. **Given** a device that has consumed its entire block, **When** it is online, **Then** it transparently obtains the next block and continues numbering without user intervention.
4. **Given** a device that has consumed its entire block while offline, **When** the user creates another record, **Then** the record is created with a clearly-marked provisional number and receives its final sequence number on reconnect.
5. **Given** a device reporting its consumption on reconnect, **Then** the server records the device's progress within its block; unconsumed remainder of an expired/abandoned block is never reissued (gaps are acceptable, duplicates are not).

---

### User Story 4 - User-scoped sequences (Priority: P3)

A business gives each sales representative an independent numbering book: User A issues `AINV-1001, AINV-1002, …` while User B issues `BINV-1001, BINV-1002, …`. Each user's counter advances independently of the others.

**Why this priority**: Valuable for field-sales businesses, but a refinement of the scoping rules established in Stories 1–2.

**Independent Test**: Configure user-scoped invoice sequences with different prefixes for two users; verify each user's generations advance only their own counter.

**Acceptance Scenarios**:

1. **Given** user-scoped definitions for two users, **When** each generates three numbers, **Then** each user's numbers are sequential within their own scope and unaffected by the other.
2. **Given** an entity type with a user-scoped definition for User A only, **When** User B generates a number for that entity type, **Then** User B's generation resolves to the workspace-scoped definition (or default) — never to User A's sequence.

---

### Edge Cases

- **Cancellation/void**: A record whose number was issued is later cancelled — the number is permanently consumed; the next generation continues from the current counter (no reuse, gaps allowed).
- **Concurrent generation bursts**: Many simultaneous generation requests for the same sequence must serialize on the counter without duplicating values.
- **Definition deactivated mid-flight**: Devices holding blocks of a now-inactive sequence may exhaust their blocks; new allocations against an inactive definition are refused.
- **Duplicate definition attempts**: Only one active definition may exist per (workspace, entity type, scope, user) key; a second create attempt is rejected with a clear error.
- **Format change while blocks are outstanding**: Formatting (prefix/padding) is applied at generation time on the device from the definition snapshot it holds; numbers already issued never change.
- **Counter lowered below current**: Updating a start value lower than the current value must not cause re-issuance of already-used numbers; the system rejects lowering the counter.
- **Clock-independent ordering**: Sequence ordering is determined solely by counter values, never device clocks.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow workspace administrators to create, update, deactivate, and list sequence definitions with: entity type, scope (workspace or user), optional owning user (user scope only), optional prefix, optional suffix, padding length, start value, increment step (default 1), and active flag.
- **FR-002**: System MUST enforce at most one active sequence definition per lookup key — (workspace, entity type) for workspace scope; (workspace, entity type, user) for user scope.
- **FR-003**: System MUST resolve a generation request for (entity type, user) by preferring an active user-scoped definition for that user, then the active workspace-scoped definition, then an auto-provisioned default (entity-standard prefix, start 1, increment 1, no padding).
- **FR-004**: System MUST generate formatted numbers as `{prefix}-{padded counter}-{suffix}` (separator omitted when prefix/suffix absent), where the counter is the previous value plus the increment step, padded to the configured length.
- **FR-005**: Counter advancement MUST be atomic — concurrent generation requests against the same sequence MUST each receive a distinct value with no duplicates.
- **FR-006**: System MUST provide a preview of the next number for a definition without advancing the counter or reserving the value.
- **FR-007**: Consumed numbers MUST never be reused, including numbers belonging to records that are later cancelled, deleted, or voided, and unreturned remainders of device blocks. Gaps in issued numbers are acceptable.
- **FR-008**: System MUST allocate exclusive, contiguous number blocks to requesting devices (identified by device ID) for a given sequence definition, advancing the sequence high-water mark past the granted range; default block size 50, configurable per request within server-set bounds (min 1, max 1000).
- **FR-009**: Devices MUST be able to generate numbers locally from their allocated block while offline, applying the definition's formatting locally, with zero possibility of collision with other devices.
- **FR-010**: Devices MUST report block consumption progress to the server (allocation reference + next available value) when connectivity resumes; the server records progress for observability and support.
- **FR-011**: When a device exhausts its block and cannot reach the server, the system MUST keep the user's workflow unblocked (provisional local numbering clearly distinguishable from final numbers, finalized on reconnect).
- **FR-012**: System MUST refuse new allocations and direct generations against inactive definitions and reject definition updates that would lower the counter below its current value.
- **FR-013**: All sequence operations MUST be isolated per workspace (multi-tenant): no definition, counter, or allocation is ever visible to or affects another workspace.
- **FR-014**: System MUST support at least the entity types product, customer, order, and invoice at launch, and accept new entity types without code changes to the sequence module (entity type is an open string key).
- **FR-015**: System MUST expose a direct "generate next number now" operation (server-side, single number) for online callers and web clients that do not use block allocation.
- **FR-016**: The design MUST NOT preclude future enhancements: periodic resets (fiscal year/month), store/branch-specific scopes, dynamic prefix templates (e.g. `INV-2026-0001`), and an audit history of issued numbers. These are explicitly OUT of scope for v1.

### Key Entities

- **Sequence Definition**: A numbering scheme for one entity type within a workspace. Attributes: entity type, scope (workspace/user), owning user (nullable), prefix, suffix, padding length, start value, increment step, current value (high-water mark), active flag, timestamps. Uniqueness: one active definition per lookup key.
- **Sequence Allocation**: An exclusive contiguous range of counter values granted to one device for one sequence definition. Attributes: device ID, sequence definition reference, range start, range end, next available (device progress), allocation timestamp, status (active/exhausted/released). Ranges for the same definition never overlap.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Zero duplicate document numbers issued within a scope, measured across concurrent multi-user and multi-device test runs (including offline/reconnect cycles).
- **SC-002**: 99.99% of generation requests succeed; a user creating a record always receives a number (final or provisional) without manual intervention.
- **SC-003**: Next-number retrieval (server-side generation or allocation grant) completes in under 50ms at the service layer under normal load; local on-device generation from a block is instantaneous (no network round-trip).
- **SC-004**: The platform sustains 100,000+ number generations per day across thousands of concurrent users without counter corruption.
- **SC-005**: A device with an allocated block can create records fully offline for the entire block size (default 50 records) with no degradation, and reconciles on reconnect with zero conflicts.
- **SC-006**: Sequence-related support issues (duplicate/wrong numbers) reduce by 90% versus the pre-module baseline.

## Assumptions

- **Document number vs technical UID**: The generated sequence number is a human-facing document/reference number stored alongside the existing technical UID (32-char `UidGenerator` UIDs remain the primary keys); the sequence module does not replace UID generation.
- **Reservation model**: V1 implements reservation via device block allocation (a block IS a reservation of a range). A separate single-number server-side "reserve then consume" workflow is not built in v1; the direct-generate operation (FR-015) covers online single-number needs. This satisfies the PRD's drafting use case because a draft created on a device consumes from the device's block.
- **Default block size**: 50 numbers per allocation (PRD examples use 50-wide ranges), refreshed when the device drops below a low-water threshold (~20% remaining).
- **Abandoned blocks**: Never reclaimed or reissued in v1 — gaps are acceptable per FR-007; an expiry/return mechanism is a future enhancement.
- **Permissions**: Sequence definition management uses the existing workspace admin/role model; any workspace member may generate numbers and request allocations for their own devices.
- **Backfill**: Existing records are not renumbered; sequences apply to records created after the feature is enabled. Administrators set the start value above any legacy numbering to avoid visual collisions.
- **Mobile entity wiring**: V1 delivers the numbering service and device allocation client; wiring every entity creation flow (product/customer/order/invoice forms) to display/store the generated number is incremental and may land per-entity.

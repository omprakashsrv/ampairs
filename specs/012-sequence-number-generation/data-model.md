# Data Model: Sequence Number Generation Module (012)

## Backend (JPA, module `sequence`)

### SequenceDefinition — `sequence_definition` (extends `OwnableBaseDomain`)

Inherited: `id`, `uid` (prefix `SQD`), `owner_id` (`@TenantId`), `ref_id`, `created_at`, `updated_at`.

| Column | Type | Notes |
|---|---|---|
| entity_type | VARCHAR(64) NOT NULL | open string key: `product`, `customer`, `order`, `invoice`, … |
| scope | VARCHAR(16) NOT NULL | enum `WORKSPACE` / `USER` |
| user_id | VARCHAR(200) NULL | required iff scope = USER (owning user uid) |
| prefix | VARCHAR(32) NULL | |
| suffix | VARCHAR(32) NULL | |
| padding_length | INT NOT NULL DEFAULT 0 | 0 = no padding; max 18 |
| start_value | BIGINT NOT NULL DEFAULT 1 | first value to be issued |
| increment_step | INT NOT NULL DEFAULT 1 | ≥ 1 |
| current_value | BIGINT NOT NULL DEFAULT 0 | high-water mark = last issued/allocated value; 0 = nothing issued (next = start_value) |
| active | BOOLEAN NOT NULL DEFAULT TRUE | soft-delete / deactivate |

Indexes: unique `uid`; `(owner_id, entity_type)`; `(owner_id, updated_at)`.

**Invariants**
- At most one `active = true` row per (owner_id, entity_type, scope, user_id) — service-enforced (R2).
- `current_value` is monotonically non-decreasing; updates that would lower it are rejected (`InvalidSequenceUpdateException`).
- `next value = if (current_value < start_value) start_value else current_value + increment_step` (allows raising start_value later; never re-issues).
- Formatting: `[prefix-]paddedValue[-suffix]`, padding to `padding_length` with leading zeros.

### SequenceAllocation — `sequence_allocation` (extends `OwnableBaseDomain`)

Inherited: `id`, `uid` (prefix `SQA`), `owner_id`, `created_at`, `updated_at`.

| Column | Type | Notes |
|---|---|---|
| definition_uid | VARCHAR(200) NOT NULL | references `sequence_definition.uid` (no FK — soft reference, matches codebase style) |
| entity_type | VARCHAR(64) NOT NULL | denormalized for device lookups |
| device_id | VARCHAR(200) NOT NULL | opaque client device id |
| user_id | VARCHAR(200) NULL | requesting user (audit + user-scope resolution) |
| range_start | BIGINT NOT NULL | first value in block (inclusive) |
| range_end | BIGINT NOT NULL | last value in block (inclusive) |
| next_available | BIGINT NOT NULL | device progress; = range_start at grant; > range_end ⇒ exhausted |
| status | VARCHAR(16) NOT NULL | enum `ACTIVE` / `EXHAUSTED` / `RELEASED` |
| active | BOOLEAN NOT NULL DEFAULT TRUE | |

Indexes: unique `uid`; `(owner_id, device_id, status)`; `(owner_id, definition_uid)`.

**Invariants**
- Block grant: under definition row lock, `range_start = nextValue`, `range_end = range_start + (block_size − 1) × increment_step`, then `definition.current_value = range_end`. Ranges for one definition never overlap.
- Consumption report may only move `next_available` forward, clamped to `range_end + increment_step`; status flips to `EXHAUSTED` when past `range_end`.
- Abandoned ranges are never reclaimed (gaps OK, duplicates never).
- block_size: default 50, min 1, max 1000.

## Mobile (Room, `feature/sequence`, DB `sequence` v1, workspace-scoped)

### `sequence_definition` (SequenceDefinitionEntity)
Mirror of the server row: `uid` (PK), `entityType`, `scope`, `userId`, `prefix`, `suffix`, `paddingLength`, `startValue`, `incrementStep`, `currentValue`, `active`, `updatedAt` (ISO string), `synced: Boolean`.
Local edits set `synced = false` + `SyncStateDao.markPendingPush(SyncEntity.SEQUENCE)`.

### `sequence_allocation` (SequenceAllocationEntity)
`uid` (PK, server-issued), `definitionUid`, `entityType`, `deviceId`, `rangeStart`, `rangeEnd`, `nextAvailable`, `status`, **format snapshot**: `prefix`, `suffix`, `paddingLength`, `incrementStep` (copied from grant response so offline formatting never needs the definition row), `allocatedAt`, `synced: Boolean` (false = consumption not yet reported).

**Local consumption** (in one Room transaction): read ACTIVE allocation for entityType with `nextAvailable <= rangeEnd`, take `nextAvailable`, write `nextAvailable += incrementStep` (+ `EXHAUSTED` when past end), `synced = false`, then `markPendingPush(SEQUENCE)`.

## State transitions

```
SequenceDefinition.active: true ⇄ false (deactivate/reactivate; reactivation re-checks uniqueness)
SequenceAllocation.status: ACTIVE → EXHAUSTED (next_available > range_end)
                           ACTIVE → RELEASED  (reserved for future explicit release; unused in v1)
```

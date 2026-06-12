# API Contract: sequence module

Base path: `/sequence/v1` (client builds URLs via `ApiUrlBuilder.sequenceUrl("v1/…")` → `/api/sequence/v1/…`).
All endpoints require `X-Workspace-ID`; all responses are `ApiResponse<T>`; JSON is global snake_case.

## Definitions — canonical offline-sync contract

### GET `/sequence/v1/definitions/sync`
Query: `last_sync` (ISO, optional), `page` (0), `size` (100), `sort_by` (`updatedAt`), `sort_dir` (`ASC`).
→ `ApiResponse<PageResponse<SequenceDefinitionResponse>>` — feed **includes inactive rows**.

### POST `/sequence/v1/definitions/sync`
Body: `List<SequenceDefinitionRequest>` (UID-keyed bulk upsert; `active=false` rides in-band as delete).
→ `ApiResponse<List<SequenceDefinitionResponse>>`
Server rules: client `current_value` is ignored for existing rows (server counter authoritative); uniqueness of active key enforced; counter never lowered.

### CRUD conveniences (web/admin)
- `GET /sequence/v1/definitions` → `ApiResponse<List<SequenceDefinitionResponse>>` (all rows, active first)
- `POST /sequence/v1/definitions` body `SequenceDefinitionRequest` → `ApiResponse<SequenceDefinitionResponse>` (409 `SEQUENCE_DEFINITION_DUPLICATE` if active key exists)
- `PUT /sequence/v1/definitions/{uid}` body `SequenceDefinitionRequest` → `ApiResponse<SequenceDefinitionResponse>` (400 `SEQUENCE_INVALID_UPDATE` when lowering counter)
- `GET /sequence/v1/definitions/{uid}/preview` → `ApiResponse<SequencePreviewResponse>` (no counter advance)

### POST `/sequence/v1/definitions/next` — direct server-side generation (FR-015)
Body:
```json
{ "entity_type": "invoice" }
```
→ `ApiResponse<SequenceNumberResponse>`; resolves USER-scope (caller) → WORKSPACE-scope → auto-provisioned default; atomically advances the counter.

## Allocations — off-contract device RPC

### POST `/sequence/v1/allocations` — request a block
```json
{ "entity_type": "invoice", "device_id": "DEV…", "block_size": 50 }
```
`block_size` optional (default 50, min 1, max 1000).
→ `ApiResponse<SequenceAllocationResponse>`; 409 `SEQUENCE_DEFINITION_INACTIVE` if the resolved definition is inactive.

### POST `/sequence/v1/allocations/report` — report consumption (bulk)
```json
[ { "uid": "SQA…", "next_available": 1023 } ]
```
→ `ApiResponse<List<SequenceAllocationResponse>>`; progress only moves forward (regressions ignored, response carries server state); unknown uid → 404 `SEQUENCE_ALLOCATION_NOT_FOUND`.

### GET `/sequence/v1/allocations?device_id=DEV…&status=ACTIVE`
`status` optional. → `ApiResponse<List<SequenceAllocationResponse>>` (device recovery after reinstall).

## DTOs

### SequenceDefinitionRequest
```json
{
  "uid": "SQD… | null",
  "entity_type": "invoice",
  "scope": "WORKSPACE | USER",
  "user_id": "USR… | null",
  "prefix": "INV",
  "suffix": null,
  "padding_length": 0,
  "start_value": 1,
  "increment_step": 1,
  "active": true,
  "ref_id": null
}
```
Validation: `entity_type` not blank; `scope=USER` ⇒ `user_id` required; `padding_length` 0–18; `start_value ≥ 1`; `increment_step ≥ 1`.

### SequenceDefinitionResponse
All request fields + `uid`, `current_value`, `next_preview` (formatted next number), `created_at`, `updated_at`.

### SequencePreviewResponse
```json
{ "definition_uid": "SQD…", "next_value": 1045, "formatted": "INV-1045" }
```

### SequenceNumberResponse
```json
{ "definition_uid": "SQD…", "entity_type": "invoice", "value": 1251, "formatted": "INV-1251" }
```

### SequenceAllocationResponse
```json
{
  "uid": "SQA…",
  "definition_uid": "SQD…",
  "entity_type": "invoice",
  "device_id": "DEV…",
  "range_start": 1001,
  "range_end": 1050,
  "next_available": 1001,
  "status": "ACTIVE",
  "prefix": "INV",
  "suffix": null,
  "padding_length": 0,
  "increment_step": 1,
  "created_at": "2026-06-12T10:00:00Z",
  "updated_at": "2026-06-12T10:00:00Z"
}
```
(`prefix`/`suffix`/`padding_length`/`increment_step` are the definition format snapshot at grant time — clients format offline from these.)

## Error codes
`SEQUENCE_DEFINITION_NOT_FOUND` (404), `SEQUENCE_DEFINITION_DUPLICATE` (409), `SEQUENCE_DEFINITION_INACTIVE` (409), `SEQUENCE_INVALID_UPDATE` (400), `SEQUENCE_ALLOCATION_NOT_FOUND` (404).

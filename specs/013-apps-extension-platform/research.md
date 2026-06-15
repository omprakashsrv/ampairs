# Phase 0 Research: Apps & Extensions Connector Platform

All decisions below resolve the spec's clarified requirements against the existing Ampairs codebase. No `NEEDS CLARIFICATION` markers remain in the spec.

## R1. Where does the connector platform live?

- **Decision**: New backend bounded-context module **`connector`** is the system of record; the KMP client reuses its existing sync infrastructure; the Angular web app gets the catalogue/install/mapping UI.
- **Rationale**: Constitution Principle IX (module boundaries) — installations, credentials, mappings, and run history are a distinct bounded context. The existing module layout (`setting`, `customer`, `product`) is the template.
- **Alternatives considered**: Extend `setting` (rejected — overloads a config registry with credentials + execution + run history); put it all client-side (rejected — defeats the goal of backend-persisted, cross-device config).

## R2. Hosting model — client-side execution, backend persistence

- **Decision**: Tally is a **client-side** connector. Push/pull keeps executing in the Ampairs desktop app (`tally/` Ktor XML client + `shared/desktopMain/com/ampairs/tallysync/`). Config, field mapping, sync checkpoints, and run history move from `AppPreferencesDataStore` to the backend `connector` module, pulled to the client via a provider mirroring `StoreSettingsProvider` and a `ConnectorSyncDelegate` mirroring `StoreSyncDelegate` (new `SyncEntity.CONNECTOR`). Server-side connectors are deferred.
- **Rationale**: Matches clarifications (client-side hosting; backend persistence of config/mapping/checkpoints/run-history). `store` module already proves the server-driven-config pull pattern with delete-by-absence and a per-entity checkpoint.
- **Alternatives considered**: Server-side relay/agent for Tally (rejected in clarification — Tally stays client-executed); separate standalone connector app (rejected — runs inside existing client app).

## R3. Partial update — preventing unmapped/omitted columns from nulling data

- **Decision**: A **connector-scoped sparse upsert** write path. For each row the server computes the writable column set = **columns present in the row payload ∩ the connector installation's mapped allowlist for that entity**. It loads the existing record (matched by the entity's single `refId` within the workspace), applies only those columns, and leaves all others untouched. Columns outside the mapping are never written; columns omitted from a row are preserved; a mapped column present with explicit null intentionally clears the value.
- **Rationale**: FR-018/FR-018b/FR-018c. Reuses the proven `ProductService.updateProducts()` load-existing-then-copy-selected-fields merge pattern. The connector mapping (now backend-persisted) is the security/correctness allowlist; per-row presence is the intent signal.
- **Alternatives considered**: Server-derived uniform per-connector column set (rejected — can't express row1≠row2 column sets, would null missing columns); client field-mask per row (rejected by stakeholder in favour of presence); null-means-skip merge (rejected — can never clear a value, and conflates omitted with null).

## R4. Wire format that distinguishes "omitted" from "null"

- **Decision**: The connector sparse-upsert request models each row's mapped values as a **sparse map of column→value** (only the columns the row provides are keys), carried alongside the row's `ref_id`. Omitted columns are simply absent keys; an explicit null value is a present key with null. The server validates keys against the installation's mapped allowlist before applying.
- **Rationale**: FR-018c needs unambiguous presence. A `Map<String, ...>` (or Jackson `ObjectNode`) body preserves "key absent vs key present-with-null" natively, where a fixed Kotlin DTO with nullable fields cannot (unset → serialized null). This is confined to the connector write path; the global `/sync` fixed-DTO contract is unchanged (FR-018a).
- **Open implementation detail (for tasks/plan, not blocking)**: exact representation (`Map<String, JsonNode>` vs typed sparse wrapper vs `JsonNullable`) to be finalized in data-model/contracts against the project's Jackson setup. Tracked in Complexity Tracking.
- **Alternatives considered**: `JsonNullable<T>` per field on a fixed DTO (heavier, per-field boilerplate); separate `updated_fields: []` mask (the rejected client-field-mask approach).

## R5. Record identity across multiple connectors

- **Decision**: The entity's `refId` (on `OwnableBaseDomain`), or the client-authored `uid`, is the identity key — and the **only** match key. Matching is by `refId`/`uid` alone; **no business-key reconciliation**. A row that matches updates the record; a non-matching row creates a new one. A connector never overwrites another connector's `refId`. Field mapping is per connector installation; overlapping fields across connectors resolve by last-write-wins (timestamp).
- **Rationale**: FR-019/FR-019a/FR-019b — chosen "single refId/uid, one owner" + "allow overlap, last-write-wins"; the analysis-remediation decision dropped business-key reconciliation to keep matching deterministic and simple.
- **Alternatives considered**: Per-connector external-reference table (rejected — single refId); business-key reconciliation (rejected in remediation — refId/uid only); field ownership / priority order (rejected in favour of last-write-wins).

## R6. Entitlement / catalogue gating

- **Decision**: Reuse the existing workspace module-enablement / subscription-tier mechanism to gate which connectors appear in a workspace's catalogue (via the public service interfaces used by `setting`'s installed-modules/tier gating). No new entitlement system.
- **Rationale**: Spec assumption + Principle IX (cross-module via public service interfaces).
- **Alternatives considered**: New connector-specific entitlement table (rejected — duplicates existing tier/module gating).

## R7. Credentials security

- **Decision**: Connector credentials/secrets are stored encrypted at rest in `ConnectorConfig`, with the encryption key supplied via environment variable; response DTOs never include secret values (write-only / masked). A connection "test/validate" action reports success/failure without echoing secrets.
- **Rationale**: FR-008, Constitution Principle XI.
- **Alternatives considered**: Plaintext (rejected); external secret vault (over-scoped for first release; env-var-keyed encryption is consistent with existing secret hygiene).

## R8. Sync state & run history

- **Decision**: Backend persists per-installation, per-entity checkpoints (`ConnectorSyncCheckpoint`, ISO-8601/`Instant` watermark) and per-run audit records (`ConnectorSyncRun`: trigger, start/end, created/updated/failed counts, error). The client reports these up and reads them back so they survive reinstall/device change. Incremental, stateful, resumable (FR-016/FR-017/FR-020/FR-022).
- **Rationale**: Clarification chose "config + mapping + checkpoints + run history" persisted to backend.
- **Alternatives considered**: Keep checkpoints/history client-local (rejected — loses cross-device resume + server observability).

## R9. Two-way sync (future)

- **Decision**: Model `supportedDirections` on the catalogue `Connector` and a per-installation direction setting now, but only implement external→Ampairs (one-way) in the first release. Conflict authority = most-recent-update-wins (FR-031), already aligned with last-write-wins.
- **Rationale**: Stakeholder deferred two-way; platform must accommodate without re-architecture (FR-030).
- **Alternatives considered**: Build two-way now (rejected — out of scope).

## R10. Web repository availability

- **Decision**: The Angular `ampairs-web` repo is **not present** in this environment. The web workstream (catalogue, install, connection config form, data-mapping editor) is specified at the interface/endpoint level here and must be planned/implemented against the web repo separately, using Angular Material 3 only (Principle VIII).
- **Rationale**: Verified absent locally; cannot produce concrete web file paths/components without it.
- **Alternatives considered**: None — environmental constraint.
</content>

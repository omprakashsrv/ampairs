# agent module

Server-side support for the mobile app's **on-device AI assistant** (LiteRT-LM / Gemma). Top-level
bounded context (`com.ampairs.agent`) serving the `/agent/v1/**` namespace. Depends only on `:core`;
Spring discovers it via the default `com.ampairs` component scan (registered as a dependency of
`:ampairs_service`).

The model catalog is curated reference data in code (`AiModelCatalog`), not a tenant entity. The
module owns **one** DB table: `agent_chat_log` (opt-in chat telemetry, spec below) — so the module
now has Flyway migrations and **is** in `migrationModules`. It depends on `:core` plus
`spring-boot-starter-data-jpa`.

## What it owns
- `AiModelCatalog` / `AiModelDescriptor` — the curated list of downloadable LiteRT-LM `.litertlm`
  checkpoints (id, name, family, parameterLabel, **role**, fileName, sizeBytes, sha256, requiredRamMb,
  backendId, platforms, recommended, and a server-only `sourceUrl`).
- The model **manifest** + **download proxy** endpoints.

## Seeding the catalog (it's code, not a DB)
There is **no DB table and no SQL seed** — "seed/update the server model list" = edit
`AiModelCatalog.MODELS` (Kotlin) and ship a backend release. The list mirrors the **Google AI Edge
Gallery `model_allowlists/{appVersion}.json`** (e.g. `1_0_9.json`): copy `modelId` + `modelFile` +
`sizeInBytes` **verbatim**. That allowlist has no URL field — build `sourceUrl` as
`https://huggingface.co/{modelId}/resolve/main/{modelFile}`. Note the 270M tool-caller is listed there
as **TinyGarden-270M** (`google/functiongemma-270m-it` / `tiny_garden.litertlm`), not a literal
"function-gemma-270m".

## Model `role` (INTENT / CHAT / FALLBACK)
`AiModelDescriptor.role` + `AiModelResponse.role` (`ModelRole` enum, Jackson serializes as
`"INTENT"`/`"CHAT"`/`"FALLBACK"`). The app uses it to wire the tiny tool-caller (FunctionGemma-270M =
`INTENT`) separately from chat models — its `ProviderRegistry.selectedChatModel()` auto-picks only
`CHAT` models, so a mis-roled 270M would get auto-selected as a (poor) chat model. Mark conversational
models `CHAT`, the function-caller `INTENT`. (The app mirrors this in
`feature/agent/.../data/api/AiModelResponse.kt` → `roleFromManifest()`.)

## Endpoints (`/agent/v1/models`)
Global (not tenant-scoped) but JWT-authenticated (`SecurityConfiguration.anyRequest().authenticated()`).
No `SessionUserFilter` exemption: the app is always inside a workspace when it calls these, so the
`X-Workspace-ID` header is present; the endpoints just don't use tenant context.

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/agent/v1/models` | Manifest the app's model-picker renders. `ApiResponse<List<AiModelResponse>>`. |
| `GET` | `/agent/v1/models/{id}/download` | Streaming **proxy** of the `.litertlm` bytes. Forwards the client's `Range` header upstream and relays `206`/`Content-Range` so multi-GB downloads resume. Returns `application/octet-stream` (binary — the documented exception to the `ApiResponse` rule, like `file`). |

## Why a proxy (not direct URLs / redirect)
The app downloads through the backend so the upstream source stays opaque and there is one control
point (auth/logging/quota). `AiModelProxyService` uses the JDK `HttpClient`
(`BodyHandlers.ofInputStream`) for a bounded-memory streaming copy and follows redirects
(HF `resolve/main` → CDN).

## Catalog data caveat
`sourceUrl` / `sha256` / `sizeBytes` are best-known values for the LiteRT-LM repos and should be
verified against HuggingFace before GA. **`sizeBytes` must match the upstream file byte-for-byte** —
the app's downloader validates size and fails the download otherwise. A wrong `sourceUrl` only fails
the runtime download (`502`). **Neither is caught by CI** (both are runtime-only) — verify new entries
on-device. `sha256` is null today (verification skipped app-side until digests are filled in).

## Chat telemetry (`agent_chat_log`)
Opt-in (default OFF on the client) capture of assistant turns for later quality analysis.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/agent/v1/chat-logs` | Upload a batch of turns. Body `List<ChatLogRequest>` → `ApiResponse<ChatLogUploadResponse>` (saved count). |

- **Workspace-scoped**: `ChatLog : OwnableBaseDomain` → `owner_id` = the `X-Workspace-ID` the request
  carried. `SessionUserFilter` enforces auth + membership and sets the tenant context, so the
  controller does **not** touch `TenantContextHolder`.
- **User is server-stamped**: `user_id` comes from the security context (`AuthenticationHelper`), never
  the request body. `owner_id`/`user_id` are not client-supplied.
- One row per turn (`user_message` + `assistant_message`) plus `model_id` / `intent` / `module_name` /
  `action_type` / `client_timestamp` (epoch-millis in the DTO → `Instant` in the entity).
- **Write-mostly**: no read API yet; analysis is offline/admin. Cross-tenant reads would need
  `nativeQuery = true` to bypass `@TenantId` filtering.
- Flyway: `agent/src/main/resources/db/migration/{mysql,postgresql}/V1.0.102__create_agent_chat_log_table.sql`.

## App counterpart
`ampairs-app` → `feature/agent`: `ModelManager` fetches the manifest and downloads via the proxy
(`ApiUrlBuilder.agentUrl("v1/models/...")`), caches to `filesDir/agent_models/{fileName}`, and the
`ProviderRegistry` loads the selected model into the LiteRT-LM engine. The opt-in chat-telemetry
uploader POSTs to `ApiUrlBuilder.agentUrl("v1/chat-logs")`.

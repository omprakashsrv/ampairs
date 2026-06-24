# agent module

Server-side support for the mobile app's **on-device AI assistant** (LiteRT-LM / Gemma). Top-level
bounded context (`com.ampairs.agent`) serving the `/agent/v1/**` namespace. Depends only on `:core`;
Spring discovers it via the default `com.ampairs` component scan (registered as a dependency of
`:ampairs_service`).

This module owns **no DB tables** — the model catalog is curated reference data in code
(`AiModelCatalog`), not a tenant entity, so there are no Flyway migrations and it is **not** in
`migrationModules`.

## What it owns
- `AiModelCatalog` / `AiModelDescriptor` — the curated list of downloadable Gemma `.litertlm`
  checkpoints (id, fileName, sizeBytes, sha256, requiredRamMb, backendId, platforms, recommended,
  and a server-only `sourceUrl`).
- The model **manifest** + **download proxy** endpoints.

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
`sourceUrl` / `sha256` / `sizeBytes` are best-known values for the LiteRT-LM `litert-community`
Gemma repos and should be verified against HuggingFace before GA. A wrong `sourceUrl` only fails the
runtime download (`502`), never the build. `sha256` is null today (verification skipped app-side
until digests are filled in).

## App counterpart
`ampairs-app` → `feature/agent`: `ModelManager` fetches the manifest and downloads via the proxy
(`ApiUrlBuilder.agentUrl("v1/models/...")`), caches to `filesDir/agent_models/{fileName}`, and the
`ProviderRegistry` loads the selected model into the LiteRT-LM engine.

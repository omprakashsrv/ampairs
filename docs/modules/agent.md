# agent module

Server-side support for the mobile app's **on-device AI assistant** (LiteRT-LM / Gemma models). Owns the curated model catalog (code, not DB), the model manifest + download proxy, and opt-in chat telemetry.

## REST Endpoints (`/agent/v1`)

| Controller | Base path | Purpose |
|---|---|---|
| `AiModelController` | `/agent/v1/models` | Model manifest (`GET /models`) + download proxy (`GET /models/{id}/download`) |
| `AiChatController` | `/agent/v1/chat` | Chat-related server endpoints |
| `ChatLogController` | `/agent/v1/chat-logs` | Opt-in chat telemetry ingestion |

## Model catalog — code, not a DB table

- `AiModelCatalog.MODELS` (Kotlin object) is the source of truth for downloadable `.litertlm` checkpoints — id, name, family, parameterLabel, **role** (`INTENT`/`CHAT`/`FALLBACK`), fileName, `sizeBytes`, sha256, requiredRamMb, platforms, and a server-only `sourceUrl`.
- "Seed/update the server model list" = edit that Kotlin list and ship a backend release — there is no SQL seed.
- The list mirrors the Google AI Edge Gallery `model_allowlists/{appVersion}.json`. Copy `modelId` + `modelFile` + `sizeInBytes` **verbatim** (the app validates downloaded size byte-for-byte); build `sourceUrl` as `https://huggingface.co/{modelId}/resolve/main/{modelFile}`.
- The `role` matters: the app auto-selects only `CHAT` models for conversation; the tiny tool-caller must be `INTENT`.

## Data

The module owns **one** DB table: `agent_chat_log` (opt-in telemetry) — so it has Flyway migrations and is in `migrationModules`.

## Dependencies

- `:core` + `spring-boot-starter-data-jpa`.

See `agent/CLAUDE.md` for the full guidance (catalog rules, role semantics, seeding workflow).

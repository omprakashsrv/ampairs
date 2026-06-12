# Quickstart: Sequence Number Generation (012)

## Backend

```bash
cd ampairs
./gradlew :sequence:build            # compile + unit tests
./gradlew :ampairs_service:bootRun   # migrations V1.0.83 apply on start
```

Configure a workspace invoice sequence:

```bash
curl -X POST http://localhost:8080/api/sequence/v1/definitions \
  -H "Authorization: Bearer $TOKEN" -H "X-Workspace-ID: $WS" -H "Content-Type: application/json" \
  -d '{"entity_type":"invoice","scope":"WORKSPACE","prefix":"INV","padding_length":0,"start_value":1001,"increment_step":1}'
```

Generate one number (server-side):

```bash
curl -X POST .../api/sequence/v1/definitions/next -d '{"entity_type":"invoice"}' …
# → {"data":{"definition_uid":"SQD…","entity_type":"invoice","value":1001,"formatted":"INV-1001"}}
```

Grant a device block:

```bash
curl -X POST .../api/sequence/v1/allocations -d '{"entity_type":"invoice","device_id":"DEV1","block_size":50}' …
# → range_start 1002, range_end 1051; definition current_value advances to 1051
```

Report consumption:

```bash
curl -X POST .../api/sequence/v1/allocations/report -d '[{"uid":"SQA…","next_available":1010}]' …
```

## Mobile

```bash
cd ampairs-app
./gradlew androidApp:compileDebugKotlinAndroid shared:compileKotlinIosSimulatorArm64 desktopApp:compileKotlin
```

From any ViewModel in the workspace graph:

```kotlin
@Inject class InvoiceFormViewModel(
    private val sequenceNumberProvider: SequenceNumberProvider,
) : ViewModel() {
    suspend fun nextInvoiceNumber(): SequenceNumberResult =
        sequenceNumberProvider.next("invoice")
        // online or with a local block → final number (e.g. INV-1003)
        // offline + exhausted block → provisional = true result; finalize on reconnect
}
```

Definitions sync automatically via `CentralSyncService` (`SyncEntity.SEQUENCE`); consumption reports ride the same delegate's push.

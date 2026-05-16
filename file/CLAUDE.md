# file module

Object storage abstraction — S3 (production), MinIO (self-hosted), local (dev).

## Storage path convention
`{workspace_slug}/{entity_type}/{entity_uid}/{file_uid}.{ext}`

## Key entity
- `File` — name, bucket, objectKey, contentType, size, etag

## Extension detection order
1. From filename → 2. From MIME type → 3. Default `jpg`

## Backend selection
Configured via Spring profile — all backends implement `ObjectStorageService` interface.
Production: S3. Dev: local filesystem (served at `/files/{bucket}/**`).

## Env vars
`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `AWS_S3_BUCKET`

## Full docs
`docs/modules/file.md`

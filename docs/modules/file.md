# file module

Object storage abstraction for file upload, download, thumbnail generation, and caching. Supports S3, MinIO, and local filesystem backends.

## Storage Backends

| Backend | Class | Use case |
|---------|-------|---------|
| AWS S3 | `S3ObjectStorageService` | Production |
| MinIO | `MinioObjectStorageService` | Self-hosted / on-premise |
| Local filesystem | `LocalObjectStorageService` | Development / CI |

The active backend is selected via Spring configuration. All backends implement `ObjectStorageService` interface, so switching is a config change only.

## REST Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/files/{bucket}/**` | Serve file from local storage (dev only) |
| GET | `/files/health` | Storage health check |

In production, files are served directly from S3/MinIO signed URLs — the backend does not proxy the content.

## Key Entity

### File

```kotlin
class File : BaseDomain() {
    val name: String           // original filename
    val bucket: String         // S3 bucket or storage root
    val objectKey: String      // full path within bucket
    val contentType: String?   // MIME type
    val size: Long?            // bytes
    val etag: String?          // S3 ETag for integrity check
}
```

## Storage Path Convention

Used across all modules that store files:

```
{workspace_slug}/{entity_type}/{entity_uid}/{file_uid}.{ext}
```

Examples:
```
acme-corp/customer/CUS_abc123/IMG_xyz.jpg
acme-corp/business/BIZ_def456/logo.png
acme-corp/product/PRD_ghi789/variant_thumb.webp
```

## Thumbnail Generation

`ImageResizingService` generates thumbnails on upload. Thumbnails are cached via `ThumbnailCacheService`.

Standard thumbnail dimensions used across modules:
- Logo: 256 × 256
- Product images: 200 × 200
- Customer images: module-specific

## File Extension Detection

`FileValidationService` detects extension from:
1. Filename (e.g. `photo.jpg` → `jpg`)
2. MIME type fallback: `image/jpeg` → `jpg`, `image/png` → `png`, `image/webp` → `webp`
3. Default: `jpg`

## AWS Configuration

```bash
AWS_ACCESS_KEY_ID=xxx
AWS_SECRET_ACCESS_KEY=xxx
AWS_REGION=ap-south-1
AWS_S3_BUCKET=ampairs-storage
```

## Package Structure

```
com.ampairs.file
├── config/         — SpringCloudAwsConfig
├── controller/     — FileController (local dev serving)
├── domain/
│   ├── dto/        — FileResponse
│   ├── model/      — File
│   └── service/    — FileService
├── exception/      — FileExceptionHandler
├── repository/     — FileRepository
├── service/        — FileValidationService, ImageResizingService, ThumbnailCacheService
└── storage/        — ObjectStorageService (interface),
                      S3ObjectStorageService, MinioObjectStorageService,
                      LocalObjectStorageService
```

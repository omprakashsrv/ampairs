# File Module

The `file` module isolates file and object storage responsibilities that were
previously embedded in `core`. It provides:

- Spring Boot configuration properties for AWS S3, MinIO, and local storage.
- `File` JPA entity, repository, and DTO helpers for persisting metadata.
- Storage abstractions (`ObjectStorageService`) with S3, MinIO, and local implementations.
- Image processing utilities (`ImageResizingService`, `ThumbnailCacheService`) and validation helpers.
- REST controller for serving files when using the local storage provider.

Other feature modules (e.g., `product`, `customer`) can depend on this module
to upload, manage, and retrieve binary assets without pulling additional
responsibilities into `core`.

# Core Module

## Overview

The Core module serves as the foundation of the Ampairs application, providing shared utilities, domain models, AWS
configuration, and multi-tenancy support. This module contains common components used across all other modules in the
application.

## Architecture

### Package Structure

```
com.ampairs.core/
├── config/                 # Configuration classes
├── controller/             # Test controllers
├── domain/                # Domain models and DTOs
│   ├── dto/               # Data Transfer Objects
│   ├── enums/             # Enumeration types
│   ├── model/             # Entity models
│   └── service/           # Domain services
├── exception/             # Global exception handling
├── multitenancy/          # Multi-tenant support
├── repository/            # Data access layer
└── utils/                 # Utility classes
```

## Key Components

### Configuration Classes

- **`ApplicationProperties.kt`** - Centralized type-safe configuration properties for security, cache, and integrations
- **`AWSConfig.kt`** - AWS service configuration with IAM role support and credential management
- **`CacheConfig.kt`** - Caching infrastructure configuration using Caffeine
- **`RateLimitConfig.kt`** - Rate limiting configuration using Bucket4j
- **`Constants.kt`** - Application-wide constants

### Domain Models

#### Base Entities

- **`BaseDomain.kt`** - Abstract base entity with common fields (id, seqId, timestamps)
- **`OwnableBaseDomain.kt`** - Extended base for tenant-aware entities
- **`AbstractIdVerification.kt`** - Base for entities requiring ID verification

#### Core Models

- **`Address.kt`** - Common address model used across modules

### Multi-tenancy Support

- **`TenantContext.kt`** - Thread-local tenant storage for request-scoped tenant identification
- **`TenantIdentifierResolver.kt`** - Hibernate multi-tenant identifier resolver
- **`TenantFilter.kt`** - HTTP filter for tenant identification from requests

### Services

- **`ValidationService.kt`** - Central validation helpers for request payloads

### Exception Handling

- **`GlobalExceptionHandler.kt`** - Centralized exception handling
- **`BaseExceptionHandler.kt`** - Base exception handler with common response patterns
- **`GenericResponseAdvise.kt`** - Response wrapper for consistent API responses
- **`AuthEntryPointJwt.kt`** - JWT authentication entry point

### Utilities

- **`Helper.kt`** - Common utility functions
- **`PropertiesUtils.kt`** - Property manipulation utilities
- **`UniqueIdGenerator.kt`** - Unique ID generation utilities

## Key Features

### Multi-tenancy Architecture

- Thread-local tenant context management
- Hibernate-based multi-tenant data isolation
- Request-level tenant identification

### AWS Integration

- S3 file storage with metadata tracking
- IAM role-based credential management
- Error handling and retry mechanisms

### Configuration Management

- Type-safe configuration properties
- Environment-based configuration
- Centralized configuration patterns

### Caching & Rate Limiting

- Caffeine cache provider integration
- Bucket4j rate limiting support
- Configurable cache policies

### Global Exception Handling

- Standardized error responses
- HTTP status code mapping
- Comprehensive error logging

## Dependencies

### Core Dependencies

- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Cache
- Hibernate Core
- AWS SDK for S3

### Utility Libraries

- Caffeine (Caching)
- Bucket4j (Rate Limiting)
- Jackson (JSON Processing)

## Usage

### Multi-tenancy Usage

```kotlin
// Set tenant context
TenantContext.setCurrentTenant("tenant-id")

// Create tenant-aware entity
class MyEntity : OwnableBaseDomain() {
    // Entity properties
}
```

### File Service Usage

File storage responsibilities now live in the dedicated `file` module.
Import `com.ampairs.file.domain.service.FileService` in feature modules that need to
upload or manage binary assets.

### Configuration Usage

```kotlin
@ConfigurationProperties(prefix = "ampairs")
class ApplicationProperties {
    val security: Security = Security()
    val aws: Aws = Aws()
    val cache: Cache = Cache()
}
```

## Testing

The module includes comprehensive unit tests covering:

- Multi-tenancy functionality
- File service operations
- Configuration validation
- Exception handling scenarios

## Build & Deployment

### Build Commands

```bash
# Build the module
./gradlew :core:build

# Run tests
./gradlew :core:test

# Clean build
./gradlew :core:clean build
```

### Configuration

Ensure proper configuration in `application.yml`:

```yaml
ampairs:
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: 86400000
  aws:
    region: ${AWS_REGION:us-east-1}
    s3:
      bucket: ${S3_BUCKET_NAME}
  cache:
    spec: "maximumSize=10000,expireAfterWrite=15m"
```

## Integration

This module is designed to be imported by all other modules in the Ampairs application. It provides the foundational
infrastructure required for:

- Authentication and authorization
- Multi-tenant data management
- File storage and retrieval
- Caching and rate limiting
- Global error handling
- Configuration management

Other modules should extend the base domain classes and utilize the provided services for consistent behavior across the
application.

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Ampairs is a modern, multi-module Spring Boot application built with Kotlin that provides comprehensive business
management functionality including authentication, customer management, product/inventory management, order processing,
and invoice generation. The application features a secure, multi-tenant architecture with robust integrations including
Tally ERP synchronization and AWS cloud services.

## Recent Architecture Improvements (2025)

The application has been significantly restructured with modern Spring Boot best practices:

### Security Enhancements

- **Removed hardcoded credentials** - All AWS and sensitive configurations now use environment variables or IAM roles
- **Enhanced JWT implementation** - Added proper token validation, refresh token support, and tenant-aware claims
- **Multi-tenant security** - JWT tokens now include tenant context and user roles
- **Comprehensive error handling** - Global exception handler with proper HTTP status codes and error responses

### Configuration Management

- **Centralized properties** - All configuration consolidated in `ApplicationProperties` with proper type safety
- **Environment-based configuration** - All values externalized with sensible defaults
- **Profile-aware setup** - Different configurations for development, testing, and production environments

### Integration Improvements

- **AWS Integration** - Proper credential management with IAM role support, enhanced S3 service with error handling
- **Tally Integration** - Retry mechanisms, proper error handling, and connection pooling
- **File Management** - Enhanced file service with metadata tracking and security features

## Architecture

### Module Structure

This is a Gradle multi-module project with the following modules:

- **core**: Shared utilities, domain models, AWS configuration, and multi-tenancy support
- **auth**: Authentication and JWT token management with user management
- **workspace**: Workspace and location management with role-based access
- **customer**: Customer management with pagination support
- **product**: Product catalog, inventory, tax management, and product categorization
- **order**: Order processing and management
- **invoice**: Invoice generation and management
- **ampairs_service**: Main Spring Boot application that aggregates all modules
- **tally**: Tally ERP integration service with sync tasks
- **tally_core**: Core Tally XML processing and client communication

### Technology Stack

- **Language**: Kotlin 2.2.0 with Java 21
- **Framework**: Spring Boot 3.5.3
- **Database**: MySQL with JPA/Hibernate
- **Security**: Spring Security with JWT tokens
- **Build Tool**: Gradle with Kotlin DSL
- **Caching**: Caffeine with rate limiting via Bucket4j
- **Cloud Services**: AWS S3 integration, SNS notifications
- **External Integration**: Tally ERP via XML/HTTP

## Development Commands

### Building

```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :module_name:build

# Clean and build
./gradlew clean build
```

### Running

```bash
# Run main application
./gradlew :ampairs_service:bootRun

# Run Tally integration service
./gradlew :tally:bootRun
```

### Testing

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :module_name:test
```

### Deployment

The `ampairs_service/build_run.sh` script handles production deployment:

- Builds the application JAR
- Creates deployment package
- Uploads to production server via SCP
- Restarts the systemd service

## Key Architectural Patterns

### Multi-tenancy

The application supports multi-tenancy through:

- `TenantContext` for request-scoped tenant identification
- `TenantIdentifierResolver` for Hibernate session management
- Base domain classes that include tenant information

### Domain-Driven Design

Each module follows DDD patterns:

- `controller/`: REST endpoints
- `service/`: Business logic
- `repository/`: Data access layer
- `domain/model/`: Entity definitions
- `domain/dto/`: Data transfer objects
- `domain/enums/`: Enumeration types

### Security Architecture

- JWT-based authentication with refresh tokens
- Rate limiting on authentication endpoints (1 request per 20 seconds for /auth/v1/init)
- Global rate limiting (20 requests per minute per IP)
- Role-based access control through company associations

### Integration Patterns

- **Tally Integration**: XML-based communication with scheduled sync tasks
- **AWS Services**: S3 for file storage, SNS for notifications
- **Database**: Connection pooling with HikariCP (20 max connections)

## Configuration

### Database

- Uses MySQL with CamelCase to underscore naming strategy
- Connection pool configured for 20 max connections
- DDL auto-update enabled for development

### Caching

- Caffeine cache provider with JCache API
- Rate limiting buckets cached for 15 minutes
- Maximum 10,000 entries per cache

### Logging

- Structured logging with file rotation (7 days retention, 1000MB max file size)
- Access logs enabled with custom pattern
- Separate log files per service module

## Development Guidelines

### Code Organization

- Follow existing package structure: `com.ampairs.{module}.{layer}`
- Use Kotlin data classes for DTOs
- Implement proper error handling with standardized error responses
- Maintain separation of concerns between layers

### Database Entities

- Extend `BaseDomain` or `OwnableBaseDomain` for common fields
- Use JPA annotations with Kotlin compatibility
- Follow underscore naming convention in database

### API Design

- REST endpoints should follow `/api/v1/{resource}` pattern
- Use proper HTTP status codes
- Implement pagination for list endpoints
- Include proper validation annotations

### Testing

- Use JUnit 5 platform
- Follow existing test structure in each module
- Mock external dependencies appropriately
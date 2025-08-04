# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Ampairs is a modern, multi-module Spring Boot application built with Kotlin that provides comprehensive business
management functionality including authentication, customer management, product/inventory management, order processing,
and invoice generation. The application features a secure, multi-tenant architecture with robust AWS cloud services
integration.

## Recent Architecture Improvements (2025)

The application has been significantly restructured with modern Spring Boot best practices:

### Security Enhancements

- **Removed hardcoded credentials** - All AWS and sensitive configurations now use environment variables or IAM roles
- **Enhanced JWT implementation** - Added proper token validation, refresh token support, and tenant-aware claims
- **Multi-device authentication** - Support for multiple concurrent logins with device tracking and management
- **Multi-tenant security** - JWT tokens now include tenant context and user roles
- **Device-aware security** - JWT tokens include device_id for device-specific session management
- **Comprehensive error handling** - Global exception handler with proper HTTP status codes and error responses

### Configuration Management

- **Centralized properties** - All configuration consolidated in `ApplicationProperties` with proper type safety
- **Environment-based configuration** - All values externalized with sensible defaults
- **Profile-aware setup** - Different configurations for development, testing, and production environments

### Integration Improvements

- **AWS Integration** - Proper credential management with IAM role support, enhanced S3 service with error handling
- **File Management** - Enhanced file service with metadata tracking and security features

## Architecture

### System Architecture Overview

Ampairs follows a **Multi-Module Microservice Architecture** with the following key architectural principles:

- **Domain-Driven Design (DDD)**: Each module represents a bounded context with clear domain boundaries
- **Multi-Tenancy**: Tenant-aware data isolation and security at all levels
- **Event-Driven Communication**: Asynchronous processing for cross-module operations
- **Layered Architecture**: Clear separation of concerns with controller, service, repository, and domain layers
- **API-First Design**: RESTful APIs with comprehensive documentation and versioning

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Applications                       │
│                    (Web, Mobile, API Clients)                   │
└─────────────────────────┬───────────────────────────────────────┘
                          │ HTTPS/REST API
┌─────────────────────────▼───────────────────────────────────────┐
│                     API Gateway Layer                           │
│                   (ampairs_service)                             │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐              │
│  │ Load        │ │ Rate        │ │ Security    │              │
│  │ Balancing   │ │ Limiting    │ │ (JWT/CORS)  │              │
│  └─────────────┘ └─────────────┘ └─────────────┘              │
└─────────────────────────┬───────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────────┐
│                    Business Logic Layer                         │
│                    (Domain Modules)                             │
│  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐     │
│  │   Auth    │ │ Workspace │ │ Customer  │ │  Product  │     │
│  │  Module   │ │  Module   │ │  Module   │ │  Module   │     │
│  └───────────┘ └───────────┘ └───────────┘ └───────────┘     │
│  ┌───────────┐ ┌───────────┐                                 │
│  │   Order   │ │  Invoice  │                                 │
│  │  Module   │ │  Module   │                                 │
│  └───────────┘ └───────────┘                                 │
└─────────────────────────┬───────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────────┐
│                    Foundation Layer                             │
│                     (Core Module)                               │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐              │
│  │Multi-tenancy│ │   AWS       │ │  Exception  │              │
│  │   Support   │ │Integration  │ │  Handling   │              │
│  └─────────────┘ └─────────────┘ └─────────────┘              │
└─────────────────────────┬───────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────────┐
│                   Data & Integration Layer                      │
│  ┌─────────────┐ ┌─────────────┐                              │
│  │   MySQL     │ │   AWS S3    │                              │
│  │  Database   │ │   Storage   │                              │
│  └─────────────┘ └─────────────┘                              │
└─────────────────────────────────────────────────────────────────┘
```

### Module Architecture Patterns

#### 1. **Hexagonal Architecture (Ports & Adapters)**

```
┌─────────────────────────────────────────────────────────────┐
│                     Module Architecture                     │
│                                                             │
│  ┌─────────────┐    ┌─────────────────┐    ┌─────────────┐  │
│  │   REST      │    │                 │    │   Database  │  │
│  │ Controllers │◄──►│   Domain Core   │◄──►│ Repositories│  │
│  │ (Adapters)  │    │   (Business     │    │ (Adapters)  │  │
│  └─────────────┘    │    Logic)       │    └─────────────┘  │
│                     │                 │                     │
│  ┌─────────────┐    │  ┌───────────┐  │    ┌─────────────┐  │
│  │   Event     │    │  │ Services  │  │    │   External  │  │
│  │ Handlers    │◄──►│  │   DTOs    │  │◄──►│    APIs     │  │
│  │ (Adapters)  │    │  │ Entities  │  │    │ (Adapters)  │  │
│  └─────────────┘    │  └───────────┘  │    └─────────────┘  │
│                     └─────────────────┘                     │
└─────────────────────────────────────────────────────────────┘
```

#### 2. **Domain-Driven Design Layers**

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│           (Controllers, DTOs, Request/Response)             │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                   Application Layer                         │
│              (Services, Use Cases, Workflows)               │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                     Domain Layer                            │
│          (Entities, Value Objects, Domain Services)         │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                  Infrastructure Layer                       │
│         (Repositories, External APIs, Persistence)          │
└─────────────────────────────────────────────────────────────┘
```

### Module Structure & Dependencies

This is a Gradle multi-module project organized by domain boundaries:

#### **Foundation Layer**
- **core**: Shared utilities, domain models, AWS configuration, and multi-tenancy support

#### **Security & Identity Layer**
- **auth**: Authentication and JWT token management with user management
- **workspace**: Workspace and location management with role-based access

#### **Business Domain Layer**
- **customer**: Customer management with pagination support
- **product**: Product catalog, inventory, tax management, and product categorization
- **order**: Order processing and management
- **invoice**: Invoice generation and management

#### **Application & Integration Layer**
- **ampairs_service**: Main Spring Boot application that aggregates all modules

### Module Dependency Graph

```
                    ┌─────────────────┐
                    │ ampairs_service │
                    │   (Main App)    │
                    └─────────┬───────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
    ┌─────────▼─────────┐ ┌───▼────┐ ┌───────▼───────┐
    │     customer      │ │ product│ │     order     │
    │                   │ │        │ │               │
    └─────────┬─────────┘ └───┬────┘ └───────┬───────┘
              │               │               │
              └───────────────┼───────────────┘
                              │
                    ┌─────────▼─────────┐
                    │     invoice       │
                    │                   │
                    └─────────┬─────────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
    ┌─────────▼─────────┐ ┌───▼────┐ ┌───────▼───────┐
    │      auth         │ │workspace│ │     core      │
    │                   │ │        │ │   (Foundation)│
    └───────────────────┘ └────────┘ └───────────────┘
```

### Cross-Cutting Concerns Architecture

#### **Multi-Tenancy Architecture**

```
┌─────────────────────────────────────────────────────────────┐
│                      Request Flow                           │
│                                                             │
│  HTTP Request → JWT Token → Tenant Context → Data Access   │
│                                                             │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐      │
│  │   JWT       │   │   Tenant    │   │   Database  │      │
│  │ Validator   │──►│  Resolver   │──►│   Filter    │      │
│  └─────────────┘   └─────────────┘   └─────────────┘      │
│                                                             │
│  Every database query automatically includes:               │
│  WHERE tenant_id = :currentTenantId                         │
└─────────────────────────────────────────────────────────────┘
```

#### **Security Architecture**

```
┌─────────────────────────────────────────────────────────────┐
│                    Security Layers                          │
│                                                             │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐      │
│  │    CORS     │   │    Rate     │   │     JWT     │      │
│  │  Headers    │──►│  Limiting   │──►│    Auth     │      │
│  └─────────────┘   └─────────────┘   └─────────────┘      │
│                                           │                 │
│  ┌─────────────┐   ┌─────────────┐       │                │
│  │   Method    │   │    Role     │       │                │
│  │  Security   │◄──│   Based     │◄──────┘                │
│  └─────────────┘   └─────────────┘                        │
└─────────────────────────────────────────────────────────────┘
```

#### **Data Flow Architecture**

```
┌─────────────────────────────────────────────────────────────┐
│                   Data Processing Flow                      │
│                                                             │
│  Request → Validation → Business Logic → Persistence       │
│                                                             │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐      │
│  │   Input     │   │  Business   │   │  Database   │      │
│  │ Validation  │──►│   Rules     │──►│ Transaction │      │
│  └─────────────┘   └─────────────┘   └─────────────┘      │
│                                                             │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐      │
│  │   Cache     │   │   Event     │   │   Response  │      │
│  │  Updates    │◄──│ Publishing  │◄──│ Generation  │      │
│  └─────────────┘   └─────────────┘   └─────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### API Architecture

#### **RESTful API Design Principles**

- **Resource-Based URLs**: `/api/v1/{resource}/{id}`
- **HTTP Methods**: GET, POST, PUT, DELETE for CRUD operations
- **Status Codes**: Proper HTTP status codes for all responses
- **Versioning**: URL-based versioning (`/api/v1/`)
- **Content Negotiation**: JSON as primary content type
- **Pagination**: Consistent pagination for list endpoints
- **Error Handling**: Standardized error response format

#### **API Response Format**

```json
{
  "success": true,
  "data": {
    "id": "12345",
    "name": "Sample Resource",
    "createdAt": "2023-01-01T12:00:00Z"
  },
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  },
  "metadata": {
    "timestamp": "2023-01-01T12:00:00Z",
    "version": "v1"
  }
}
```

#### **Error Response Format**

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input data",
    "details": {
      "field": "email",
      "issue": "Invalid email format"
    },
    "timestamp": "2023-01-01T12:00:00Z"
  }
}
```

### Technology Stack

#### **Core Technologies**
- **Language**: Kotlin 2.2.0 with Java 21
- **Framework**: Spring Boot 3.5.3
- **Database**: MySQL 8.0 with JPA/Hibernate
- **Security**: Spring Security 6.x with JWT tokens
- **Build Tool**: Gradle 8.x with Kotlin DSL

#### **Persistence & Data**

- **ORM**: Hibernate 6.x with multi-tenancy support
- **Connection Pooling**: HikariCP (20 max connections)
- **Database Migrations**: Hibernate DDL auto-update
- **Caching**: Caffeine cache with JCache API
- **JSON Processing**: Jackson with JAXB support

#### **Security & Authentication**

- **Authentication**: JWT with refresh tokens
- **Authorization**: Role-based access control (RBAC)
- **Rate Limiting**: Bucket4j with Redis backend
- **CORS**: Configurable cross-origin support
- **Multi-tenancy**: Tenant-aware data isolation

#### **Cloud & External Services**

- **File Storage**: AWS S3 with metadata tracking
- **Notifications**: AWS SNS for SMS/email
- **Monitoring**: Micrometer with Prometheus metrics
- **External Integration**: Third-party API integrations via REST/HTTP

#### **Development & Operations**

- **Testing**: JUnit 5, Mockito, TestContainers
- **Documentation**: OpenAPI 3.0 (Swagger)
- **Logging**: Logback with structured JSON logging
- **Deployment**: Docker containers with systemd services
- **Monitoring**: Spring Boot Actuator with health checks

### Deployment Architecture

#### **Production Environment**

```
┌─────────────────────────────────────────────────────────────┐
│                     Load Balancer                          │
│                    (Nginx/HAProxy)                         │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTPS/SSL
┌─────────────────────────▼───────────────────────────────────┐
│                  Application Servers                       │
│   ┌─────────────┐   ┌─────────────┐                       │
│   │ Ampairs     │   │   Static    │                       │
│   │ Service     │   │   Assets    │                       │
│   │ (Port 8080) │   │ (CDN/S3)    │                       │
│   └─────────────┘   └─────────────┘                       │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    Data Layer                               │
│   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐     │
│   │   MySQL     │   │   Redis     │   │    AWS      │     │
│   │  Database   │   │   Cache     │   │  Services   │     │
│   │ (Primary)   │   │             │   │  (S3, SNS)  │     │
│   └─────────────┘   └─────────────┘   └─────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

#### **Development Environment**

```
┌─────────────────────────────────────────────────────────────┐
│                    Local Development                        │
│                                                             │
│   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐     │
│   │ Ampairs     │   │   MySQL     │   │    AWS      │     │
│   │ Service     │   │  (Docker)   │   │ LocalStack  │     │
│   │ (IDE/Gradle)│   │             │   │ (Optional)  │     │
│   └─────────────┘   └─────────────┘   └─────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

### Data Architecture

#### **Database Schema Design**

```sql
-- Multi-tenant base structure (conceptual representation)
-- All entities extend from BaseDomain or OwnableBaseDomain via JPA inheritance

-- Base domain fields (via @MappedSuperclass)
-- id: VARCHAR(36) PRIMARY KEY
-- seq_id: BIGINT AUTO_INCREMENT UNIQUE  
-- created_at: TIMESTAMP DEFAULT CURRENT_TIMESTAMP
-- updated_at: TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
-- version: INT DEFAULT 0

-- Tenant-aware entities extend OwnableBaseDomain adding:
-- tenant_id: VARCHAR(36) NOT NULL
-- created_by: VARCHAR(36)
-- updated_by: VARCHAR(36)

-- Key indexes for performance:
-- idx_tenant_id, idx_created_at, idx_updated_at
-- idx_tenant_created (tenant_id, created_at)
```

#### **Entity Relationships**

```
Workspace (1) ────── (N) User_Company ────── (N) User
    │                                            │
    │                                            │
    └── (1:N) ── Customer ── (1:N) ── Order ─── (N:1) ── Auth_Session
                     │           │
                     │           └── (1:N) ── Order_Item ── (N:1) ── Product
                     │                                           │
                     └── (1:N) ── Invoice ── (1:N) ── Invoice_Item ──┘
                                     │
                                     └── (N:1) ── Tax_Code
```

### Performance & Scalability Architecture

#### **Caching Strategy**

```
┌─────────────────────────────────────────────────────────────┐
│                     Cache Layers                            │
│                                                             │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐      │
│  │   L1 Cache  │   │   L2 Cache  │   │   L3 Cache  │      │
│  │ (Caffeine)  │   │   (Redis)   │   │ (Database)  │      │
│  │  In-Memory  │   │ Distributed │   │ Persistent  │      │
│  └─────────────┘   └─────────────┘   └─────────────┘      │
│                                                             │
│  Cache TTL: 15min   Cache TTL: 1hr   Cache TTL: 24hr      │
│  Max Size: 10k      Max Size: 100k   Max Size: Unlimited  │
└─────────────────────────────────────────────────────────────┘
```

#### **Rate Limiting Strategy**

```
┌─────────────────────────────────────────────────────────────┐
│                   Rate Limiting Tiers                      │
│                                                             │
│  Global: 20 req/min per IP                                 │
│  Auth Endpoints: 1 req/20sec per IP                       │
│  API Endpoints: 100 req/min per user                      │
│  File Upload: 10 req/min per user                         │
│  Bulk Operations: 5 req/min per user                      │
└─────────────────────────────────────────────────────────────┘
```

### Security Architecture Details

#### **Multi-Device Authentication Flow**

```
1. User Login Request → Device Info Extraction → OTP Generation → SMS/Email Delivery
2. OTP Verification → Device Session Creation → JWT Token Generation (with device_id)
3. API Request → JWT Validation → Device Validation → Tenant Resolution → Authorization
4. Token Refresh → Device Session Update → New JWT Generation
5. Device Logout → Device Session Deactivation (other devices remain active)
6. Logout All Devices → All Device Sessions Deactivated → All Tokens Revoked
```

#### **Device Session Management**

The application supports multiple concurrent logins from different devices:

- **Device Identification**: Each device gets a unique device_id (client-provided for mobile, server-generated for web)
- **Device Tracking**: Comprehensive device information including browser, OS, IP address, and user agent
- **Session Isolation**: Each device maintains its own session with independent refresh tokens
- **Device-Specific Operations**: Login, logout, and token refresh are device-specific
- **Security Monitoring**: Track login history, IP addresses, and suspicious activity per device

**Device Session Entity Structure:**

```kotlin
DeviceSession {
  deviceId: String           // Unique device identifier
  deviceName: String         // Human-readable device name
  deviceType: String         // mobile, desktop, tablet
  platform: String           // iOS, Android, Web
  browser: String            // Chrome, Safari, Mobile App
  os: String                 // iOS 17.1, Windows 11, etc.
  ipAddress: String          // Current IP address
  userAgent: String          // Full user agent string
  location: String?          // Optional location based on IP
  lastActivity: LocalDateTime // Last API request timestamp
  loginTime: LocalDateTime   // Initial login timestamp
  isActive: Boolean          // Session status
  refreshTokenHash: String   // Hashed refresh token
}
```

**API Endpoints for Device Management:**
```
GET    /auth/v1/devices                    # List all active devices
POST   /auth/v1/devices/{deviceId}/logout  # Logout specific device
POST   /auth/v1/logout                     # Logout current device
POST   /auth/v1/logout/all                 # Logout all devices
```

#### **Authorization Matrix**

```
Role         | OWNER | ADMIN | MANAGER | EMPLOYEE | VIEWER
-------------|-------|-------|---------|----------|--------
User Mgmt    |   ✓   |   ✓   |    ✗    |    ✗     |   ✗
Data CRUD    |   ✓   |   ✓   |    ✓    |    ✓     |   ✗
Reports      |   ✓   |   ✓   |    ✓    |  Limited | Limited
Settings     |   ✓   |   ✓   |    ✗    |    ✗     |   ✗
Integration  |   ✓   |   ✓   |    ✗    |    ✗     |   ✗
```

### Integration Architecture

#### **External System Integration**

```
┌─────────────────────────────────────────────────────────────┐
│                  Integration Patterns                       │
│                                                             │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐      │
│  │   Third     │   │     AWS     │   │   Payment   │      │
│  │   Party     │   │  Services   │   │  Gateways   │      │
│  │    APIs     │   │(REST/SDK)   │   │   (REST)    │      │
│  └─────────────┘   └─────────────┘   └─────────────┘      │
│                                                             │
│  Retry Logic: 3x   Circuit Breaker  Webhook Support       │
│  Error Handling    Monitoring       Event Processing      │
└─────────────────────────────────────────────────────────────┘
```

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

- **External Integrations**: RESTful API communication with third-party services
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

### JSON Naming Convention

**IMPORTANT: Always use snake_case for JSON properties in REST APIs**

The application is configured with:

```yaml
spring:
  jackson:
    property-naming-strategy: SNAKE_CASE
```

This means:

- **All JSON requests/responses use snake_case** (e.g., `country_code`, `recaptcha_token`, `session_id`)
- **Kotlin properties remain camelCase** (e.g., `countryCode`, `recaptchaToken`, `sessionId`)
- **Use `@JsonProperty("snake_case_name")` annotations** on DTOs to map between JSON and Kotlin naming conventions

Example DTO pattern:

```kotlin
data class AuthInitRequest(
  @JsonProperty("country_code")
  var countryCode: Int = 91,

  @JsonProperty("recaptcha_token")
  var recaptchaToken: String? = null,

  var phone: String = ""  // No annotation needed - 'phone' is same in both cases
)
```

Example JSON request:

```json
{
  "phone": "9591781662",
  "country_code": 91,
  "recaptcha_token": "dev-dummy-token-1754245041724"
}
```

This approach follows REST API industry standards and maintains consistency with database underscore naming convention.

### Multi-Device Authentication Usage

**Mobile App Login (Android/iOS):**

```json
POST /auth/v1/init
{
  "phone": "9591781662",
  "country_code": 91,
  "device_id": "MOBILE_ABC123_DEVICE_FINGERPRINT",
  "device_name": "John's iPhone 15",
  "recaptcha_token": "your_recaptcha_token"
}

POST /auth/v1/verify
{
  "session_id": "LSQ20250804100456522TBFOQ8U44LIBLX",
  "otp": "123456",
  "auth_mode": "SMS",
  "device_id": "MOBILE_ABC123_DEVICE_FINGERPRINT",
  "device_name": "John's iPhone 15"
}
```

**Web Browser Login:**

```json
POST /auth/v1/init
{
  "phone": "9591781662",
  "country_code": 91,
  "recaptcha_token": "your_recaptcha_token"
  // device_id will be auto-generated for web clients
}

POST /auth/v1/verify
{
  "session_id": "LSQ20250804100456522TBFOQ8U44LIBLX",
  "otp": "123456",
  "auth_mode": "SMS"
}
```

**Device-Specific Refresh Token:**

```json
POST /auth/v1/refresh_token
{
  "refresh_token": "your_refresh_token",
  "device_id": "MOBILE_ABC123_DEVICE_FINGERPRINT"
}
```

**Device Management:**

```json
GET /auth/v1/devices
Authorization: Bearer your_access_token

Response:
[
{
"device_id": "MOBILE_ABC123_DEVICE_FINGERPRINT",
"device_name": "John's iPhone 15",
"device_type": "Mobile",
"platform": "iOS",
"browser": "Mobile App",
"os": "iOS 17.1",
"ip_address": "192.168.1.100",
"location": null,
"last_activity": "2025-01-04T10:30:00",
"login_time": "2025-01-04T09:00:00",
"is_current_device": true
},
{
"device_id": "WEB_DEF456_BROWSER_HASH",
"device_name": "Chrome on Windows",
"device_type": "Desktop",
"platform": "Windows",
"browser": "Google Chrome",
"os": "Windows 11",
"ip_address": "192.168.1.101",
"location": null,
"last_activity": "2025-01-04T08:45:00",
"login_time": "2025-01-04T08:00:00",
"is_current_device": false
}
]
```

**Logout from Specific Device:**

```json
POST /auth/v1/devices/WEB_DEF456_BROWSER_HASH/logout
Authorization: Bearer your_access_token
```

**Logout from All Devices:**

```json
POST /auth/v1/logout/all
Authorization: Bearer your_access_token
```

### Testing

- Use JUnit 5 platform
- Follow existing test structure in each module
- Mock external dependencies appropriately

## Module Documentation

Each module now includes comprehensive README.md files with detailed architecture and functionality documentation:

### Core Modules

- **[core/README.md](core/README.md)** - Foundation module with shared utilities, AWS integration, multi-tenancy
  support, and global exception handling
- **[auth/README.md](auth/README.md)** - Authentication and JWT token management with OTP-based verification, user
  management, and session handling
- **[workspace/README.md](workspace/README.md)** - Company/workspace management with role-based access control,
  user-company associations, and geographic support

### Business Logic Modules

- **[customer/README.md](customer/README.md)** - Customer relationship management with comprehensive address handling,
  GST compliance, and pagination support
- **[product/README.md](product/README.md)** - Product catalog with inventory management, tax integration, hierarchical
  categorization, and AWS S3 image storage
- **[order/README.md](order/README.md)** - Order processing with complex pricing calculations, tax handling, status
  workflow, and customer-to-customer order support
- **[invoice/README.md](invoice/README.md)** - Invoice generation with GST compliance, PDF creation, email delivery, and
  order-to-invoice conversion

### Application & Integration Modules

- **[ampairs_service/README.md](ampairs_service/README.md)** - Main Spring Boot application with security configuration,
  module aggregation, and production deployment setup

### README Content Structure

Each module README includes:

- **Overview** - Module purpose and key functionality
- **Architecture** - Package structure and component organization
- **Key Features** - Major capabilities and business logic
- **Data Models** - Entity structures and relationships with examples
- **API Endpoints** - REST API documentation with request/response samples
- **Configuration** - Required properties and environment setup
- **Dependencies** - Core libraries and integration requirements
- **Validation Rules** - Data validation and business rules
- **Error Handling** - Exception patterns and error response formats
- **Testing** - Unit and integration testing approaches
- **Build & Deployment** - Commands and deployment procedures
- **Usage Examples** - Code samples and integration patterns
- **Integration** - Inter-module dependencies and communication

### Navigation

To understand a specific module's functionality:

1. Start with the module's README.md file for comprehensive documentation
2. Review the package structure and key components
3. Check API endpoints for available operations
4. Examine data models for entity relationships
5. Review configuration requirements for setup

The README files provide complete documentation for developers to understand, maintain, and extend each module's
functionality within the Ampairs ecosystem.
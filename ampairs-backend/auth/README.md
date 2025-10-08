# Auth Module

## Overview

The Auth module provides comprehensive authentication and JWT token management with OTP-based verification. It handles
user authentication, session management, and secure token operations with multi-tenant support.

## Architecture

### Package Structure

```
com.ampairs.auth/
├── controller/             # REST API endpoints
├── exception/             # Authentication exception handling
├── model/                 # Authentication entities
│   ├── dto/               # Data Transfer Objects
│   └── enums/             # Authentication enumerations
├── repository/            # Data access layer
└── service/               # Authentication business logic

com.ampairs.user/
├── config/                # User configuration
├── controller/            # User management endpoints
├── model/                 # User entities and DTOs
├── repository/            # User data access
└── service/               # User business logic
```

## Key Components

### Controllers

- **`AuthController.kt`** - Main authentication endpoints including init, verify, login, logout, and token refresh
- **`UserController.kt`** - User management endpoints for profile operations

### Authentication Services

- **`AuthService.kt`** - Core authentication logic including OTP generation, user verification, and token management
- **`JwtService.kt`** - JWT token generation, validation, and claims management
- **`OtpService.kt`** - OTP verification and validation logic
- **`LogoutService.kt`** - Token revocation and session cleanup

### User Services

- **`UserService.kt`** - User profile management and operations

### Models

#### Authentication Entities

- **`LoginSession.kt`** - OTP session management with expiration tracking
- **`Token.kt`** - JWT token storage with revocation support and tenant isolation

#### User Entities

- **`User.kt`** - User profile entity with contact information and tenant association

### DTOs

#### Authentication DTOs

- **`AuthInitRequest.kt`** - Authentication initialization request
- **`AuthInitResponse.kt`** - OTP delivery response
- **`AuthenticationRequest.kt`** - Login request with OTP
- **`AuthenticationResponse.kt`** - Login response with tokens
- **`RefreshTokenRequest.kt`** - Token refresh request
- **`OtpVerificationRequest.kt`** - OTP verification request
- **`SessionResponse.kt`** - Session information response

#### User DTOs

- **`UserResponse.kt`** - User profile response
- **`UserUpdateRequest.kt`** - User profile update request

### Enumerations

- **`App.kt`** - Application types (WEB, MOBILE)
- **`ClientType.kt`** - Client platform identification
- **`TokenType.kt`** - Token types (ACCESS, REFRESH)
- **`AuthMode.kt`** - Authentication modes

## Key Features

### OTP-Based Authentication

- SMS-based OTP delivery via AWS SNS
- Configurable OTP expiration (default: 5 minutes)
- Rate limiting on authentication endpoints (1 request per 20 seconds)
- OTP validation with automatic cleanup

### JWT Token Management

- Access and refresh token generation
- Token revocation and blacklisting
- Tenant-aware token claims
- Automatic token expiration handling

### Session Management

- Secure session creation and validation
- Multi-device session support
- Session cleanup on logout
- Tenant-isolated sessions

### Security Features

- Rate limiting protection
- Token-based authentication
- Secure token storage
- Multi-tenant isolation
- Global exception handling

## API Endpoints

### Response Format

All API endpoints follow a consistent response structure using `ApiResponse<T>`:

#### Success Response Structure

```json
{
  "success": true,
  "data": {
    // Actual response data based on endpoint
  },
  "timestamp": "2025-08-07T23:09:13.145+00:00"
}
```

#### Error Response Structure

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Error description",
    "details": {
      // Additional error context
    },
    "timestamp": "2025-08-07T23:09:13.145+00:00"
  }
}
```

### Authentication Endpoints

#### Initialize Authentication Session

```http
POST /auth/v1/init
Content-Type: application/json
{
  "phone": "9591781662",
  "country_code": 91,
  "recaptcha_token": "your_recaptcha_token",
  "device_id": "MOBILE_ABC123_DEVICE_FINGERPRINT",
  "device_name": "John's iPhone 15"
}
```

**Success Response:**

```json
{
  "success": true,
  "data": {
    "session_id": "LSQ20250804100456522TBFOQ8U44LIBLX",
    "otp_sent": true,
    "expires_at": "2025-08-07T23:14:13.145+00:00"
  },
  "timestamp": "2025-08-07T23:09:13.145+00:00"
}
```

#### Verify OTP and Authenticate

```http
POST /auth/v1/verify
Content-Type: application/json
{
  "session_id": "LSQ20250804100456522TBFOQ8U44LIBLX",
  "otp": "123456",
  "auth_mode": "SMS",
  "recaptcha_token": "your_recaptcha_token",
  "device_id": "MOBILE_ABC123_DEVICE_FINGERPRINT",
  "device_name": "John's iPhone 15"
}
```

**Success Response:**

```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiJ9...",
    "refresh_token": "eyJhbGciOiJIUzI1NiJ9...",
    "access_token_expires_at": "2025-08-08T23:09:13.145+00:00",
    "refresh_token_expires_at": "2025-08-14T23:09:13.145+00:00",
    "user": {
      "id": "user-uuid-123",
      "phone": "9591781662",
      "country_code": 91,
      "first_name": "John",
      "last_name": "Doe",
      "email": "john.doe@example.com"
    }
  },
  "timestamp": "2025-08-07T23:09:13.145+00:00"
}
```

#### Refresh Access Token

```http
POST /auth/v1/refresh_token
Content-Type: application/json
{
  "refresh_token": "eyJhbGciOiJIUzI1NiJ9...",
  "device_id": "MOBILE_ABC123_DEVICE_FINGERPRINT"
}
```

**Success Response:**

```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiJ9...",
    "refresh_token": "eyJhbGciOiJIUzI1NiJ9...",
    "access_token_expires_at": "2025-08-08T23:09:13.145+00:00"
  },
  "timestamp": "2025-08-07T23:09:13.145+00:00"
}
```

#### Logout from Current Device

```http
POST /auth/v1/logout
Authorization: Bearer <access-token>
```

**Success Response:**

```json
{
  "success": true,
  "data": {
    "message": "Device logged out successfully"
  },
  "timestamp": "2025-08-07T23:09:13.145+00:00"
}
```

#### Logout from All Devices

```http
POST /auth/v1/logout/all
Authorization: Bearer <access-token>
```

**Success Response:**

```json
{
  "success": true,
  "data": {
    "message": "Logged out from all devices successfully"
  },
  "timestamp": "2025-08-07T23:09:13.145+00:00"
}
```

#### Get User Devices

```http
GET /auth/v1/devices
Authorization: Bearer <access-token>
```

**Success Response:**

```json
{
  "success": true,
  "data": [
    {
      "device_id": "MOBILE_ABC123_DEVICE_FINGERPRINT",
      "device_name": "John's iPhone 15",
      "device_type": "Mobile",
      "platform": "iOS",
      "browser": "Mobile App",
      "os": "iOS 17.1",
      "ip_address": "192.168.1.100",
      "location": null,
      "last_activity": "2025-08-07T22:30:00.000+00:00",
      "login_time": "2025-08-07T21:00:00.000+00:00",
      "is_current_device": true
    }
  ],
  "timestamp": "2025-08-07T23:09:13.145+00:00"
}
```

#### Logout from Specific Device

```http
POST /auth/v1/devices/{deviceId}/logout
Authorization: Bearer <access-token>
```

**Success Response:**

```json
{
  "success": true,
  "data": {
    "message": "Device logged out successfully"
  },
  "timestamp": "2025-08-07T23:09:13.145+00:00"
}
```

### User Management Endpoints

#### Get Current User Profile

```http
GET /user/v1
Authorization: Bearer <access-token>
```

**Success Response:**

```json
{
  "success": true,
  "data": {
    "id": "user-uuid-123",
    "phone": "9591781662",
    "country_code": 91,
    "first_name": "John",
    "last_name": "Doe",
    "email": "john.doe@example.com",
    "created_at": "2025-08-01T10:00:00.000+00:00",
    "updated_at": "2025-08-07T23:09:13.145+00:00"
  },
  "timestamp": "2025-08-07T23:09:13.145+00:00"
}
```

#### Update User Profile

```http
POST /user/v1/update
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "first_name": "John",
  "last_name": "Doe",
  "email": "john.doe@example.com"
}
```

**Success Response:**

```json
{
  "success": true,
  "data": {
    "id": "user-uuid-123",
    "phone": "9591781662",
    "country_code": 91,
    "first_name": "John",
    "last_name": "Doe",
    "email": "john.doe@example.com",
    "created_at": "2025-08-01T10:00:00.000+00:00",
    "updated_at": "2025-08-07T23:09:13.145+00:00"
  },
  "timestamp": "2025-08-07T23:09:13.145+00:00"
}
```

## Configuration

### Required Properties

```yaml
ampairs:
  security:
    jwt:
      secret: ${JWT_SECRET}
      access-token-expiration: 86400000  # 24 hours
      refresh-token-expiration: 604800000 # 7 days
    otp:
      expiration: 300000  # 5 minutes
      length: 6
  aws:
    sns:
      region: ${AWS_REGION:us-east-1}
  rate-limit:
    auth-init:
      capacity: 1
      refill-period: 20s
```

### AWS SNS Configuration

The module requires AWS SNS configuration for OTP delivery:

```yaml
aws:
  sns:
    region: us-east-1
    access-key: ${AWS_ACCESS_KEY_ID}
    secret-key: ${AWS_SECRET_ACCESS_KEY}
```

## Dependencies

### Core Dependencies

- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Security
- Spring Boot Starter Validation

### Authentication Libraries

- JJWT (JWT processing)
- AWS SDK for SNS (OTP delivery)
- Bucket4j (Rate limiting)

### Database

- MySQL JDBC Driver
- Hibernate Core

## Security Implementation

### JWT Token Structure

```json
{
  "sub": "user-id",
  "tenant": "tenant-id",
  "iat": 1640995200,
  "exp": 1641081600,
  "type": "ACCESS_TOKEN"
}
```

### Rate Limiting

- **Auth Init Endpoint**: 1 request per 20 seconds per IP
- **Global Rate Limit**: 20 requests per minute per IP
- **Token-based endpoints**: Standard rate limiting

### Multi-tenancy

- Tenant information embedded in JWT claims
- Tenant-isolated data access
- Request-level tenant context management

## Testing

### Unit Tests

- Authentication flow testing
- JWT token validation testing
- OTP generation and verification testing
- Rate limiting testing

### Integration Tests

- End-to-end authentication flow
- Database integration testing
- AWS SNS integration testing

## Error Handling

### Authentication Errors

All error responses follow the consistent `ApiResponse<T>` format:

#### Common Error Codes and Responses

| Error Code              | HTTP Status | Description               | Example Scenario                               |
|-------------------------|-------------|---------------------------|------------------------------------------------|
| `VALIDATION_ERROR`      | 400         | Request validation failed | Missing required fields, invalid format        |
| `INVALID_OTP`           | 401         | OTP is invalid or expired | Wrong OTP code provided                        |
| `SESSION_EXPIRED`       | 401         | Login session has expired | OTP session timeout (5 minutes)                |
| `INVALID_TOKEN`         | 401         | JWT token is invalid      | Malformed or expired access token              |
| `INVALID_REFRESH_TOKEN` | 401         | Refresh token is invalid  | Expired or revoked refresh token               |
| `RATE_LIMIT_EXCEEDED`   | 429         | Too many requests         | Exceeded rate limits (1 req/20s for auth init) |
| `USER_NOT_FOUND`        | 404         | User not found            | Phone number not registered                    |
| `DEVICE_NOT_FOUND`      | 404         | Device not found          | Device logout with invalid device_id           |
| `SESSION_NOT_FOUND`     | 404         | Login session not found   | Invalid session_id provided                    |
| `UNAUTHORIZED`          | 401         | Authentication required   | Missing Authorization header                   |
| `FORBIDDEN`             | 403         | Access denied             | Insufficient permissions                       |
| `INTERNAL_SERVER_ERROR` | 500         | Server error              | Unexpected system failure                      |

#### Validation Error Example

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed for request parameters",
    "details": {
      "field": "phone",
      "issue": "Phone number is required and must be 10 digits"
    },
    "timestamp": "2025-08-07T23:09:13.145+00:00"
  }
}
```

#### Invalid OTP Error Example

```json
{
  "success": false,
  "error": {
    "code": "INVALID_OTP",
    "message": "The provided OTP is invalid or expired",
    "details": {
      "session_id": "LSQ20250804100456522TBFOQ8U44LIBLX",
      "attempts_remaining": 2
    },
    "timestamp": "2025-08-07T23:09:13.145+00:00"
  }
}
```

#### Rate Limit Exceeded Error Example

```json
{
  "success": false,
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Too many requests. Please try again later.",
    "details": {
      "retry_after_seconds": 20,
      "limit_type": "auth_init"
    },
    "timestamp": "2025-08-07T23:09:13.145+00:00"
  }
}
```

#### Invalid Token Error Example

```json
{
  "success": false,
  "error": {
    "code": "INVALID_TOKEN",
    "message": "JWT token is invalid or expired",
    "details": {
      "token_type": "access_token",
      "reason": "Token signature verification failed"
    },
    "timestamp": "2025-08-07T23:09:13.145+00:00"
  }
}
```

#### Session Expired Error Example

```json
{
  "success": false,
  "error": {
    "code": "SESSION_EXPIRED",
    "message": "Login session has expired. Please initiate authentication again.",
    "details": {
      "session_id": "LSQ20250804100456522TBFOQ8U44LIBLX",
      "expired_at": "2025-08-07T23:04:13.145+00:00"
    },
    "timestamp": "2025-08-07T23:09:13.145+00:00"
  }
}
```

#### User Not Found Error Example

```json
{
  "success": false,
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "No user found with the provided phone number",
    "details": {
      "phone": "9591781662",
      "country_code": 91
    },
    "timestamp": "2025-08-07T23:09:13.145+00:00"
  }
}
```

## Build & Deployment

### Build Commands

```bash
# Build the module
./gradlew :auth:build

# Run tests
./gradlew :auth:test

# Run with profile
./gradlew :auth:bootRun --args='--spring.profiles.active=dev'
```

### Environment Variables

```bash
export JWT_SECRET="your-jwt-secret-key"
export AWS_ACCESS_KEY_ID="your-aws-access-key"
export AWS_SECRET_ACCESS_KEY="your-aws-secret-key"
export DATABASE_URL="jdbc:mysql://localhost:3306/ampairs"
export DATABASE_USERNAME="username"
export DATABASE_PASSWORD="password"
```

## Usage Examples

### Service Integration

```kotlin
@Service
class MyService(
    private val authService: AuthService,
    private val jwtService: JwtService
) {
    
    fun authenticateUser(request: AuthenticationRequest): AuthenticationResponse {
        return authService.authenticateUser(request)
    }
    
    fun validateToken(token: String): Boolean {
        return jwtService.isTokenValid(token)
    }
}
```

### Controller Usage

```kotlin
@RestController
@RequestMapping("/api/v1/protected")
class ProtectedController {
    
    @GetMapping("/data")
    @PreAuthorize("hasRole('USER')")
    fun getProtectedData(authentication: Authentication): ResponseEntity<*> {
        val userId = authentication.name
        // Access protected resources
    }
}
```

## Integration

This module integrates with:

- **Core Module**: Multi-tenancy support, exception handling
- **Workspace Module**: User-company associations and roles
- **AWS SNS**: OTP delivery service
- **Database**: User and session persistence
- **Spring Security**: Authorization and authentication flow

The Auth module provides the security foundation for all other modules in the Ampairs application, ensuring secure
access and proper user identity management.
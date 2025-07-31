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

### Authentication Endpoints

```http
POST /auth/v1/init
Content-Type: application/json
{
  "phoneNumber": "+1234567890",
  "app": "WEB",
  "clientType": "BROWSER"
}
```

```http
POST /auth/v1/verify
Content-Type: application/json
{
  "sessionId": "session-uuid",
  "otp": "123456"
}
```

```http
POST /auth/v1/login
Content-Type: application/json
{
  "sessionId": "session-uuid",
  "otp": "123456"
}
```

```http
POST /auth/v1/refresh
Content-Type: application/json
{
  "refreshToken": "refresh-token-jwt"
}
```

```http
POST /auth/v1/logout
Authorization: Bearer <access-token>
```

### User Management Endpoints

```http
GET /user/v1/profile
Authorization: Bearer <access-token>
```

```http
PUT /user/v1/profile
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com"
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

- Invalid OTP
- Expired sessions
- Invalid tokens
- Rate limit exceeded
- User not found

### Response Format

```json
{
  "success": false,
  "error": {
    "code": "INVALID_OTP",
    "message": "The provided OTP is invalid or expired",
    "timestamp": "2023-01-01T12:00:00Z"
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
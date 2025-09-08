# Ampairs Service - Main Application

## Overview

The Ampairs Service is the main Spring Boot application that aggregates all modules into a cohesive business management
platform. It serves as the central orchestration layer, providing comprehensive security configuration, cross-cutting
concerns, and production-ready features for the entire Ampairs ecosystem.

## Architecture

### Package Structure

```
com.ampairs/
├── AmpairsApplication.kt      # Main application entry point
└── config/                    # Application configuration
    ├── CorsConfig.kt          # Cross-Origin Resource Sharing configuration
    ├── SecurityConfiguration.kt # Spring Security configuration
    ├── CustomJwtAuthenticationConverter.kt # JWT authentication processing
    └── SecurityService.kt     # Security utility services
```

## Key Components

### Main Application

- **`AmpairsApplication.kt`** - Spring Boot application entry point with module aggregation and startup configuration

### Security Configuration

- **`SecurityConfiguration.kt`** - Comprehensive Spring Security setup with JWT authentication, CORS, and endpoint
  protection
- **`CustomJwtAuthenticationConverter.kt`** - Custom JWT token processing and claims extraction
- **`SecurityService.kt`** - Security utility services and helper methods

### Cross-cutting Configuration

- **`CorsConfig.kt`** - Cross-Origin Resource Sharing configuration for web application integration

## Key Features

### Module Aggregation

- Integrates all business modules (auth, customer, product, order, invoice, workspace)
- Unified dependency injection and bean management
- Centralized configuration management
- Single deployable artifact

### Comprehensive Security

- JWT-based authentication with refresh token support
- Role-based access control (RBAC)
- Multi-tenant security isolation
- Rate limiting and CORS protection
- Secure endpoint configuration

### Production-Ready Features

- Health monitoring and metrics
- Structured logging with rotation
- Caching and performance optimization
- Database connection pooling
- Error handling and monitoring

### API Gateway Functionality

- Single entry point for all business operations
- Unified API versioning and documentation
- Request routing and load balancing
- Authentication and authorization gateway

## Dependencies

### Spring Boot Starters

```kotlin
dependencies {
    // Core Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Database
    runtimeOnly("mysql:mysql-connector-java")
    
    // JWT Processing
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    implementation("io.jsonwebtoken:jjwt-impl:0.11.5")
    implementation("io.jsonwebtoken:jjwt-jackson:0.11.5")
    
    // Rate Limiting
    implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:7.6.0")
    implementation("com.github.vladimir-bukhtoyarov:bucket4j-jcache:7.6.0")
    
    // AWS Services
    implementation("software.amazon.awssdk:s3:2.20.26")
    implementation("software.amazon.awssdk:sns:2.20.26")
    
    // Caching
    implementation("com.github.ben-manes.caffeine:caffeine")
    
    // Monitoring
    implementation("io.micrometer:micrometer-registry-prometheus")
    
    // Business Modules
    implementation(project(":core"))
    implementation(project(":auth"))
    implementation(project(":workspace"))
    implementation(project(":customer"))
    implementation(project(":product"))
    implementation(project(":order"))
    implementation(project(":invoice"))
}
```

## Configuration

### Main Application Properties

```yaml
# Application Configuration
spring:
  application:
    name: ampairs-service
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  
  # Database Configuration
  datasource:
    url: ${DATABASE_URL:jdbc:mysql://localhost:3306/ampairs}
    username: ${DATABASE_USERNAME:ampairs}
    password: ${DATABASE_PASSWORD:password}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
  
  # JPA Configuration
  jpa:
    hibernate:
      ddl-auto: update
      naming:
        physical-strategy: org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
  
  # Cache Configuration
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=15m
  
  # Mail Configuration
  mail:
    host: ${SMTP_HOST:localhost}
    port: ${SMTP_PORT:587}
    username: ${SMTP_USERNAME:}
    password: ${SMTP_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

# Ampairs Configuration
ampairs:
  security:
    jwt:
      secret: ${JWT_SECRET:your-secret-key}
      access-token-expiration: 86400000  # 24 hours
      refresh-token-expiration: 604800000 # 7 days
    cors:
      allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:8080}
      allowed-methods: GET,POST,PUT,DELETE,OPTIONS
      allowed-headers: "*"
      allow-credentials: true
  
  aws:
    region: ${AWS_REGION:us-east-1}
    s3:
      bucket: ${S3_BUCKET_NAME:ampairs-storage}
    sns:
      enabled: ${SNS_ENABLED:true}
  
  rate-limit:
    global:
      capacity: 20
      refill-period: 1m
    auth-init:
      capacity: 1
      refill-period: 20s

# Actuator Configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when_authorized
  metrics:
    export:
      prometheus:
        enabled: true

# Logging Configuration
logging:
  level:
    com.ampairs: INFO
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
  file:
    name: logs/ampairs.log
  logback:
    rollingpolicy:
      max-file-size: 1000MB
      max-history: 7
      total-size-cap: 5GB
```

### Production Configuration

```yaml
# application-prod.yml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  
  cache:
    caffeine:
      spec: maximumSize=50000,expireAfterWrite=30m

logging:
  level:
    com.ampairs: INFO
    org.springframework.security: WARN
    org.hibernate.SQL: WARN
  file:
    name: logs/production.log
```

## Security Configuration

### JWT Authentication Flow

```kotlin
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfiguration(
    private val jwtAuthenticationEntryPoint: AuthEntryPointJwt,
    private val customJwtAuthenticationConverter: CustomJwtAuthenticationConverter
) {
    
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/auth/v1/**").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(customJwtAuthenticationConverter)
                }
            }
            .exceptionHandling { it.authenticationEntryPoint(jwtAuthenticationEntryPoint) }
            .build()
    }
}
```

### CORS Configuration

```kotlin
@Configuration
class CorsConfig {
    
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOriginPatterns = listOf("*")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
```

## API Endpoints Overview

### Module-based API Structure

```
/api/v1/
├── auth/                    # Authentication & user management
│   ├── init                 # OTP initialization
│   ├── verify               # OTP verification
│   ├── login                # User login
│   ├── refresh              # Token refresh
│   └── logout               # User logout
├── workspace/               # Workspace & company management
│   ├── workspaces           # CRUD operations
│   └── users                # User-workspace associations
├── customer/                # Customer management
│   ├── customers            # Customer CRUD
│   ├── search               # Customer search
│   └── states               # Geographic data
├── product/                 # Product catalog
│   ├── products             # Product CRUD
│   ├── categories           # Product categorization
│   ├── inventory            # Inventory management
│   └── tax                  # Tax management
├── order/                   # Order processing
│   ├── orders               # Order CRUD
│   ├── items                # Order items
│   └── status               # Status management
└── invoice/                 # Invoice management
    ├── invoices             # Invoice CRUD
    ├── pdf                  # PDF generation
    ├── payments             # Payment recording
    └── send                 # Email delivery
```

## Deployment

### Docker Configuration

```dockerfile
FROM openjdk:21-jre-slim

WORKDIR /app
COPY ampairs_service.jar app.jar
COPY application-prod.yml application-prod.yml

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
```

### Build Script (build_run.sh)

```bash
#!/bin/bash

# Build the application
./gradlew clean build -x test

# Create deployment package
mkdir -p latest
cp build/libs/ampairs_service-*.jar latest/ampairs_service.jar
cp src/main/resources/application-prod.yml latest/

# Create tar package
tar -czf build.tar.gz latest/

# Deploy to production server
scp -i ampairs.pem build.tar.gz user@production-server:/opt/ampairs/
ssh -i ampairs.pem user@production-server "cd /opt/ampairs && tar -xzf build.tar.gz && sudo systemctl restart ampairs"

echo "Deployment completed successfully!"
```

### System Service Configuration

```ini
# /etc/systemd/system/ampairs.service
[Unit]
Description=Ampairs Business Management Service
After=network.target

[Service]
Type=simple
User=ampairs
WorkingDirectory=/opt/ampairs/latest
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod ampairs_service.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

## Monitoring & Observability

### Health Checks

```http
GET /actuator/health
```

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 499963174912,
        "free": 91943518208,
        "threshold": 10485760
      }
    }
  }
}
```

### Metrics

```http
GET /actuator/metrics
GET /actuator/prometheus
```

### Logging

- Structured JSON logging for production
- Log rotation (7 days retention, 1000MB max file size)
- Separate log files for different components
- Configurable log levels per package

## Performance Optimization

### Database Connection Pooling

- HikariCP with optimized settings
- 20 max connections for development
- 30 max connections for production
- Connection timeout and idle timeout configuration

### Caching Strategy

- Caffeine cache for frequently accessed data
- 15-minute expiration for development
- 30-minute expiration for production
- Maximum 50,000 entries per cache

### Rate Limiting

- Global rate limiting (20 requests per minute)
- Authentication endpoint protection (1 request per 20 seconds)
- Configurable limits per endpoint

## Testing

### Integration Testing

```bash
# Run all tests
./gradlew test

# Run integration tests
./gradlew integrationTest

# Run with specific profile
./gradlew test -Dspring.profiles.active=test
```

### Load Testing

```bash
# Using Apache Bench
ab -n 1000 -c 10 -H "Authorization: Bearer <token>" http://localhost:8080/api/v1/products

# Using JMeter
jmeter -n -t load-test-plan.jmx -l results.jtl
```

## Build & Deployment

### Development

```bash
# Build and run locally
./gradlew bootRun

# Build with specific profile
./gradlew bootRun --args='--spring.profiles.active=dev'

# Build JAR
./gradlew bootJar
```

### Production Deployment

```bash
# Build production package
./gradlew clean build -Pprod

# Deploy using build script
./build_run.sh

# Manual deployment
java -jar -Dspring.profiles.active=prod ampairs_service.jar
```

## Integration

The Ampairs Service integrates and orchestrates all business modules:

- **Core Module**: Foundation services, multi-tenancy, AWS integration
- **Auth Module**: Authentication and user management
- **Workspace Module**: Company and role management
- **Customer Module**: Customer relationship management
- **Product Module**: Product catalog and inventory
- **Order Module**: Order processing and management
- **Invoice Module**: Billing and invoice generation

It provides a unified platform for comprehensive business management with enterprise-grade security, monitoring, and
deployment capabilities.
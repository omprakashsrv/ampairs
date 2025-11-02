# Ampairs Service (Aggregator)

## Overview
`ampairs_service` assembles every backend module into a single Spring Boot application. It wires cross-cutting security (JWT resource server, CORS), shared beans, and infrastructure such as caching, AWS integrations, and actuator endpoints. This is the module you run locally (`bootRun`) or package for deployment (`bootJar`).

## Architecture
### Package Structure
```
com.ampairs/
├── AmpairsApplication.kt      # Spring Boot entry point
└── config/
    ├── CorsConfig.kt          # Centralised CORS policy
    ├── SecurityConfiguration.kt # Resource-server security rules & filters
    ├── CustomJwtAuthenticationConverter.kt # Extracts claims from signed JWTs
    └── SecurityService.kt     # Helper utilities for security context introspection
```

## Key Responsibilities
- Import all domain modules (`core`, `auth`, `workspace`, `customer`, `product`, `order`, `invoice`, `tax`, `business`, `form`, `event`, `notification`).
- Configure Spring Security as an OAuth2 resource server validating RS256 tokens issued by the auth module.
- Register global CORS allowances so web and mobile clients can call APIs during development.
- Enable shared infrastructure: caching (Caffeine/JCache), AWS S3/SNS clients (via Spring Cloud AWS), scheduled tasks, and actuator metrics.
- Surface Prometheus metrics and health information when `spring-boot-starter-actuator` is enabled.

## Runtime Features
- JWT validation using `CustomJwtAuthenticationConverter` to map JWT claims into `Authentication` objects consumed by downstream modules.
- Global method security enabled, allowing module controllers to use `@PreAuthorize`.
- Application-wide rate limiting hooks rely on the advanced rate-limit components defined in `core`.
- Conditional configuration toggles via `ApplicationProperties` (e.g., AWS credentials, storage paths).
- Actuator endpoints for `/actuator/health`, `/actuator/prometheus`, etc., to integrate with observability stacks.

## Build & Run
```bash
# Run the full service with all modules
./gradlew :ampairs_service:bootRun

# Package an executable JAR (includes all modules)
./gradlew :ampairs_service:bootJar

# Execute tests scoped to the aggregator
./gradlew :ampairs_service:test
```

The aggregator honours the shared environment variables outlined in the root documentation (database connection details, JWT keys, AWS credentials, and feature toggles). When running locally, ensure supporting services (databases, Docker containers) are available as required by the individual modules.

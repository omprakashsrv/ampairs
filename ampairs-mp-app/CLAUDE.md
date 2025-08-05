# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin Multiplatform business application (ERP/retail focused) using Compose Multiplatform. It targets Android and Desktop (JVM), with iOS support commented out. The app uses a multi-module architecture with feature-based organization.

## Build Commands

### Android
```bash
./gradlew composeApp:assembleDebug
./gradlew composeApp:installDebug
```

### Desktop
```bash
./gradlew composeApp:run
./gradlew composeApp:package  # Creates native distributions (DMG, MSI, DEB)
```

### Cleanup
```bash
./gradlew clean
./cleanup.sh  # Removes IDE files, build artifacts, and iOS workspace
```

### Linting
- Android Lint is configured with baseline files (`lint-baseline.xml`)
- No specific lint command found - check via build process

## Architecture

### Technology Stack
- **UI**: Jetpack Compose Multiplatform with Material 3
- **DI**: Koin with modular setup per feature
- **Database**: SQLDelight with separate databases per feature domain
- **HTTP**: Ktor client with bearer token authentication
- **Navigation**: Androidx Navigation Compose
- **State**: ViewModels with Resource<T> wrapper for async operations
- **Image Loading**: Custom image loader with caching
- **Serialization**: kotlinx.serialization

### Module Structure
- **composeApp**: Main application module with business logic
- **core/**: Cross-cutting concerns (analytics, logging, permissions, notifications)
- **common/**: Shared UI components and utilities
- **Feature modules**: Each business domain (auth, product, order, etc.) has its own module

### Data Flow Pattern
```
UI Screen → ViewModel → Repository → (API + SQLDelight DB) → Resource<T>
```

### Feature Module Organization
Each feature follows this structure:
```
feature/
├── Navigation.kt - Navigation graph
├── FeatureModule.kt - Koin DI setup
├── api/ - HTTP API layer
├── db/ - SQLDelight database
├── domain/ - Business models
├── ui/ - Compose screens
└── viewmodel/ - ViewModels with factories
```

## Key Development Patterns

### Database Setup
- **8 separate SQLDelight databases**: Auth, Company, Customer, Product, Order, Invoice, Inventory, Tally
- Cross-platform drivers: Android SQLite vs JVM JDBC
- Repository pattern abstracts data access
- Schema files use `.sq` extension

### Authentication
- Phone + OTP authentication flow
- Bearer token with automatic refresh via Ktor interceptors
- Company-based multi-tenancy via HTTP headers
- Tokens stored in SQLDelight `userTokenEntity` table

### API Integration
- Base URL: `http://localhost:8080` (development)
- All endpoints require company context via headers
- JSON serialization with kotlinx.serialization
- Automatic token refresh handling

### Dependency Injection (Koin)
- Modular setup with `FeatureModule.kt` per domain
- Platform-specific modules for Android vs Desktop
- **ViewModels use direct `koinInject()` pattern** - All screens inject ViewModels using `koinInject()` and `parametersOf()` for clean dependency injection
- Scoped dependencies per feature

### Platform Differences
- **Android**: Activities, SQLite, OkHttp client, splash screen
- **Desktop**: JVM target, JDBC, separate DB directory per OS
- **iOS**: Currently commented out but framework structure exists

## Version Catalog (gradle/libs.versions.toml)
- **Kotlin**: 2.2.0
- **Compose**: 1.8.2
- **SQLDelight**: 2.1.0
- **Ktor**: 3.2.1
- **Koin**: 4.1.0

**Known Issue**: KSP version (2.1.0-1.0.29) needs upgrade to match Kotlin 2.2.0

## Development Environment
- **IDE**: Android Studio with Kotlin Multiplatform plugin
- **Environment Check**: Use `kdoctor` to validate setup
- **Min SDK**: 24, Target SDK: 35, Compile SDK: 36

## Business Domain
The application handles:
- Authentication (phone/OTP)
- Company management (multi-tenant)
- Product catalog with tax codes
- Inventory management
- Order processing
- Invoice generation
- Customer management
- Tally integration

## Recent Changes & Implementation Notes

### ViewModel Implementation (2025-01-24)
**Successfully migrated from complex ViewModelFactory patterns to clean Koin injection:**

#### ✅ **What was accomplished:**
1. **All screens now use `koinInject()` pattern**
   - Converted all UI screens from complex ViewModel factory patterns to clean Koin dependency injection
   - Pattern: `val viewModel: MyViewModel = koinInject { parametersOf(id) }`
   - Eliminated `CreationExtras`, `bundleOf`, and `ViewModelFactory` complexity

2. **Updated all module configurations**
   - All `*Module.kt` files now use proper Koin factory definitions: `factory { MyViewModel(get(), get()) }`
   - Added parameter support: `factory { (id: String?) -> MyViewModel(id, get()) }`

3. **Fixed compilation errors**
   - Updated navigation path references (Route.PRODUCT → Route.Product)
   - Fixed suspend function calls in Compose contexts (added coroutineScope)
   - Fixed content reference issues in pane screens (contentKey instead of content)
   - Resolved nullable parameter handling in navigation

#### **Key screens updated:**
- Product screens (ProductScreen, ProductListScreen, ProductGroupScreen, TaxInfoScreen, TaxCodeScreen)
- Order screens (OrderScreen, OrderViewScreen)
- Invoice screens (InvoiceScreen, InvoiceViewScreen)  
- Inventory screen (InventoryScreen)
- All pane screens (ProductPaneScreen, OrderPaneScreen, InvoicePaneScreen, etc.)

#### **Benefits achieved:**
- **Simplified ViewModel injection**: Clean, consistent `koinInject()` usage across all screens
- **Better maintainability**: Eliminated complex factory patterns and creation extras
- **Parameter support maintained**: ViewModels requiring parameters work seamlessly with `parametersOf()`
- **Successful compilation**: Project compiles cleanly for desktop target

### Navigation Implementation
- **Type-safe navigation** using `@Serializable` route classes instead of Android-specific Bundle arguments
- Navigation routes defined in `Routes.kt` with proper sealed interfaces for each feature
- Cross-platform compatible navigation that works on both Android and Desktop

## Common Issues
- iOS target is disabled - enable in `composeApp/build.gradle.kts` if needed
- KSP version compatibility warning appears in builds
- ProGuard is enabled for Android release builds
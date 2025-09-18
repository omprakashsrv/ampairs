# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Ampairs Mobile Application** is a Kotlin Multiplatform business management client that integrates with the Ampairs Spring Boot backend system. It targets **Android, iOS, and Desktop (JVM)** platforms using **Compose Multiplatform** with an **offline-first architecture**.

### **🏗️ System Integration**

This mobile app is part of a **three-tier Ampairs ecosystem**:

1. **Backend (Spring Boot + Kotlin)** - `/ampairs_service` + domain modules  
2. **Web Frontend (Angular + Material Design 3)** - `/ampairs-web`
3. **Mobile App (Kotlin Multiplatform)** - `/ampairs-mp-app` ← **THIS PROJECT**

**Backend Integration**: Consumes REST APIs from Spring Boot backend with JWT authentication, multi-tenant support, and offline-first synchronization.

## Architecture

### **🔄 Offline-First Architecture with Store5**

- **Pattern**: Store5 for robust offline-first data management
- **Layers**: Presentation (Compose/MVI) → Store5 (Fetcher/SourceOfTruth) → Data (Room/Ktor)
- **Integration**: Spring Boot backend with JWT auth and multi-tenancy

### **Technology Stack**

- **UI Framework**: Jetpack Compose Multiplatform with Material 3 Design System
- **Theme Management**: Reactive theme switching with Light/Dark/System modes (default: System)
- **Dependency Injection**: Koin with modular setup per feature
- **Local Database**: Room Database (replaces SQLDelight) with platform-specific drivers
- **Offline-First**: Store5 for caching, synchronization, and conflict resolution
- **HTTP Client**: Ktor with automatic JWT token refresh and bearer authentication
- **Navigation**: Androidx Navigation Compose with type-safe routing
- **State Management**: MVI pattern with ViewModels and Resource<T> wrappers
- **Image Loading**: Custom image loader with caching support
- **Serialization**: kotlinx.serialization for JSON parsing
- **Background Sync**: Platform-specific background task scheduling

### **Platform Support**

- **Android**: Native Android app with Room SQLite, background sync via WorkManager
- **iOS**: Native iOS app with Core Data integration, background refresh capabilities
- **Desktop (JVM)**: Desktop application with JDBC drivers, timer-based sync coordination

### **Module Structure**

- **Main Module**: `composeApp/` with `commonMain/`, `androidMain/`, `iosMain/`, `desktopMain/`
- **Domain Modules**: auth, workspace, customer, product, order, invoice, tally
- **Support**: core/, common/, shared/

## **🔑 Key Development Patterns**

### **Store5 Implementation Pattern**
- **Pattern**: Each domain module implements Store5 with Fetcher (Network) + SourceOfTruth (Room DB)
- **Usage**: `store.stream(StoreReadRequest.cached(key, refresh = false))`

### **Room Database Architecture**
- **Platform-specific**: Room (Android), Core Data bridge (iOS), JDBC (Desktop)
- **Multi-tenant**: All entities include `tenant_id` for data segregation
- **Sync metadata**: Entities track `syncStatus`, timestamps for offline-first

### **Authentication & Multi-tenancy**
- **JWT Auth**: Phone + OTP flow with device_id for multi-device support
- **Tenant Context**: HTTP headers include workspace/company context
- **Token Storage**: Secure storage via Room database with encryption

### **API Integration with Backend**
- **Endpoints**: Follow backend REST patterns (`/api/v1/{resource}`)
- **Headers**: Include `X-Workspace-ID` for multi-tenant context
- **Responses**: Use backend `ApiResponse<T>` wrapper format

### **Dependency Injection with Koin**
- **Modular**: Feature-based modules per domain
- **Platform-specific**: Separate Android/iOS/Desktop implementations
- **ViewModel pattern**: `koinInject { parametersOf(id) }`
- **Layers**: API → Repository → Store5 → ViewModel

### **Navigation with Type Safety**
- **Routes**: `@Serializable sealed interface Route` with data classes
- **Usage**: `navController.navigate(Route.CustomerDetails(customerId))`
- **Type safety**: `backStackEntry.toRoute<Route.CustomerDetails>()`

## **Build Commands**

### **Android**
```bash
./gradlew composeApp:assembleDebug
./gradlew composeApp:installDebug
```

### **Desktop**
```bash
./gradlew composeApp:run                    # Run desktop app
./gradlew composeApp:package               # Create native distributions
```

### **iOS** (Currently commented out)
```bash
./gradlew composeApp:embedAndSignAppleFrameworkForXcode
```

### **Cleanup**
```bash
./gradlew clean
./cleanup.sh  # Remove IDE files and build artifacts
```

## **Version Catalog**
**Key Dependencies** (`gradle/libs.versions.toml`):
- Kotlin 2.2.0, Compose Multiplatform 1.8.2, Room 2.7.0-alpha11
- Store5 5.1.0, Ktor 3.2.1, Koin 4.1.0, Kotlinx Serialization 1.7.3

## **Development Environment**
- **IDE**: Android Studio with KMP plugin, `kdoctor` for validation
- **Requirements**: Android Min SDK 24/Target 35, Java 21+, Xcode 15+ (iOS)

## **Business Domain Integration**

The app provides complete feature parity with the backend system:

### **Core Features**
- **Authentication**: Phone/OTP login with JWT tokens and multi-device support
- **Workspace Management**: Multi-tenant workspace selection and configuration  
- **Customer Management**: Comprehensive CRM with address handling and GST compliance
- **Product Catalog**: Product management with categories, tax codes, and image storage
- **Inventory Management**: Stock tracking, movement reporting, and low-stock alerts
- **Order Processing**: Order creation, management, status workflows, and pricing
- **Invoice Generation**: Invoice creation, GST compliance, PDF generation, email delivery
- **Tally Integration**: ERP system synchronization and data exchange

### **Data Flow & Synchronization**
- **Offline-first**: All CRUD operations work offline with auto-sync
- **Conflict Resolution**: Store5 last-write-wins strategy
- **Background Sync**: Platform-specific tasks for data consistency
- **Real-time**: WebSocket connections for live updates when online

## **Platform-Specific Implementations**
- **Android**: Room SQLite, WorkManager sync, local notifications
- **iOS**: Room Core Data bridge, Background App Refresh, push notifications
- **Desktop**: Room JDBC drivers, timer-based sync, native window controls

## **Recent Updates & Migration Notes**
- **Database**: ✅ Migrated SQLDelight → Room with Store5 integration
- **Architecture**: ✅ Store5 offline-first with conflict resolution
- **ViewModels**: ✅ Koin injection with `koinInject { parametersOf() }`

## **Development Guidelines**
- **Naming**: `com.ampairs.{domain}.{layer}`, `@SerialName` for snake_case API compatibility
- **Backend**: Follow REST patterns (`/api/v1/{resource}`), use `ApiResponse<T>` wrapper
- **Quality**: Offline-first design, graceful error recovery, lazy loading

## **Theme Management System**

The app includes a comprehensive theme switching system implemented in January 2025:

### **Architecture**
- **Options**: `ThemePreference.SYSTEM/LIGHT/DARK` (default: LIGHT)
- **Manager**: `ThemeManager` with `StateFlow<ThemePreference>` and `@Composable isDarkTheme()`

### **Implementation Files**
- **Core**: `ThemePreference.kt`, `ThemeManager.kt`, `ThemeRepository.kt`
- **UI**: `AppHeader.kt` with `ThemeToggleButton`
- **Integration**: `App.kt` uses `ThemeManager.isDarkTheme()`

### **Features**
- **UI**: Prominent placement beside user menu with instant reactive updates
- **Integration**: System theme respect, Material 3 color schemes, cross-platform

### **User Experience**
- **Default**: Light theme (changed from System)
- **Access**: Theme icon in header with System/Light/Dark options
- **Performance**: Instant switching with visual feedback

### **Usage in Code**
- **Injection**: `val themeManager: ThemeManager = koinInject()`
- **Theme**: `PlatformAmpairsTheme(darkTheme = themeManager.isDarkTheme())`
- **Set**: `themeManager.setThemePreference(ThemePreference.DARK)`

## **DataStore Configuration & Key-Value Storage**

### **📦 Existing DataStore Implementation (January 2025)**

**IMPORTANT**: The app has a **fully configured DataStore Preferences system** for cross-platform key-value storage. **Always reuse this existing setup** for any new persistence needs.

#### **Key Files & Structure**
- **Common Factory**: `/composeApp/src/commonMain/kotlin/com/ampairs/common/theme/createThemeDataStore.kt`
- **Platform Implementations**: `createThemeDataStore.android.kt`, `createThemeDataStore.desktop.kt`, `createThemeDataStore.ios.kt`
- **Repository Pattern**: `/composeApp/src/commonMain/kotlin/com/ampairs/common/theme/ThemeRepository.kt`
- **Koin Modules**: `androidAppConfigModule`, `desktopAppConfigModule`, `iosAppConfigModule`

#### **Storage Locations**
- **Android**: `context.filesDir/theme_preferences.preferences_pb`
- **Desktop**: `~/.ampairs/theme_preferences.preferences_pb`
- **iOS**: `Documents/theme_preferences.preferences_pb` (requires `@OptIn(ExperimentalForeignApi::class)`)

#### **Integration Pattern**
- All platform Koin modules include their respective app config modules via `includes()`
- DataStore injected as `DataStore<Preferences>` singleton
- Repository pattern with Flow-based reactive updates
- StateFlow conversion via `stateIn()` for Compose integration

#### **Usage Guidelines**
- ✅ **DO**: Inject existing `DataStore<Preferences>` and add new preference keys
- ❌ **DON'T**: Create separate DataStore instances
- **Pattern**: Repository → Manager → UI (with proper error handling and defaults)

## **iOS Target Development**

### **📱 iOS Implementation Status (January 2025)**

**Status**: ✅ **Fully implemented and production-ready**

#### **Key iOS Configurations**
- **Dispatchers**: iOS uses `Dispatchers.Default` for IO operations (no IO dispatcher)
- **Database**: Room with iOS Documents directory paths via `getIosDatabasePath()`
- **Platform APIs**: UIKit integration with `@OptIn(ExperimentalForeignApi::class)` for Foundation APIs
- **Koin**: Proper initialization in `MainViewController` before app launch
- **Navigation**: Side drawer pattern (no hardware back button)

#### **iOS-Specific Requirements**
- **File Paths**: Always use Documents directory for writable storage
- **Time Handling**: Use `kotlin.time.Clock` for cross-platform compatibility
- **Compilation**: `compileKotlinIosSimulatorArm64` for testing
- **Threading**: iOS-specific `synchronized()` and `Volatile` implementations


## **Common Issues & Solutions**

- **iOS**: Use `getIosDatabasePath()`, initialize Koin in `MainViewController`, `DispatcherProvider.io` instead of `Dispatchers.IO`
- **Room**: Check migration scripts when updating schemas
- **Store5**: Timestamp-based conflict resolution for concurrent modifications
- **Network**: Ktor client with proper timeout and retry policies

## **Integration with Backend**

- **Domain Models**: Identical patterns with Spring Boot backend
- **API Contracts**: Exact REST endpoint compatibility with JWT auth and multi-tenancy
- **Feature Parity**: Consistent across web, mobile, and API clients

*Refer to main `/ampairs/CLAUDE.md` for backend guidelines.*

## **🔗 Dynamic Module Navigation System (January 2025)**

### **📋 Overview**
The app implements a sophisticated dynamic module navigation system that integrates backend-installed modules with local navigation implementations, providing seamless module access with proper fallback handling.

### **🏗️ Architecture Components**

#### **Module Registry System**
- **File**: `com/ampairs/workspace/navigation/ModuleRegistry.kt`
- **Purpose**: Central registry mapping module codes to local navigation routes
- **Interface**: `IModuleNavigationProvider` for extensible module registration
- **Features**: Type-safe navigation, dynamic discovery, fallback handling

#### **Module Navigation Providers**
- **File**: `com/ampairs/workspace/navigation/ModuleProviders.kt`
- **Implementations**:
  - `CustomerModuleProvider`: "customer-management" → `Route.Customer`
  - `ProductModuleProvider`: "product-management" → `Route.Product`
  - `OrderModuleProvider`: "order-management" → `Route.Order`
  - `InvoiceModuleProvider`: "invoice-management" → `Route.Invoice`

#### **Enhanced WorkspaceModulesScreen**
- **File**: `com/ampairs/workspace/ui/WorkspaceModulesScreen.kt`
- **Features**:
  - Direct module navigation via registry lookup
  - "Update App" dialog for missing implementations
  - Backward compatibility with existing callback system

#### **DynamicModuleNavigationService Integration**
- **File**: `com/ampairs/workspace/navigation/DynamicModuleNavigationService.kt`
- **Enhancements**:
  - Filters installed modules by local availability
  - Separate tracking of available vs unavailable modules
  - Integration with module implementation detection

### **🚀 Navigation Flow**
```
User clicks module card
    ↓
tryNavigateToModule() checks registry
    ↓
If available: Navigate to Route.{Module}
    ↓
If unavailable: Show "Update App" dialog
    ↓
Fallback: Use original onModuleSelected callback
```

### **🗄️ Workspace Context & Database Isolation**

#### **Unified Context Management**
- **Issue Fixed**: Database paths using "workspace_default" instead of actual slug
- **Root Cause**: Two separate context systems (business vs database) not synchronized
- **Solution**: Enhanced `WorkspaceContextIntegration.setWorkspaceFromDomain()`

#### **Context Integration**
- **Business Context**: `WorkspaceContextManager` for app state
- **Database Context**: `WorkspaceContext` for database paths
- **Unified Setup**: Both contexts set simultaneously on workspace selection
- **Result**: Proper isolation with `workspace_{actual-slug}/module.db` paths

### **📱 Module Code Mappings**
```kotlin
// Local implementations available
"customer-management" → Route.Customer
"product-management" → Route.Product
"order-management" → Route.Order
"invoice-management" → Route.Invoice

// Shows "Update App" dialog
"inventory-management" → Not locally implemented
```

### **🛠️ Usage Patterns**

#### **Adding New Module Support**
1. Create navigation provider in `ModuleProviders.kt`
2. Register in `ModuleRegistry.initialize()`
3. Update availability check in `DynamicModuleNavigationService`
4. Module automatically appears in navigation

#### **Integration Guidelines**
- **Module Discovery**: Automatic backend-driven module availability
- **Local Implementation**: Registry-based route resolution
- **Fallback Strategy**: Graceful degradation for missing modules
- **Type Safety**: Compile-time route validation

### **✅ Production Status**
- **Module Integration**: ✅ Complete and tested
- **Workspace Context**: ✅ Unified and isolated
- **Navigation Flow**: ✅ Type-safe with fallbacks
- **Database Isolation**: ✅ Proper workspace segregation
- **Backward Compatibility**: ✅ No breaking changes

This system provides a robust foundation for dynamic module loading while maintaining type safety and graceful degradation for missing implementations.
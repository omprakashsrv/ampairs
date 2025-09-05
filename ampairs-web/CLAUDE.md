 Angular Application Architecture & Patterns

  Application Structure

  The Angular application follows a modern standalone component architecture with clear separation of concerns:

  src/app/
  ├── core/                    # Services, guards, interceptors, models
  │   ├── guards/             # AuthGuard, WorkspaceGuard, WorkspaceMemberGuard
  │   ├── interceptors/       # ApiResponse, Auth, Workspace, Loading
  │   ├── models/            # TypeScript interfaces (snake_case)
  │   └── services/          # Core business services
  ├── shared/                 # Reusable components and design system
  │   ├── components/        # Shared UI components
  │   └── styles/           # M3 theming system
  ├── auth/                  # Authentication flow (Login → OTP → Profile)
  ├── pages/                # Feature pages (Dashboard, Workspace management)
  ├── home/                 # Main application shell with navigation
  └── app.component.ts      # Root component (minimal shell)

  Key Patterns:
  - No NgModules: Pure standalone components with lazy loading
  - Feature-based organization: Clear boundaries between business domains
  - Multi-tenant routing: /w/:slug for workspace-scoped navigation
  - Service-based state management: BehaviorSubject reactive patterns

  Material Design 3 System

  CRITICAL: Enforce Material Design 3 exclusively

  - Components: ONLY @angular/material components allowed
  - Theming: Complete M3 color system with dynamic theme switching
  - CSS Custom Properties: All styling uses M3 design tokens
  - Typography: Full M3 typography scale (display → label)
  - Responsive: Mobile-first with proper breakpoint handling

  Theme Architecture:
  shared/styles/
  ├── _theme-m3.scss           # M3 theme definitions
  ├── _theme-m3-palettes.scss  # Color palettes
  ├── _variables.scss          # Design tokens
  └── _mixins.scss            # Utility mixins

  Material 3 Design Tokens (from src/theme/variables.scss):
  ```scss
  // Material 3 Color Tokens
  $color-primary: var(--primary-color)
  $color-primary-container: var(--primary-container-color)
  $color-surface: var(--surface-color)
  $color-surface-container: var(--surface-container-color)
  $color-on-surface: var(--on-surface-color)
  $color-outline-variant: var(--outline-variant-color)

  // Material 3 Typography Tokens
  $font-body-large: var(--mat-sys-body-large)
  $font-body-medium: var(--mat-sys-body-medium)
  $font-headline-large: var(--mat-sys-headline-large)
  $font-headline-medium: var(--mat-sys-headline-medium)
  $font-label-large: var(--mat-sys-label-large)

  // Material 3 Spacing Tokens
  $spacing-xs: var(--mat-sys-spacing-small, 0.125rem)    // ~2px
  $spacing-sm: var(--mat-sys-spacing-medium, 0.25rem)   // ~4px
  $spacing-md: var(--mat-sys-spacing-large, 0.5rem)     // ~8px
  $spacing-lg: var(--mat-sys-spacing-x-large, 0.75rem)  // ~12px
  $spacing-xl: var(--mat-sys-spacing-xx-large, 1rem)    // ~16px
  ```

  ThemeService Features:
  - Runtime theme switching (light/dark/auto)
  - Density control (-5 to 0)
  - Persistent user preferences
  - Export/import theme configurations

  Authentication & Security

  Multi-step Authentication Flow:
  1. Phone Login → 2. OTP Verification → 3. Profile Completion

  Security Patterns:
  - JWT Token Management: Automatic refresh with device tracking
  - Multi-device Sessions: Device-specific session isolation
  - reCAPTCHA Integration: Conditional security (dev bypass available)
  - Workspace Context: Tenant-aware API calls via interceptors

  Key Services:
  - AuthService: Complete auth lifecycle with session management
  - AuthInterceptor: Token injection and refresh handling
  - WorkspaceInterceptor: Tenant context headers

  HTTP Interceptor Chain

  Processing Order:
  1. ApiResponseInterceptor: Unwraps ApiResponse<T> structure
  2. AuthInterceptor: JWT token management and refresh
  3. WorkspaceInterceptor: Workspace context headers
  4. LoadingInterceptor: Global loading state management

  Component Patterns

  Form Handling:
  - Reactive Forms Only: FormBuilder and FormGroup patterns
  - Smart Validation: Custom validators with user-friendly messages
  - Auto-submit Logic: (e.g., OTP auto-submit on completion)

  State Management:
  - Service-based: No external state library needed
  - BehaviorSubject: Reactive state sharing across components
  - Subscription Management: takeUntil pattern for cleanup
  - Local Storage: Persistent user preferences and workspace selection

  Dialog Patterns:
  - Material Dialog: Consistent dialog implementation
  - Result Handling: Proper dialog result processing
  - Theme-aware: Dialogs inherit M3 theming

  Workspace Multi-tenancy

  Routing Pattern:
  /login → /workspaces → /w/:slug/dashboard

  Key Features:
  - Workspace Context: Automatic tenant isolation
  - Slug-based Routing: SEO-friendly workspace URLs
  - Role-based Guards: WorkspaceMemberGuard for access control
  - Context Persistence: Workspace selection survives browser refresh

  Error Handling & UX

  Centralized Error Processing:
  - Interceptor-based: Consistent error handling across API calls
  - User-friendly Messages: Proper error extraction and display
  - Loading States: Global loading service integration
  - Notifications: Theme-aware snackbar with semantic colors

  Development Guidelines

  TypeScript Conventions:
  - Snake Case Interfaces: Match backend API structure (following Jackson configuration)
  - Strong Typing: Comprehensive interface definitions
  - Observable Patterns: Proper RxJS lifecycle management

  Component Lifecycle:
  - OnInit/OnDestroy: Standard lifecycle implementation
  - Memory Management: Proper subscription cleanup patterns
  - Component Communication: Services for cross-component state

  Testing Patterns:
  - Component testing with Angular Testing Library approach
  - Service testing with proper mock patterns
  - E2E testing for critical user flows

  Critical Rules

  1. Material Design 3 Only: Never use Bootstrap, Tailwind, or custom UI frameworks, Use design tokens which is created from m3 theme. Make components theme-aware.
  2. Snake Case APIs: Interface properties match backend naming
  3. Standalone Components: No NgModules in new code
  4. Service State Management: Do not Use BehaviorSubject for reactive state, use signal instead for angular 20 best practise.
  5. Interceptor Chain: Respect the established HTTP processing order
  6. Workspace Context: All business APIs must be workspace-aware
  7. Theme Integration: All components must support M3 theming
  8. Security First: Follow established auth and session patterns
  9. Use angular 20 best practices.

  This architecture supports a scalable, secure, multi-tenant business management platform with excellent UX and maintainable code patterns.

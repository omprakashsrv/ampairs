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

  Centralized Design Token System (src/theme/variables.scss):
  
  CRITICAL: Always use SCSS variables instead of direct CSS custom properties
  
  ```scss
  // Import pattern for all components
  @use '../../../theme/variables' as vars;
  @use '../../../theme/mixins' as theme;
  
  // Material 3 Color System - Complete token set
  $color-primary: var(--primary-color);
  $color-primary-container: var(--primary-container-color);
  $color-on-primary: var(--on-primary-color);
  $color-on-primary-container: var(--on-primary-container-color);
  
  $color-secondary: var(--secondary-color);
  $color-secondary-container: var(--secondary-container-color);
  $color-on-secondary-container: var(--on-secondary-container-color);
  
  $color-surface: var(--surface-color);
  $color-surface-container: var(--surface-container-color);
  $color-surface-container-low: var(--surface-container-low-color);
  $color-surface-container-high: var(--surface-container-high-color);
  $color-surface-variant: var(--surface-variant-color);
  $color-on-surface: var(--on-surface-color);
  $color-on-surface-variant: var(--on-surface-variant-color);
  
  $color-background: var(--background-color);
  $color-outline: var(--outline-color);
  $color-outline-variant: var(--outline-variant-color);
  
  // Semantic Colors
  $color-error: var(--error-color);
  $color-success: var(--success-color);
  $color-warning: var(--warning-color);
  $color-info: var(--info-color);
  
  // Menu System Colors
  $color-menu-item-label: var(--on-surface-color);
  $color-menu-item-supporting: var(--on-surface-variant-color);
  $color-menu-divider: var(--outline-variant-color);

  // Typography Scale - M3 System
  $font-size-xs: var(--mat-sys-typescale-label-small-size, 0.6875rem);   // 11px
  $font-size-sm: var(--mat-sys-typescale-label-medium-size, 0.75rem);    // 12px  
  $font-size-md: var(--mat-sys-typescale-body-medium-size, 0.875rem);    // 14px
  $font-size-lg: var(--mat-sys-typescale-title-medium-size, 1rem);       // 16px
  $font-size-xl: var(--mat-sys-typescale-title-large-size, 1.375rem);    // 22px
  $font-size-title: var(--mat-sys-typescale-headline-small-size, 1.5rem); // 24px

  // Spacing Scale - M3 System  
  $spacing-xs: var(--mat-sys-spacing-x-small, 0.25rem);      // 4px
  $spacing-sm: var(--mat-sys-spacing-small, 0.5rem);         // 8px
  $spacing-md: var(--mat-sys-spacing-medium, 0.75rem);       // 12px
  $spacing-lg: var(--mat-sys-spacing-large, 1rem);           // 16px
  $spacing-xl: var(--mat-sys-spacing-x-large, 1.5rem);       // 24px
  $spacing-xxl: var(--mat-sys-spacing-xx-large, 2rem);       // 32px

  // Layout Tokens
  $border-radius-sm: var(--mat-sys-corner-extra-small, 0.25rem);  // 4px
  $border-radius-md: var(--mat-sys-corner-small, 0.5rem);         // 8px
  $border-radius-lg: var(--mat-sys-corner-medium, 1rem);          // 16px
  $border-radius-xl: var(--mat-sys-corner-large, 1rem);           // 16px
  $border-radius-round: var(--mat-sys-corner-full, 625rem);       // Full round
  
  // Animation & Transitions
  $transition-fast: 0.15s;
  $transition-normal: 0.3s;
  $transition-slow: 0.5s;
  $transition-standard: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);   // M3 easing
  
  // Elevation System
  $shadow-1: var(--shadow-1);    // Light elevation
  $shadow-2: var(--shadow-2);    // Medium elevation
  ```
  
  Usage Pattern - Always use SCSS variables:
  ```scss
  // ✅ CORRECT - Use centralized SCSS variables
  .component {
    background-color: vars.$color-surface-container;
    color: vars.$color-on-surface;
    padding: vars.$spacing-lg vars.$spacing-xl;
    border-radius: vars.$border-radius-lg;
    box-shadow: vars.$shadow-1;
    transition: vars.$transition-standard;
  }
  
  // ❌ INCORRECT - Never use CSS custom properties directly  
  .component {
    background-color: var(--surface-container-color);
    color: var(--on-surface-color);
    padding: var(--spacing-lg, 16px);
  }
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

  1. Material Design 3 Only: Never use Bootstrap, Tailwind, or custom UI frameworks. ALWAYS use centralized SCSS variables from src/theme/variables.scss instead of direct CSS custom properties. Make all components theme-aware.
  2. Design Token Usage: MANDATORY use of SCSS variables (vars.$color-primary) instead of CSS custom properties (var(--primary-color)) for consistency and build optimization.
  3. Component Import Pattern: Always include '@use "../../../theme/variables" as vars;' and '@use "../../../theme/mixins" as theme;' in every component SCSS file.
  4. Snake Case APIs: Interface properties match backend naming (following Jackson snake_case configuration).
  5. Standalone Components: No NgModules in new code - use standalone component architecture.
  6. Service State Management: Use Angular signals for reactive state (Angular 20 best practice), avoid BehaviorSubject.
  7. Interceptor Chain: Respect established HTTP processing order (ApiResponse → Auth → Workspace → Loading).
  8. Workspace Context: All business APIs must be workspace-aware with proper tenant isolation.
  9. Theme Integration: All components must support complete M3 theming with proper color contrast and accessibility.
  10. Security First: Follow established auth patterns with JWT refresh, device tracking, and session management.
  11. Responsive Design: Use established breakpoint mixins (theme.mobile, theme.tablet) for consistent responsive behavior.
  12. Performance: Leverage build-time SCSS compilation while maintaining runtime theme switching capabilities.

  This architecture supports a scalable, secure, multi-tenant business management platform with excellent UX and maintainable code patterns.

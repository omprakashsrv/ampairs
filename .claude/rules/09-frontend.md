# Frontend Rules

## Angular Web (`ampairs-web/`)
- ONLY Angular Material 3 components (`@angular/material`) — no Bootstrap, Tailwind, PrimeNG, or other frameworks.
- Component selector prefix: `amp-`; co-locate `.ts`, `.html`, `.scss`.
- Icons: Material Design Icons only.
- Themes: Material 3 color tokens with light/dark mode support.
- Lint before merge: `npm run lint`.

## Compose Multiplatform (`ampairs-mp-app/`)
- Shared business logic and UI in `shared/src/commonMain`.
- Platform launchers (`androidApp`, `desktopApp`, `iosApp`) stay thin — delegate to shared code.
- Shared resources (strings, colors) stay in `commonMain` — no duplication in platform modules.

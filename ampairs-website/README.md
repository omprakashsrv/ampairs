## Ampairs Website (Next.js)

Marketing site for the Ampairs workspace-native business management platform. Built with Next.js App Router, TypeScript, Tailwind CSS, and modular content drawn from the backend, Angular, and multiplatform projects in this monorepo.

## Getting Started

```bash
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in the browser to view the site.

## Project Structure

```
src/
├── app/                # App Router entrypoints and global layout
│   ├── layout.tsx      # Global metadata + font configuration
│   └── page.tsx        # Homepage composed from section components
├── components/         # Layout primitives and themed sections
│   ├── layout/         # Header and footer
│   └── sections/       # Hero, features, CTA, FAQ, etc.
└── lib/                # Content configuration and navigation data
```

## Content Model

Marketing copy, CTA labels, and navigation items live in `src/lib/site.ts`. Update this file to tweak messaging without touching layouts or styles.

## Useful Commands

| Command | Purpose |
| --- | --- |
| `npm run dev` | Start the development server with hot reload |
| `npm run build` | Create an optimized production build |
| `npm start` | Serve the production build |
| `npm run lint` | Execute ESLint on the project |

## Deployment

Deploy to Vercel or any Node-compatible host. Set `NODE_ENV=production`, run `npm run build`, then `npm start`. Integrate with existing CI pipelines alongside `./gradlew ciBuild` so the marketing site ships with the rest of the Ampairs platform.

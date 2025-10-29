import { platformHighlights } from "@/lib/site";

export function PlatformHighlightsSection() {
  return (
    <section className="bg-muted py-24 dark:bg-[#0f172a]" id="solutions">
      <div className="mx-auto max-w-6xl px-6">
        <div className="grid gap-12 lg:grid-cols-[1fr,1.2fr]">
          <div className="space-y-5">
            <p className="text-sm font-semibold uppercase tracking-wide text-primary">
              Modules that work together
            </p>
            <h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">
              Domain-focused services plug into a unified workspace graph.
            </h2>
            <p className="text-base text-foreground/70">
              Each Ampairs service is purpose-built for a business domain yet
              shares the same tenant context, event stream, and API envelope.
              Activate only the modules you need and scale without silos.
            </p>
          </div>
          <div className="grid gap-6">
            {platformHighlights.map((highlight) => (
              <div
                key={highlight.name}
                className="rounded-3xl border border-foreground/10 bg-background px-6 py-6 shadow-sm dark:border-white/10"
              >
                <div className="flex items-center justify-between gap-4">
                  <h3 className="text-lg font-semibold">{highlight.name}</h3>
                  <span className="rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-primary">
                    {highlight.modules.length} services
                  </span>
                </div>
                <p className="mt-3 text-sm leading-6 text-foreground/70">
                  {highlight.summary}
                </p>
                <div className="mt-5 flex flex-wrap gap-2 text-xs font-medium uppercase tracking-wide text-foreground/60">
                  {highlight.modules.map((module) => (
                    <span
                      key={module}
                      className="rounded-full bg-foreground/5 px-3 py-1"
                    >
                      {module}
                    </span>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}

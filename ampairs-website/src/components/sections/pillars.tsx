import { solutionPillars } from "@/lib/site";

export function SolutionPillarsSection() {
  return (
    <section className="mx-auto max-w-6xl px-6 py-24" id="security">
      <div className="grid gap-12 lg:grid-cols-[1fr,1.1fr]">
        <div className="space-y-5">
          <p className="text-sm font-semibold uppercase tracking-wide text-primary">
            Why teams choose Ampairs
          </p>
          <h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">
            Ship fast, stay compliant, and scale globally.
          </h2>
          <p className="text-base text-foreground/70">
            Grounded in Spring Boot security patterns, hardened APIs, and a
            multi-channel delivery stack, Ampairs gives operators the controls
            they need without slowing releases.
          </p>
        </div>
        <div className="grid gap-6 md:grid-cols-3 md:gap-4">
          {solutionPillars.map((pillar) => (
            <div
              key={pillar.title}
              className="rounded-3xl border border-white/10 bg-background px-5 py-6 shadow-sm"
            >
              <h3 className="text-lg font-semibold">{pillar.title}</h3>
              <p className="mt-3 text-sm leading-6 text-foreground/70">
                {pillar.description}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

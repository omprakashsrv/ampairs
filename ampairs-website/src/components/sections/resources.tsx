import Link from "next/link";
import { resourceItems } from "@/lib/site";

export function ResourcesSection() {
  return (
    <section className="bg-background py-24" id="resources">
      <div className="mx-auto max-w-6xl px-6">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">
            Resources to accelerate your rollout
          </h2>
          <p className="mt-4 text-base text-foreground/70">
            Implementation guides and API references from the same teams that
            build our platform services.
          </p>
        </div>
        <div className="mt-12 grid gap-6 md:grid-cols-3">
          {resourceItems.map((resource) => (
            <Link
              key={resource.title}
              href={resource.href}
              className="group flex h-full flex-col justify-between rounded-3xl border border-white/10 bg-background px-6 py-6 text-left shadow-sm transition-transform hover:-translate-y-1 hover:shadow-lg"
            >
              <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-primary">
                  Resource
                </p>
                <h3 className="mt-3 text-lg font-semibold">
                  {resource.title}
                </h3>
                <p className="mt-3 text-sm leading-6 text-foreground/70">
                  {resource.description}
                </p>
              </div>
              <span className="mt-6 text-sm font-semibold text-primary group-hover:text-primary/80">
                Explore →
              </span>
            </Link>
          ))}
        </div>
      </div>
    </section>
  );
}

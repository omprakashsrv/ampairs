import Link from "next/link";
import { ArrowRight, PlayCircle } from "lucide-react";
import { heroCopy } from "@/lib/site";

export function HeroSection() {
  return (
    <section
      id="top"
      className="relative overflow-hidden bg-gradient-to-b from-white via-white to-accent/30 py-24 dark:from-[#0a0a0a] dark:via-[#0a0a0a] dark:to-[#0f172a]"
    >
      <div className="absolute left-1/2 top-20 h-[32rem] w-[32rem] -translate-x-1/2 rounded-full bg-primary/10 blur-3xl" />
      <div className="relative mx-auto flex max-w-6xl flex-col gap-12 px-6">
        <div className="inline-flex w-fit items-center gap-2 rounded-full border border-primary/20 bg-primary/10 px-3 py-1 text-xs font-semibold uppercase tracking-wider text-primary">
          {heroCopy.eyebrow}
        </div>
        <div className="grid gap-10 lg:grid-cols-[1.1fr,0.9fr] lg:items-center">
          <div className="space-y-8">
            <h1 className="text-4xl font-semibold tracking-tight sm:text-5xl">
              {heroCopy.title}
            </h1>
            <p className="max-w-xl text-lg text-foreground/70">
              {heroCopy.subtitle}
            </p>
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
              <Link
                href={heroCopy.primaryCta.href}
                className="inline-flex items-center justify-center rounded-full bg-primary px-6 py-3 text-sm font-semibold text-primary-foreground shadow-lg shadow-primary/25 transition-transform hover:-translate-y-0.5 hover:shadow-xl"
              >
                {heroCopy.primaryCta.label}
                <ArrowRight className="ml-2 h-4 w-4" />
              </Link>
              <Link
                href={heroCopy.secondaryCta.href}
                className="inline-flex items-center justify-center rounded-full border border-foreground/15 px-6 py-3 text-sm font-semibold text-foreground transition-colors hover:border-foreground/40"
              >
                {heroCopy.secondaryCta.label}
              </Link>
              <Link
                href={heroCopy.videoCta.href}
                className="inline-flex items-center justify-center gap-2 text-sm font-semibold text-primary transition-colors hover:text-primary/80"
              >
                <PlayCircle className="h-5 w-5" />
                {heroCopy.videoCta.label}
              </Link>
            </div>
          </div>
          <div className="relative">
            <div className="rounded-3xl border border-white/20 bg-white/80 p-6 shadow-xl shadow-primary/10 backdrop-blur dark:bg-white/5">
              <p className="text-sm font-semibold uppercase tracking-wide text-primary">
                Unified command centre
              </p>
              <h2 className="mt-3 text-2xl font-semibold text-foreground">
                Every workspace, every workflow, one timeline
              </h2>
              <p className="mt-4 text-sm leading-6 text-foreground/70">
                Monitor live orders, inventory deltas, invoice statuses, and
                notifications in one real-time view. Designed for multi-tenant
                operations teams who need precision and pace.
              </p>
              <div className="mt-6 grid gap-3 text-sm">
                <div className="flex items-center justify-between rounded-2xl bg-primary/5 px-4 py-3">
                  <span className="font-medium">Live orders</span>
                  <span className="font-semibold text-primary">+18 new</span>
                </div>
                <div className="flex items-center justify-between rounded-2xl bg-secondary/10 px-4 py-3">
                  <span className="font-medium">Inventory alerts</span>
                  <span className="font-semibold text-secondary">3 pending</span>
                </div>
                <div className="flex items-center justify-between rounded-2xl bg-muted px-4 py-3">
                  <span className="font-medium">Invoices reconciled</span>
                  <span className="font-semibold text-foreground/80">
                    97% today
                  </span>
                </div>
              </div>
            </div>
            <div className="absolute -bottom-10 -right-6 hidden w-40 rounded-2xl border border-white/10 bg-primary px-4 py-3 text-xs font-semibold text-primary-foreground shadow-lg shadow-primary/20 sm:block">
              Auto-sync to mobile agents
              <span className="mt-1 block text-[10px] font-normal text-primary-foreground/70">
                Kotlin multiplatform apps stay current in seconds.
              </span>
            </div>
          </div>
        </div>
        <dl className="grid gap-6 rounded-3xl border border-white/10 bg-white/80 px-6 py-6 text-sm shadow-sm backdrop-blur md:grid-cols-3 dark:bg-white/5">
          {heroCopy.metrics.map((metric) => (
            <div key={metric.label} className="space-y-1">
              <dt className="text-xs uppercase tracking-wide text-foreground/60">
                {metric.label}
              </dt>
              <dd className="text-2xl font-semibold text-foreground">
                {metric.value}
              </dd>
            </div>
          ))}
        </dl>
      </div>
    </section>
  );
}

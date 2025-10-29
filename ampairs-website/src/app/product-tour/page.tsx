import type { Metadata } from "next";
import Link from "next/link";
import { ArrowUpRight, MonitorSmartphone, Receipt, Workflow } from "lucide-react";
import { PageIntro } from "@/components/layout/page-intro";
import { featureList, platformHighlights } from "@/lib/site";

export const metadata: Metadata = {
  title: "Ampairs Product Tour",
  description:
    "See how Ampairs unifies orders, inventory, invoicing, automation, and engagement across workspace-native services.",
};

const timelineHighlights = [
  {
    label: "Customer places order",
    detail:
      "Angular web UI captures configurable forms, triggers inventory reservations, and publishes workspace events.",
  },
  {
    label: "Operations fulfill",
    detail:
      "Order service coordinates pick, pack, ship while notifications service keeps customers informed across channels.",
  },
  {
    label: "Finance reconciles",
    detail:
      "Invoice and tax services generate GST-compliant documents, sync with accounting exports, and close the loop.",
  },
];

const experienceTiles = [
  {
    icon: Workflow,
    title: "Command centre workspace",
    description:
      "Monitor live orders, stock movements, invoices, and notifications on a unified dashboard tailored per workspace role.",
  },
  {
    icon: Receipt,
    title: "Finance & compliance cockpit",
    description:
      "Track payments, taxation, discounts, and audit trails with exports ready for GST filings and enterprise reconciliation.",
  },
  {
    icon: MonitorSmartphone,
    title: "Field-ready mobile apps",
    description:
      "Kotlin multiplatform apps deliver offline-first experiences for field teams with secure sync, geolocation, and device policies.",
  },
];

export default function ProductTourPage() {
  return (
    <div className="bg-background text-foreground">
      <main className="mx-auto max-w-6xl space-y-20 px-6 py-24">
        <PageIntro
          eyebrow="Interactive Tour"
          title="Experience Ampairs in motion"
          description="Walk through the workflows that power high-velocity operations teams. Every screen maps directly to our Spring Boot services, Angular web app, and Kotlin multiplatform mobile clients."
        >
          <Link
            href="/#get-started"
            className="inline-flex items-center justify-center rounded-full bg-primary px-6 py-3 text-sm font-semibold text-primary-foreground shadow-lg shadow-primary/25 transition-transform hover:-translate-y-0.5 hover:shadow-xl"
          >
            Start free trial
            <ArrowUpRight className="ml-2 h-4 w-4" />
          </Link>
        </PageIntro>

        <section className="space-y-6 rounded-3xl border border-white/10 bg-background px-6 py-8 shadow-sm">
          <h2 className="text-2xl font-semibold">Workspace journey timeline</h2>
          <p className="text-sm leading-6 text-foreground/70">
            Ampairs orchestrates ordering, fulfillment, payments, and engagement in
            a real-time stream. Here&apos;s how a typical journey flows through the
            platform.
          </p>
          <ol className="grid gap-4 md:grid-cols-3">
            {timelineHighlights.map((item) => (
              <li
                key={item.label}
                className="rounded-2xl border border-foreground/10 bg-foreground/5 p-4"
              >
                <p className="text-xs font-semibold uppercase tracking-wide text-primary">
                  {item.label}
                </p>
                <p className="mt-2 text-sm leading-6 text-foreground/80">
                  {item.detail}
                </p>
              </li>
            ))}
          </ol>
        </section>

        <section className="space-y-10">
          <div className="max-w-2xl">
            <h2 className="text-2xl font-semibold">Product experiences</h2>
            <p className="mt-3 text-sm leading-6 text-foreground/70">
              Each Ampairs client surface consumes the same APIs and event streams.
              Configure once, deliver everywhere.
            </p>
          </div>
          <div className="grid gap-6 md:grid-cols-3">
            {experienceTiles.map((tile) => (
              <div
                key={tile.title}
                className="flex flex-col gap-4 rounded-3xl border border-white/10 bg-background px-5 py-6 shadow-sm"
              >
                <div className="inline-flex h-11 w-11 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                  <tile.icon className="h-5 w-5" />
                </div>
                <h3 className="text-lg font-semibold">{tile.title}</h3>
                <p className="text-sm leading-6 text-foreground/70">
                  {tile.description}
                </p>
              </div>
            ))}
          </div>
        </section>

        <section className="space-y-6">
          <div className="max-w-2xl">
            <h2 className="text-2xl font-semibold">
              Services powering the experience
            </h2>
            <p className="mt-3 text-sm leading-6 text-foreground/70">
              Behind every screen are Ampairs domain services. Activating modules is
              as simple as toggling workspace capabilities.
            </p>
          </div>
          <div className="grid gap-6 md:grid-cols-2">
            {featureList.slice(0, 4).map((feature) => (
              <div
                key={feature.title}
                className="rounded-3xl border border-white/10 bg-foreground/5 px-6 py-6"
              >
                <h3 className="text-lg font-semibold">{feature.title}</h3>
                <p className="mt-3 text-sm leading-6 text-foreground/70">
                  {feature.description}
                </p>
              </div>
            ))}
          </div>
        </section>

        <section className="space-y-6 rounded-3xl border border-white/10 bg-background px-6 py-8 shadow-sm">
          <h2 className="text-2xl font-semibold">
            Rollout paths for every team
          </h2>
          <div className="grid gap-6 md:grid-cols-2">
            {platformHighlights.map((highlight) => (
              <div key={highlight.name} className="space-y-3">
                <h3 className="text-lg font-semibold">{highlight.name}</h3>
                <p className="text-sm leading-6 text-foreground/70">
                  {highlight.summary}
                </p>
                <div className="flex flex-wrap gap-2 text-xs font-medium uppercase tracking-wide text-foreground/60">
                  {highlight.modules.map((module) => (
                    <span
                      key={module}
                      className="rounded-full bg-foreground/10 px-3 py-1"
                    >
                      {module}
                    </span>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="rounded-[2rem] border border-primary/30 bg-primary/10 px-8 py-10 text-center shadow-lg shadow-primary/20">
          <h2 className="text-2xl font-semibold text-primary">
            Want a guided walkthrough?
          </h2>
          <p className="mt-3 text-sm leading-6 text-primary/80">
            Share your workspace requirements and we&apos;ll tailor a live tour with
            real data flows.
          </p>
          <div className="mt-6 flex flex-col items-center justify-center gap-4 sm:flex-row">
            <Link
              href="/#contact"
              className="inline-flex items-center justify-center rounded-full bg-primary px-6 py-3 text-sm font-semibold text-primary-foreground shadow-lg shadow-primary/30 transition-transform hover:-translate-y-0.5 hover:shadow-xl"
            >
              Book a demo
            </Link>
            <Link
              href="/brief"
              className="inline-flex items-center justify-center rounded-full border border-primary/40 px-6 py-3 text-sm font-semibold text-primary transition-colors hover:border-primary/60 hover:text-primary/80"
            >
              Download product brief
            </Link>
          </div>
        </section>
      </main>
    </div>
  );
}

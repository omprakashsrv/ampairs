import type { Metadata } from "next";
import Link from "next/link";
import { ClipboardCheck, Timer } from "lucide-react";
import { PageIntro } from "@/components/layout/page-intro";

export const metadata: Metadata = {
  title: "Ampairs Implementation Playbook",
  description:
    "Follow a proven rollout plan to launch Ampairs across workspaces with minimal disruption.",
};

const phases = [
  {
    title: "Discover & align",
    description:
      "Audit current workflows, identify tenant boundaries, and map the data you plan to migrate.",
    checklist: [
      "Confirm workspace hierarchy and permission matrix",
      "Gather sample orders, invoices, and product catalogs",
      "Document integration touchpoints (ERPs, payment gateways)",
    ],
  },
  {
    title: "Configure & extend",
    description:
      "Stand up staging environments, seed catalog data, and customise form schemas for each team.",
    checklist: [
      "Enable required modules (orders, invoices, notifications, etc.)",
      "Configure automation triggers and notification channels",
      "Set up API integrations and QA test data flows",
    ],
  },
  {
    title: "Enable & launch",
    description:
      "Train workspace admins, run pilot cohorts, and execute production cutover with monitoring in place.",
    checklist: [
      "Run role-based training for web and mobile clients",
      "Validate reconciliation and tax reports in parallel",
      "Switch over production traffic with rollback plan",
    ],
  },
];

export default function ImplementationPlaybookPage() {
  return (
    <div className="bg-background text-foreground">
      <main className="mx-auto max-w-5xl space-y-16 px-6 py-24">
        <PageIntro
          eyebrow="Implementation Playbook"
          title="Launch Ampairs in four weeks"
          description="Use this phased approach to orchestrate a smooth rollout. Every step links to supporting guides maintained by the Ampairs product and solutions teams."
        >
          <div className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-4 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-primary">
            <Timer className="h-4 w-4" />
            Average go-live: 48 hours
          </div>
        </PageIntro>

        <section className="grid gap-6 md:grid-cols-3">
          {phases.map((phase) => (
            <div
              key={phase.title}
              className="flex flex-col gap-4 rounded-3xl border border-white/10 bg-background px-6 py-6 shadow-sm"
            >
              <div>
                <h2 className="text-lg font-semibold">{phase.title}</h2>
                <p className="mt-2 text-sm leading-6 text-foreground/70">
                  {phase.description}
                </p>
              </div>
              <div className="space-y-2">
                {phase.checklist.map((item) => (
                  <div key={item} className="flex items-start gap-2 text-sm text-foreground/70">
                    <ClipboardCheck className="mt-0.5 h-4 w-4 text-primary" />
                    <span>{item}</span>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </section>

        <section className="rounded-3xl border border-white/10 bg-background px-6 py-8 shadow-sm">
          <h2 className="text-xl font-semibold">Rollout checklist</h2>
          <ul className="grid gap-3 text-sm text-foreground/70 md:grid-cols-2">
            <li>✅ Configure tenant-aware API clients</li>
            <li>✅ Validate automation triggers across notifications</li>
            <li>✅ Import historical data with dry-run scripts</li>
            <li>✅ Align finance sign-off with invoice templates</li>
            <li>✅ Schedule hypercare support windows post-launch</li>
            <li>✅ Enable monitoring dashboards for SLAs</li>
          </ul>
        </section>

        <section className="space-y-4 rounded-3xl border border-white/10 bg-background px-6 py-8 shadow-sm">
          <h2 className="text-xl font-semibold">Partner with us</h2>
          <p className="text-sm leading-6 text-foreground/70">
            Our solutions engineers co-pilot deployments for enterprise plans.
            Share your timeline and we&apos;ll tailor workshops for each phase.
          </p>
          <Link
            href="/#contact"
            className="inline-flex w-fit items-center justify-center rounded-full border border-foreground/15 px-5 py-2 text-sm font-semibold transition-colors hover:border-foreground/40"
          >
            Schedule a workshop
          </Link>
        </section>
      </main>
    </div>
  );
}

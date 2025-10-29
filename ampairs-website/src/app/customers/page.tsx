import type { Metadata } from "next";
import Link from "next/link";
import { TrendingUp } from "lucide-react";
import { PageIntro } from "@/components/layout/page-intro";

export const metadata: Metadata = {
  title: "Ampairs Customer Stories",
  description:
    "See how businesses use Ampairs to streamline operations, finance, and customer engagement across workspaces.",
};

const caseStudies = [
  {
    company: "Maxwell Retail",
    metric: "30% faster order-to-cash",
    summary:
      "Unified product, order, and invoicing services cut reconciliation time and improved fulfillment accuracy.",
  },
  {
    company: "InstaServe Logistics",
    metric: "4x automation coverage",
    summary:
      "Event-driven workflows and multi-channel notifications kept field teams in sync without manual updates.",
  },
  {
    company: "Northstar Finance",
    metric: "Zero GST penalties",
    summary:
      "Tax configuration, audit-ready invoices, and scheduled exports made compliance a daily habit.",
  },
];

export default function CustomersPage() {
  return (
    <div className="bg-background text-foreground">
      <main className="mx-auto max-w-5xl space-y-16 px-6 py-24">
        <PageIntro
          eyebrow="Customer Stories"
          title="Teams scaling with Ampairs"
          description="From multi-location retail to fast-moving logistics, Ampairs helps operations teams stay in control. Explore how customers use our workspace-native services to deliver more with less."
        >
          <div className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-4 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-primary">
            <TrendingUp className="h-4 w-4" />
            50+ workspaces launched monthly
          </div>
        </PageIntro>

        <section className="grid gap-6 md:grid-cols-3">
          {caseStudies.map((study) => (
            <div
              key={study.company}
              className="rounded-3xl border border-white/10 bg-background px-6 py-6 shadow-sm"
            >
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary">
                {study.metric}
              </p>
              <h3 className="mt-2 text-lg font-semibold">{study.company}</h3>
              <p className="mt-3 text-sm leading-6 text-foreground/70">
                {study.summary}
              </p>
            </div>
          ))}
        </section>

        <section className="rounded-3xl border border-white/10 bg-background px-6 py-8 shadow-sm">
          <h2 className="text-xl font-semibold">Why they chose Ampairs</h2>
          <ul className="space-y-2 text-sm text-foreground/70">
            <li>⭐ Workspace-first tenancy with granular controls</li>
            <li>⭐ Real-time automation and notifications for every channel</li>
            <li>⭐ GST-compliant finance stack and export ready data</li>
            <li>⭐ Shared logic across web and mobile clients</li>
            <li>⭐ Dedicated rollout playbook and support</li>
          </ul>
        </section>

        <section className="space-y-4 rounded-3xl border border-white/10 bg-background px-6 py-8 shadow-sm">
          <h2 className="text-xl font-semibold">Share your story</h2>
          <p className="text-sm leading-6 text-foreground/70">
            Have a success to highlight? We love featuring operators who push the
            limits of automation. Submit your story and our team will reach out.
          </p>
          <Link
            href="mailto:stories@ampairs.in"
            className="inline-flex w-fit items-center justify-center rounded-full border border-foreground/15 px-5 py-2 text-sm font-semibold transition-colors hover:border-foreground/40"
          >
            Email stories@ampairs.in
          </Link>
        </section>
      </main>
    </div>
  );
}

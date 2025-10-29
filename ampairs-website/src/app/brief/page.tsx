import Link from "next/link";
import type { Metadata } from "next";
import { PageIntro } from "@/components/layout/page-intro";

export const metadata: Metadata = {
  title: "Ampairs Product Brief",
  description:
    "Download the Ampairs product brief to see how workspace-native services orchestrate operations, finance, and engagement for modern teams.",
};

export default function ProductBriefPage() {
  return (
    <div className="bg-background text-foreground">
      <main className="mx-auto max-w-4xl space-y-16 px-6 py-24">
        <PageIntro
          eyebrow="Product Brief"
          title="Ampairs workspace-native business management platform"
          description="Explore how Ampairs unifies Spring Boot microservices, Angular web, and Kotlin multiplatform apps into one SaaS offering that brings orders, invoices, inventory, and customer engagement into a single command centre."
        >
          <div className="flex flex-col gap-4 pt-4 sm:flex-row sm:items-center">
            <Link
              href="#download"
              className="inline-flex items-center justify-center rounded-full bg-primary px-6 py-3 text-sm font-semibold text-primary-foreground shadow-lg shadow-primary/25 transition-transform hover:-translate-y-0.5 hover:shadow-xl"
            >
              Download PDF brief
            </Link>
            <Link
              href="/#contact"
              className="inline-flex items-center justify-center rounded-full border border-foreground/15 px-6 py-3 text-sm font-semibold transition-colors hover:border-foreground/40"
            >
              Talk to our team
            </Link>
          </div>
        </PageIntro>

        <section className="space-y-4">
          <h2 className="text-xl font-semibold">What&apos;s inside</h2>
          <ul className="list-disc space-y-2 pl-6 text-sm leading-6 text-foreground/70">
            <li>
              Architecture overview of core backend domains (auth, workspace,
              orders, invoices, notifications) and how they inherit shared
              multi-tenancy controls.
            </li>
            <li>
              Product experience snapshots across the Angular web client and
              Kotlin multiplatform mobile apps for field and back office teams.
            </li>
            <li>
              Automation playbook covering event streaming, scheduled jobs, and
              notification orchestration that keeps operations real-time.
            </li>
            <li>
              Implementation milestones from sandbox provisioning to production
              cutover with migration considerations for GST compliance.
            </li>
          </ul>
        </section>

        <section className="space-y-4">
          <h2 className="text-xl font-semibold">
            Rollout blueprint in four weeks
          </h2>
          <ol className="list-decimal space-y-2 pl-6 text-sm leading-6 text-foreground/70">
            <li>
              <strong className="text-foreground">Week 1 · Discovery:</strong>{" "}
              Workspace modelling, data import review, and security posture
              confirmation.
            </li>
            <li>
              <strong className="text-foreground">Week 2 · Configure:</strong>{" "}
              Form schemas, tax rules, product catalogs, and automation triggers
              deployed to staging.
            </li>
            <li>
              <strong className="text-foreground">Week 3 · Enablement:</strong>{" "}
              Angular web admins and mobile field teams onboarded with training
              and role-based access.
            </li>
            <li>
              <strong className="text-foreground">Week 4 · Launch:</strong>{" "}
              Cutover with monitoring dashboards, notification fallback
              policies, and sandbox retention plan.
            </li>
          </ol>
        </section>

        <section id="download" className="space-y-4 rounded-3xl border border-white/10 bg-background px-6 py-6 shadow-sm">
          <h2 className="text-xl font-semibold">Download the brief</h2>
          <p className="text-sm leading-6 text-foreground/70">
            Request the latest PDF brief tailored to your workspace by emailing{" "}
            <a
              href="mailto:sales@ampairs.in?subject=Ampairs%20Product%20Brief%20Request"
              className="font-semibold text-primary hover:text-primary/80"
            >
              sales@ampairs.in
            </a>
            . We&apos;ll deliver a branded copy with the modules and rollout options that
            match your needs.
          </p>
        </section>

        <section className="space-y-4">
          <h2 className="text-xl font-semibold">Supporting resources</h2>
          <ul className="space-y-3 text-sm text-foreground/70">
            <li>
              <Link
                href="/api-reference"
                className="font-semibold text-primary hover:text-primary/80"
              >
                API Reference
              </Link>{" "}
              · Detailed REST specifications and request/response envelopes.
            </li>
            <li>
              <Link
                href="/implementation-playbook"
                className="font-semibold text-primary hover:text-primary/80"
              >
                Implementation Playbook
              </Link>{" "}
              · Configuration best practices across workspaces and modules.
            </li>
            <li>
              <Link
                href="/security"
                className="font-semibold text-primary hover:text-primary/80"
              >
                Security Overview
              </Link>{" "}
              · Encryption standards, RBAC controls, and compliance roadmap.
            </li>
          </ul>
        </section>
      </main>
    </div>
  );
}

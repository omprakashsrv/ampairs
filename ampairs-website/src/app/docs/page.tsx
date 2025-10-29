import type { Metadata } from "next";
import Link from "next/link";
import { BookOpen, FileText } from "lucide-react";
import { PageIntro } from "@/components/layout/page-intro";

export const metadata: Metadata = {
  title: "Ampairs Documentation",
  description:
    "Access guides for configuring workspaces, managing modules, and integrating with the Ampairs platform.",
};

const docSections = [
  {
    title: "Workspace administration",
    description:
      "Step-by-step guides for provisioning workspaces, configuring RBAC, and managing member onboarding.",
    href: "/implementation-playbook",
  },
  {
    title: "API guides",
    description:
      "REST and webhook references for integrating orders, products, invoices, and notifications into your systems.",
    href: "/api-reference",
  },
  {
    title: "Client apps",
    description:
      "Instructions for deploying Angular web and Kotlin multiplatform apps, including theme overrides and release pipelines.",
    href: "/product-tour",
  },
];

export default function DocumentationPage() {
  return (
    <div className="bg-background text-foreground">
      <main className="mx-auto max-w-5xl space-y-16 px-6 py-24">
        <PageIntro
          eyebrow="Documentation"
          title="Everything you need to run Ampairs"
          description="Browse implementation resources, API references, and rollout checklists curated by the teams that build Ampairs."
        >
          <div className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-4 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-primary">
            <BookOpen className="h-4 w-4" />
            Updated monthly
          </div>
        </PageIntro>

        <section className="grid gap-6 md:grid-cols-3">
          {docSections.map((section) => (
            <Link
              key={section.title}
              href={section.href}
              className="group flex flex-col justify-between rounded-3xl border border-white/10 bg-background px-6 py-6 shadow-sm transition-transform hover:-translate-y-1 hover:shadow-lg"
            >
              <div>
                <FileText className="h-5 w-5 text-primary" />
                <h2 className="mt-4 text-lg font-semibold">{section.title}</h2>
                <p className="mt-2 text-sm leading-6 text-foreground/70">
                  {section.description}
                </p>
              </div>
              <span className="mt-6 text-sm font-semibold text-primary group-hover:text-primary/80">
                View guide →
              </span>
            </Link>
          ))}
        </section>

        <section className="space-y-4 rounded-3xl border border-white/10 bg-background px-6 py-8 shadow-sm">
          <h2 className="text-xl font-semibold">Need something else?</h2>
          <p className="text-sm leading-6 text-foreground/70">
            Reach out to{" "}
            <a
              href="mailto:support@ampairs.in"
              className="font-semibold text-primary hover:text-primary/80"
            >
              support@ampairs.in
            </a>{" "}
            for detailed implementation questions or to request additional
            documentation.
          </p>
        </section>
      </main>
    </div>
  );
}

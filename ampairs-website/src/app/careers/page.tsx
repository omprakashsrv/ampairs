import type { Metadata } from "next";
import Link from "next/link";
import { Rocket } from "lucide-react";
import { PageIntro } from "@/components/layout/page-intro";

export const metadata: Metadata = {
  title: "Careers at Ampairs",
  description:
    "Join the team building the workspace-native business management platform powering modern operations teams.",
};

const openRoles = [
  {
    title: "Senior Backend Engineer (Kotlin/Spring)",
    location: "Bengaluru · Hybrid",
    summary:
      "Own domain services (orders, invoices, notifications) and evolve our multi-tenant architecture.",
  },
  {
    title: "Product Designer",
    location: "Remote (India)",
    summary:
      "Design experiences spanning Angular web and Kotlin multiplatform apps with a shared design system.",
  },
  {
    title: "Solutions Architect",
    location: "Mumbai · Client-facing",
    summary:
      "Guide enterprise rollouts, integrate Ampairs APIs, and deliver implementation workshops.",
  },
];

export default function CareersPage() {
  return (
    <div className="bg-background text-foreground">
      <main className="mx-auto max-w-5xl space-y-16 px-6 py-24">
        <PageIntro
          eyebrow="Careers"
          title="Build the workspace-native future"
          description="We are a distributed team united by the mission to simplify operations. Join us to ship secure, high-impact features across web, mobile, and backend services."
        >
          <div className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-4 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-primary">
            <Rocket className="h-4 w-4" />
            Hiring across India
          </div>
        </PageIntro>

        <section className="grid gap-6 md:grid-cols-3">
          {openRoles.map((role) => (
            <div
              key={role.title}
              className="rounded-3xl border border-white/10 bg-background px-6 py-6 shadow-sm"
            >
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary">
                {role.location}
              </p>
              <h3 className="mt-2 text-lg font-semibold">{role.title}</h3>
              <p className="mt-3 text-sm leading-6 text-foreground/70">
                {role.summary}
              </p>
              <Link
                href="mailto:careers@ampairs.in"
                className="mt-4 inline-flex items-center justify-center rounded-full border border-foreground/15 px-4 py-2 text-sm font-semibold transition-colors hover:border-foreground/40"
              >
                Apply via email
              </Link>
            </div>
          ))}
        </section>

        <section className="rounded-3xl border border-white/10 bg-background px-6 py-8 shadow-sm">
          <h2 className="text-xl font-semibold">Life at Ampairs</h2>
          <ul className="space-y-2 text-sm text-foreground/70">
            <li>💡 Product squads own features end-to-end across all platforms</li>
            <li>🌐 Remote-first with hubs in Bengaluru and Mumbai</li>
            <li>📚 Learning budget and conference sponsorships</li>
            <li>🩺 Comprehensive health insurance for you and your family</li>
            <li>🎉 Quarterly build weeks focused on innovation</li>
          </ul>
        </section>

        <section className="space-y-4 rounded-3xl border border-white/10 bg-background px-6 py-8 shadow-sm">
          <h2 className="text-xl font-semibold">Don&apos;t see your role?</h2>
          <p className="text-sm leading-6 text-foreground/70">
            We&apos;re always excited to meet builders and operators. Share your
            profile and we&apos;ll reach out when there&apos;s a fit.
          </p>
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
            <Link
              href="mailto:careers@ampairs.in?subject=General%20Application"
              className="inline-flex items-center justify-center rounded-full bg-primary px-6 py-3 text-sm font-semibold text-primary-foreground shadow-lg shadow-primary/25 transition-transform hover:-translate-y-0.5 hover:shadow-xl"
            >
              Send resume
            </Link>
            <Link
              href="/about"
              className="inline-flex items-center justify-center rounded-full border border-foreground/15 px-6 py-3 text-sm font-semibold transition-colors hover:border-foreground/40"
            >
              Learn about Ampairs
            </Link>
          </div>
        </section>
      </main>
    </div>
  );
}

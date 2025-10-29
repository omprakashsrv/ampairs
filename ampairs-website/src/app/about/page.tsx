import type { Metadata } from "next";
import Link from "next/link";
import { Building, Globe, Users } from "lucide-react";
import { PageIntro } from "@/components/layout/page-intro";

export const metadata: Metadata = {
  title: "About Ampairs",
  description:
    "Meet the team building the Ampairs workspace-native business management platform and learn how our mission aligns with modern operations teams.",
};

const leadership = [
  {
    name: "Aanya Menon",
    title: "Co-founder & CEO",
    bio: "Guides product strategy and customer success after leading operations transformations across enterprise retail.",
  },
  {
    name: "Rahul Singh",
    title: "Co-founder & CTO",
    bio: "Architects the modular Spring Boot backend and Kotlin multiplatform stack, ensuring every workspace is secure by design.",
  },
  {
    name: "Meera Desai",
    title: "Head of Product",
    bio: "Bridges product research, Angular web, and mobile experiences so cross-functional teams ship with clarity.",
  },
];

const milestones = [
  {
    year: "2022",
    title: "Ampairs founded",
    description:
      "Launched the workspace-native architecture to solve multi-tenant operational sprawl for mid-market businesses.",
  },
  {
    year: "2023",
    title: "Multichannel rollout",
    description:
      "Released Angular web and Kotlin multiplatform apps backed by unified APIs, enabling field and back office collaboration.",
  },
  {
    year: "2024",
    title: "Automation platform",
    description:
      "Expanded notification, event streaming, and form services to orchestrate real-time workflows across verticals.",
  },
  {
    year: "2025",
    title: "Global expansion",
    description:
      "Scaling regional deployments and investing in compliance, including SOC 2 Type I and data residency options.",
  },
];

export default function AboutPage() {
  return (
    <div className="bg-background text-foreground">
      <main className="mx-auto max-w-5xl space-y-20 px-6 py-24">
        <PageIntro
          eyebrow="About Ampairs"
          title="Building the operating system for every workspace"
          description="Ampairs exists to remove friction for operations teams who manage orders, inventory, finance, and customer journeys across multiple workspaces. We combine modern engineering with pragmatic rollout playbooks to help businesses launch faster, scale confidently, and stay compliant."
        >
          <div className="flex flex-col gap-4 pt-4 sm:flex-row sm:items-center">
            <Link
              href="/product-tour"
              className="inline-flex items-center justify-center rounded-full bg-primary px-6 py-3 text-sm font-semibold text-primary-foreground shadow-lg shadow-primary/25 transition-transform hover:-translate-y-0.5 hover:shadow-xl"
            >
              Explore product tour
            </Link>
            <Link
              href="/#contact"
              className="inline-flex items-center justify-center rounded-full border border-foreground/15 px-6 py-3 text-sm font-semibold transition-colors hover:border-foreground/40"
            >
              Talk with the team
            </Link>
          </div>
        </PageIntro>

        <section className="grid gap-6 md:grid-cols-3">
          {[
            {
              icon: Building,
              title: "Workspace-first architecture",
              description:
                "Every feature is grounded in tenancy, RBAC, and automation so enterprises can onboard teams without refactoring.",
            },
            {
              icon: Users,
              title: "Cross-functional squads",
              description:
                "Product, engineering, and design squads co-own backend modules, Angular features, and Compose multiplatform apps.",
            },
            {
              icon: Globe,
              title: "Global by design",
              description:
                "Regional deployments, GST compliance, and translation-ready clients keep us aligned with international customers.",
            },
          ].map((item) => (
            <div
              key={item.title}
              className="rounded-3xl border border-white/10 bg-background px-6 py-6 shadow-sm"
            >
              <div className="inline-flex h-11 w-11 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                <item.icon className="h-5 w-5" />
              </div>
              <h3 className="mt-4 text-lg font-semibold">{item.title}</h3>
              <p className="mt-2 text-sm leading-6 text-foreground/70">
                {item.description}
              </p>
            </div>
          ))}
        </section>

        <section className="space-y-6">
          <h2 className="text-2xl font-semibold">Leadership</h2>
          <div className="grid gap-6 md:grid-cols-3">
            {leadership.map((leader) => (
              <div
                key={leader.name}
                className="rounded-3xl border border-white/10 bg-background px-5 py-6 shadow-sm"
              >
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
                  {leader.title}
                </p>
                <h3 className="mt-2 text-lg font-semibold">{leader.name}</h3>
                <p className="mt-3 text-sm leading-6 text-foreground/70">
                  {leader.bio}
                </p>
              </div>
            ))}
          </div>
        </section>

        <section className="space-y-6">
          <h2 className="text-2xl font-semibold">Milestones</h2>
          <div className="grid gap-6 md:grid-cols-2">
            {milestones.map((milestone) => (
              <div
                key={milestone.year}
                className="rounded-3xl border border-white/10 bg-foreground/5 px-6 py-5"
              >
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
                  {milestone.year}
                </p>
                <h3 className="mt-2 text-lg font-semibold">{milestone.title}</h3>
                <p className="mt-3 text-sm leading-6 text-foreground/70">
                  {milestone.description}
                </p>
              </div>
            ))}
          </div>
        </section>

        <section className="space-y-4 rounded-3xl border border-white/10 bg-background px-6 py-8 shadow-sm">
          <h2 className="text-2xl font-semibold">Where we&apos;re headed</h2>
          <p className="text-sm leading-6 text-foreground/70">
            We are investing heavily in automation, compliance, and intelligence
            layers that learn from workspace activity. Expect deeper integrations
            with payments, ledger systems, and AI-enabled workflows that surface
            insights across the stack.
          </p>
          <p className="text-sm leading-6 text-foreground/70">
            Interested in building with us? Explore open roles and contribute to
            our mission to streamline operations for every workspace.
          </p>
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
            <Link
              href="/careers"
              className="inline-flex items-center justify-center rounded-full bg-primary px-6 py-3 text-sm font-semibold text-primary-foreground shadow-lg shadow-primary/25 transition-transform hover:-translate-y-0.5 hover:shadow-xl"
            >
              View careers
            </Link>
            <Link
              href="mailto:careers@ampairs.in"
              className="inline-flex items-center justify-center rounded-full border border-foreground/15 px-6 py-3 text-sm font-semibold transition-colors hover:border-foreground/40"
            >
              Email recruiting
            </Link>
          </div>
        </section>
      </main>
    </div>
  );
}

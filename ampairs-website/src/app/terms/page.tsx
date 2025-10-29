import type { Metadata } from "next";
import Link from "next/link";
import { PageIntro } from "@/components/layout/page-intro";

export const metadata: Metadata = {
  title: "Ampairs Terms of Service",
  description:
    "Review the terms that govern access to the Ampairs workspace-native business management platform.",
};

export default function TermsPage() {
  return (
    <div className="bg-background text-foreground">
      <main className="mx-auto max-w-4xl space-y-16 px-6 py-24">
        <PageIntro
          eyebrow="Terms of Service"
          title="Agreement for using the Ampairs platform"
          description="These terms describe how Ampairs provides access to the platform, how customers may use the services, and mutual responsibilities for keeping every workspace secure and compliant."
        >
          <p className="text-xs uppercase tracking-[0.25em] text-foreground/60">
            Effective date: 29 October 2025
          </p>
        </PageIntro>

        <section className="space-y-4">
          <h2 className="text-xl font-semibold">1. Subscription & access</h2>
          <p className="text-sm leading-6 text-foreground/70">
            Ampairs grants customers a non-exclusive, non-transferable licence to
            access the platform for the duration of the contracted term. Access is
            provisioned per workspace; administrators control memberships, roles,
            and feature flags using the Workspace service.
          </p>
        </section>

        <section className="space-y-4">
          <h2 className="text-xl font-semibold">2. Customer responsibilities</h2>
          <ul className="list-disc space-y-2 pl-6 text-sm leading-6 text-foreground/70">
            <li>
              Maintain accurate billing, tax, and compliance information.
            </li>
            <li>
              Configure role-based access controls and enforce strong
              authentication for workspace members.
            </li>
            <li>
              Ensure data uploaded to Ampairs complies with applicable laws,
              including GST regulations.
            </li>
          </ul>
        </section>

        <section className="space-y-4">
          <h2 className="text-xl font-semibold">3. Service commitments</h2>
          <p className="text-sm leading-6 text-foreground/70">
            Ampairs targets 99.95% uptime measured monthly. Planned maintenance
            notifications are issued at least 48 hours in advance. SLAs for
            support response times are available in your subscription agreement.
          </p>
        </section>

        <section className="space-y-4">
          <h2 className="text-xl font-semibold">4. Data ownership</h2>
          <p className="text-sm leading-6 text-foreground/70">
            Customers retain all rights to data stored in their workspaces.
            Ampairs processes this data solely to deliver the services. Data usage
            is further detailed in the{" "}
            <Link
              href="/privacy"
              className="font-semibold text-primary hover:text-primary/80"
            >
              Privacy Policy
            </Link>
            .
          </p>
        </section>

        <section className="space-y-4">
          <h2 className="text-xl font-semibold">5. Prohibited use</h2>
          <p className="text-sm leading-6 text-foreground/70">
            Customers may not reverse engineer or attempt to circumvent security
            measures, resell the service without authorisation, or transmit
            malicious code. Violations result in suspension and may trigger
            termination.
          </p>
        </section>

        <section className="space-y-4">
          <h2 className="text-xl font-semibold">6. Termination</h2>
          <p className="text-sm leading-6 text-foreground/70">
            Either party may terminate for material breach with 30 days&apos;
            written notice if the breach remains uncured. Upon termination,
            customers may export workspace data for 30 days by contacting
            support@ampairs.in.
          </p>
        </section>

        <section className="space-y-4">
          <h2 className="text-xl font-semibold">Contact</h2>
          <p className="text-sm leading-6 text-foreground/70">
            Questions about these terms? Reach out to{" "}
            <a
              href="mailto:legal@ampairs.in"
              className="font-semibold text-primary hover:text-primary/80"
            >
              legal@ampairs.in
            </a>{" "}
            or your Ampairs account representative.
          </p>
        </section>
      </main>
    </div>
  );
}

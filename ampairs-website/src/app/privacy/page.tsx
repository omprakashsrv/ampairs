import type { Metadata } from "next";
import Link from "next/link";
import { PageIntro } from "@/components/layout/page-intro";

export const metadata: Metadata = {
  title: "Ampairs Privacy Policy",
  description:
    "Learn how Ampairs collects, processes, and protects customer data across the workspace-native business management platform.",
};

export default function PrivacyPolicyPage() {
  return (
    <div className="bg-background text-foreground">
      <main className="mx-auto max-w-4xl space-y-16 px-6 py-24">
        <PageIntro
          eyebrow="Privacy Policy"
          title="Protecting data across every workspace"
          description="Ampairs is designed to help businesses orchestrate operations securely. This privacy statement describes what data we collect, how we use it, and the controls available to your workspace administrators."
        >
          <p className="text-xs uppercase tracking-[0.25em] text-foreground/60">
            Last updated: 29 October 2025
          </p>
        </PageIntro>

        <section className="space-y-4">
          <h2 className="text-xl font-semibold">1. Data we collect</h2>
          <p className="text-sm leading-6 text-foreground/70">
            Ampairs collects only the information required to deliver the
            platform and comply with regulation. Data is scoped to a workspace
            and includes:
          </p>
          <ul className="list-disc space-y-2 pl-6 text-sm leading-6 text-foreground/70">
            <li>
              <strong className="text-foreground">Account data:</strong> user
              identifiers, profile details, and authentication devices managed by
              the Auth service.
            </li>
            <li>
              <strong className="text-foreground">Business operations data:</strong>{" "}
              customers, products, orders, invoices, and supporting files
              configured through domain services.
            </li>
            <li>
              <strong className="text-foreground">Telemetry:</strong> usage events,
              API access logs, and device context used for diagnostics and
              security monitoring.
            </li>
          </ul>
        </section>

        <section className="space-y-4">
          <h2 className="text-xl font-semibold">2. How we use data</h2>
          <ul className="list-disc space-y-2 pl-6 text-sm leading-6 text-foreground/70">
            <li>
              Deliver, maintain, and improve workspace features such as order
              processing, invoicing, and notifications.
            </li>
            <li>
              Provide support, resolve incidents, and troubleshoot performance
              using scoped audit logs.
            </li>
            <li>
              Meet legal or regulatory obligations, including GST compliance and
              billing.
            </li>
          </ul>
        </section>

        <section className="space-y-4">
          <h2 className="text-xl font-semibold">3. Data retention & deletion</h2>
          <p className="text-sm leading-6 text-foreground/70">
            Workspace administrators control retention policies. Operational data
            is retained while the subscription remains active, after which it is
            scheduled for deletion following a 30-day grace period. Backups used
            for disaster recovery are encrypted and rotated on a 35-day cycle.
          </p>
        </section>

        <section className="space-y-4">
          <h2 className="text-xl font-semibold">4. Subprocessors</h2>
          <p className="text-sm leading-6 text-foreground/70">
            Ampairs leverages vetted infrastructure providers for hosting,
            storage, and communications. A current list of subprocessors and their
            data residency commitments is available in the{" "}
            <Link
              href="/security"
              className="font-semibold text-primary hover:text-primary/80"
            >
              Security Overview
            </Link>
            .
          </p>
        </section>

        <section className="space-y-4">
          <h2 className="text-xl font-semibold">5. Your rights</h2>
          <ul className="list-disc space-y-2 pl-6 text-sm leading-6 text-foreground/70">
            <li>
              Access and export data stored within your workspace modules.
            </li>
            <li>
              Update or delete records using in-product tools or API endpoints.
            </li>
            <li>
              Request removal or restriction of processing by contacting
              privacy@ampairs.in. We respond within 30 days.
            </li>
          </ul>
        </section>

        <section className="space-y-4">
          <h2 className="text-xl font-semibold">Contact</h2>
          <p className="text-sm leading-6 text-foreground/70">
            For privacy requests or questions, email{" "}
            <a
              href="mailto:privacy@ampairs.in"
              className="font-semibold text-primary hover:text-primary/80"
            >
              privacy@ampairs.in
            </a>{" "}
            or write to Ampairs Technologies, Bengaluru, India.
          </p>
        </section>
      </main>
    </div>
  );
}

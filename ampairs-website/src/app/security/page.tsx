import type { Metadata } from "next";
import Link from "next/link";
import { ShieldCheck } from "lucide-react";
import { PageIntro } from "@/components/layout/page-intro";

export const metadata: Metadata = {
  title: "Ampairs Security Overview",
  description:
    "Discover how Ampairs protects customer data with layered infrastructure, application, and compliance controls across the platform.",
};

export default function SecurityPage() {
  return (
    <div className="bg-background text-foreground">
      <main className="mx-auto max-w-5xl space-y-16 px-6 py-24">
        <PageIntro
          eyebrow="Security"
          title="Layered protection for every workspace"
          description="Security is embedded into Ampairs architecture—from multi-tenant isolation in the Workspace service to encrypted mobile sync. Below is a summary of the safeguards we maintain and how to engage with our security team."
        >
          <div className="flex items-center gap-2 rounded-full bg-primary/10 px-4 py-2 text-xs font-semibold uppercase tracking-[0.25em] text-primary">
            <ShieldCheck className="h-4 w-4" />
            SOC 2 Type I in progress
          </div>
        </PageIntro>

        <section className="space-y-4">
          <h2 className="text-xl font-semibold">Infrastructure security</h2>
          <ul className="list-disc space-y-2 pl-6 text-sm leading-6 text-foreground/70">
            <li>
              Ampairs runs on hardened cloud infrastructure with network
              segmentation between application, database, and messaging layers.
            </li>
            <li>
              Data at rest is encrypted with AES-256, while all data in transit
              uses TLS 1.3. Keys are rotated via managed KMS.
            </li>
            <li>
              Continuous monitoring with alerting on anomalous behaviour and
              infrastructure drift.
            </li>
          </ul>
        </section>

        <section className="space-y-4">
          <h2 className="text-xl font-semibold">Application security</h2>
          <ul className="list-disc space-y-2 pl-6 text-sm leading-6 text-foreground/70">
            <li>
              Multi-factor authentication and device pinning managed by the Auth
              service.
            </li>
            <li>
              Tenant context enforcement executed in the Core and Workspace
              modules to prevent cross-tenant data access.
            </li>
            <li>
              Secure defaults in Angular and Kotlin multiplatform clients
              including certificate pinning and secure storage.
            </li>
            <li>
              Static code analysis, dependency scanning, and peer reviews
              integrated into `./gradlew ciBuild`.
            </li>
          </ul>
        </section>

        <section className="space-y-4">
          <h2 className="text-xl font-semibold">Data protection & privacy</h2>
          <p className="text-sm leading-6 text-foreground/70">
            We minimise data collection and apply retention controls aligned with
            workspace policies. For more detail, see the{" "}
            <Link
              href="/privacy"
              className="font-semibold text-primary hover:text-primary/80"
            >
              Privacy Policy
            </Link>
            .
          </p>
          <ul className="list-disc space-y-2 pl-6 text-sm leading-6 text-foreground/70">
            <li>Granular role-based access controls per workspace and module.</li>
            <li>Encrypted backups with 35-day retention and disaster recovery testing.</li>
            <li>Data residency options within India with regional deployment roadmap.</li>
          </ul>
        </section>

        <section className="space-y-4">
          <h2 className="text-xl font-semibold">Compliance roadmap</h2>
          <p className="text-sm leading-6 text-foreground/70">
            Ampairs follows industry frameworks while formal certifications are
            under way. Our roadmap includes SOC 2 Type I (target Q4 2025) and ISO
            27001. Data processing agreements and GST compliance artefacts are
            available on request.
          </p>
        </section>

        <section className="space-y-4">
          <h2 className="text-xl font-semibold">Incident response</h2>
          <ul className="list-disc space-y-2 pl-6 text-sm leading-6 text-foreground/70">
            <li>24×7 on-call rotation across infrastructure and application teams.</li>
            <li>
              Documented playbooks covering containment, eradication, and
              customer communication.
            </li>
            <li>
              Post-incident reviews shared with impacted customers within five
              business days.
            </li>
          </ul>
        </section>

        <section className="space-y-4">
          <h2 className="text-xl font-semibold">Responsible disclosure</h2>
          <p className="text-sm leading-6 text-foreground/70">
            We welcome reports from the security community. If you discover a
            vulnerability, email{" "}
            <a
              href="mailto:security@ampairs.in"
              className="font-semibold text-primary hover:text-primary/80"
            >
              security@ampairs.in
            </a>{" "}
            with steps to reproduce. We commit to acknowledging submissions within
            two business days.
          </p>
        </section>

        <section className="space-y-3 rounded-3xl border border-white/10 bg-background px-6 py-6 shadow-sm">
          <h2 className="text-lg font-semibold">Security resources</h2>
          <ul className="space-y-2 text-sm text-foreground/70">
            <li>
              <Link
                href="/privacy"
                className="font-semibold text-primary hover:text-primary/80"
              >
                Privacy Policy
              </Link>
              {" · "}Data handling and retention commitments.
            </li>
            <li>
              <Link
                href="/terms"
                className="font-semibold text-primary hover:text-primary/80"
              >
                Terms of Service
              </Link>
              {" · "}Usage requirements and incident responsibilities.
            </li>
            <li>
              <Link
                href="/status"
                className="font-semibold text-primary hover:text-primary/80"
              >
                Status Page
              </Link>
              {" · "}Real-time uptime and incident history.
            </li>
          </ul>
        </section>
      </main>
    </div>
  );
}

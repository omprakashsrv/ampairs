import type { Metadata } from "next";
import Link from "next/link";
import { Code, GitCommit } from "lucide-react";
import { PageIntro } from "@/components/layout/page-intro";

export const metadata: Metadata = {
  title: "Ampairs API Reference",
  description:
    "Discover REST endpoints, event payloads, and webhook formats for the Ampairs workspace-native platform.",
};

const apiDomains = [
  {
    name: "Authentication & workspaces",
    summary:
      "Manage OTP flows, JWT sessions, workspace memberships, and RBAC assignments.",
    endpoints: ["/auth/v1", "/workspace/v1"],
  },
  {
    name: "Commerce operations",
    summary:
      "Create and manage customers, products, orders, and invoices with GST-ready payloads.",
    endpoints: ["/customer/v1", "/product/v1", "/order/v1", "/invoice/v1"],
  },
  {
    name: "Automation & engagement",
    summary:
      "Stream events, trigger notifications, and configure dynamic forms per workspace.",
    endpoints: ["/event/v1", "/notification/v1", "/form/v1"],
  },
];

export default function ApiReferencePage() {
  return (
    <div className="bg-background text-foreground">
      <main className="mx-auto max-w-5xl space-y-16 px-6 py-24">
        <PageIntro
          eyebrow="API Reference"
          title="Integrate with Ampairs services"
          description="Build on top of the same APIs that power our Angular web and Kotlin multiplatform apps. REST responses follow consistent envelopes with tenant-aware context."
        >
          <div className="flex items-center gap-2 rounded-full bg-primary/10 px-4 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-primary">
            <Code className="h-4 w-4" />
            Stable contracts
          </div>
        </PageIntro>

        <section className="rounded-3xl border border-white/10 bg-background px-6 py-8 shadow-sm">
          <h2 className="text-xl font-semibold">Authentication</h2>
          <p className="mt-3 text-sm leading-6 text-foreground/70">
            Authenticate using OAuth 2.0 compatible flows with workspace headers.
            Requests include the `X-Workspace-Id` header to scope tenant data.
          </p>
          <pre className="mt-4 overflow-x-auto rounded-2xl bg-foreground/5 p-4 text-xs text-foreground/80">
{`POST /auth/v1/init
{
  "phone": "+91XXXXXXXXXX"
}`}
          </pre>
        </section>

        <section className="grid gap-6 md:grid-cols-2">
          {apiDomains.map((domain) => (
            <div
              key={domain.name}
              className="rounded-3xl border border-white/10 bg-background px-6 py-6 shadow-sm"
            >
              <h3 className="text-lg font-semibold">{domain.name}</h3>
              <p className="mt-2 text-sm leading-6 text-foreground/70">
                {domain.summary}
              </p>
              <div className="mt-3 flex flex-wrap gap-2 text-xs font-medium uppercase tracking-wide text-foreground/60">
                {domain.endpoints.map((endpoint) => (
                  <span
                    key={endpoint}
                    className="rounded-full bg-foreground/10 px-3 py-1"
                  >
                    {endpoint}
                  </span>
                ))}
              </div>
            </div>
          ))}
        </section>

        <section className="rounded-3xl border border-white/10 bg-background px-6 py-8 shadow-sm">
          <h2 className="text-xl font-semibold">Changelog</h2>
          <ul className="space-y-3 text-sm text-foreground/70">
            <li className="flex items-start gap-2">
              <GitCommit className="mt-0.5 h-4 w-4 text-primary" />
              <span>
                <strong className="text-foreground">v1.4</strong> · Added webhook
                retries with exponential backoff for notification deliveries.
              </span>
            </li>
            <li className="flex items-start gap-2">
              <GitCommit className="mt-0.5 h-4 w-4 text-primary" />
              <span>
                <strong className="text-foreground">v1.3</strong> · Introduced
                order tagging and invoice reconciliation status endpoints.
              </span>
            </li>
            <li className="flex items-start gap-2">
              <GitCommit className="mt-0.5 h-4 w-4 text-primary" />
              <span>
                <strong className="text-foreground">v1.2</strong> · Enhanced
                workspace membership APIs with device session metadata.
              </span>
            </li>
          </ul>
        </section>

        <section className="space-y-4 rounded-3xl border border-white/10 bg-background px-6 py-8 shadow-sm">
          <h2 className="text-xl font-semibold">Get support</h2>
          <p className="text-sm leading-6 text-foreground/70">
            Request sandbox access, client credentials, or feature previews by
            contacting{" "}
            <a
              href="mailto:developers@ampairs.in"
              className="font-semibold text-primary hover:text-primary/80"
            >
              developers@ampairs.in
            </a>
            .
          </p>
          <Link
            href="/docs"
            className="inline-flex w-fit items-center justify-center rounded-full border border-foreground/15 px-5 py-2 text-sm font-semibold transition-colors hover:border-foreground/40"
          >
            Back to documentation
          </Link>
        </section>
      </main>
    </div>
  );
}

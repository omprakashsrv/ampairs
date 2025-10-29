import type { Metadata } from "next";
import Link from "next/link";
import { Activity, CheckCircle, Clock } from "lucide-react";
import { PageIntro } from "@/components/layout/page-intro";

export const metadata: Metadata = {
  title: "Ampairs Status",
  description:
    "Check real-time and historical availability for the Ampairs platform.",
};

const statusHistory = [
  {
    date: "2025-10-20",
    status: "Normal",
    note: "No incidents reported. All regions operating normally.",
    icon: CheckCircle,
  },
  {
    date: "2025-10-14",
    status: "Resolved",
    note: "Delayed notification delivery in APAC region. Mitigated within 12 minutes.",
    icon: Activity,
  },
  {
    date: "2025-10-02",
    status: "Maintenance",
    note: "Planned infrastructure updates completed with no downtime.",
    icon: Clock,
  },
];

export default function StatusPage() {
  return (
    <div className="bg-background text-foreground">
      <main className="mx-auto max-w-5xl space-y-16 px-6 py-24">
        <PageIntro
          eyebrow="Status"
          title="Platform availability"
          description="Ampairs monitors uptime across all services and regions. Subscribe to updates or review the latest incidents below."
        >
          <div className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-4 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-primary">
            <CheckCircle className="h-4 w-4" />
            Operational
          </div>
        </PageIntro>

        <section className="rounded-3xl border border-white/10 bg-background px-6 py-8 shadow-sm">
          <h2 className="text-xl font-semibold">Subscribe to updates</h2>
          <p className="mt-3 text-sm leading-6 text-foreground/70">
            Receive incident notifications via email or SMS. Workspace admins can
            configure alert channels inside the Notification service.
          </p>
          <Link
            href="/#contact"
            className="mt-4 inline-flex w-fit items-center justify-center rounded-full border border-foreground/15 px-5 py-2 text-sm font-semibold transition-colors hover:border-foreground/40"
          >
            Contact support
          </Link>
        </section>

        <section className="space-y-4 rounded-3xl border border-white/10 bg-background px-6 py-8 shadow-sm">
          <h2 className="text-xl font-semibold">Recent updates</h2>
          <ul className="space-y-4">
            {statusHistory.map((entry) => (
              <li key={entry.date} className="flex gap-3 rounded-2xl border border-white/10 bg-foreground/5 px-5 py-4">
                <entry.icon className="mt-1 h-5 w-5 text-primary" />
                <div className="space-y-1 text-sm text-foreground/70">
                  <p className="font-semibold text-foreground">
                    {entry.date} · {entry.status}
                  </p>
                  <p>{entry.note}</p>
                </div>
              </li>
            ))}
          </ul>
        </section>
      </main>
    </div>
  );
}

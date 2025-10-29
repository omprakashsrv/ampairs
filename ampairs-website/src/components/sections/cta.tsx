import Link from "next/link";
import { contactCta } from "@/lib/site";

export function ContactSection() {
  return (
    <section
      id="contact"
      className="relative overflow-hidden py-24 sm:py-28"
    >
      <div className="absolute inset-0 -z-10 bg-gradient-to-r from-primary/20 via-secondary/20 to-primary/20 blur-3xl" />
      <div
        id="get-started"
        className="mx-auto max-w-4xl rounded-[2.5rem] border border-white/20 bg-background/80 px-8 py-14 text-center shadow-2xl backdrop-blur dark:bg-white/5"
      >
        <div className="inline-flex items-center justify-center rounded-full border border-primary/40 bg-primary/15 px-4 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-primary">
          Let’s build together
        </div>
        <h2 className="mt-6 text-3xl font-semibold tracking-tight sm:text-4xl">
          {contactCta.title}
        </h2>
        <p className="mt-4 text-base text-foreground/70">
          {contactCta.description}
        </p>
        <div className="mt-8 flex flex-col justify-center gap-4 sm:flex-row">
          <Link
            href={contactCta.primary.href}
            className="inline-flex items-center justify-center rounded-full bg-primary px-6 py-3 text-sm font-semibold text-primary-foreground shadow-lg shadow-primary/30 transition-transform hover:-translate-y-0.5 hover:shadow-xl"
          >
            {contactCta.primary.label}
          </Link>
          <Link
            href={contactCta.secondary.href}
            className="inline-flex items-center justify-center rounded-full border border-foreground/15 px-6 py-3 text-sm font-semibold transition-colors hover:border-foreground/40"
          >
            {contactCta.secondary.label}
          </Link>
        </div>
        <p className="mt-6 text-xs uppercase tracking-[0.25em] text-foreground/60">
          No implementation fees · Enterprise-ready · SOC 2 in progress
        </p>
      </div>
    </section>
  );
}

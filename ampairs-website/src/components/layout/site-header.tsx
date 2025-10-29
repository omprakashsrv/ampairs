import Link from "next/link";
import { siteNav } from "@/lib/site";

export function SiteHeader() {
  return (
    <header className="sticky top-0 z-50 border-b border-white/10 bg-background/80 backdrop-blur">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-5">
        <Link
          href="/"
          className="flex items-center gap-2 font-semibold text-lg tracking-tight"
        >
          <span className="inline-flex h-9 w-9 items-center justify-center rounded-full bg-primary/20 text-primary">
            A
          </span>
          <span>Ampairs</span>
        </Link>
        <nav className="hidden items-center gap-8 text-sm font-medium text-foreground/70 md:flex">
          {siteNav.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className="transition-colors hover:text-foreground"
            >
              {item.label}
            </Link>
          ))}
        </nav>
        <div className="hidden items-center gap-3 md:flex">
          <Link
            href="/#contact"
            className="rounded-full border border-foreground/10 px-4 py-2 text-sm font-medium transition-colors hover:border-foreground/20 hover:text-foreground"
          >
            Talk to sales
          </Link>
          <Link
            href="/#get-started"
            className="rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground shadow-sm transition-transform hover:-translate-y-0.5 hover:shadow"
          >
            Start free trial
          </Link>
        </div>
        <Link
          href="/#get-started"
          className="rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground shadow-sm transition-transform hover:-translate-y-0.5 hover:shadow md:hidden"
        >
          Get started
        </Link>
      </div>
    </header>
  );
}

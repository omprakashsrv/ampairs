import Link from "next/link";

const footerLinks = [
  {
    title: "Product",
    items: [
      { label: "Platform overview", href: "/#platform" },
      { label: "Security", href: "/security" },
      { label: "Pricing", href: "/#get-started" },
    ],
  },
  {
    title: "Resources",
    items: [
      { label: "Documentation", href: "/docs" },
      { label: "Status", href: "/status" },
      { label: "API reference", href: "/api-reference" },
    ],
  },
  {
    title: "Company",
    items: [
      { label: "About", href: "/about" },
      { label: "Careers", href: "/careers" },
      { label: "Contact", href: "/#contact" },
    ],
  },
];

export function SiteFooter() {
  return (
    <footer className="border-t border-white/10 bg-background">
      <div className="mx-auto grid max-w-6xl gap-10 px-6 py-16 md:grid-cols-[2fr,3fr]">
        <div className="space-y-4">
          <div className="flex items-center gap-2 text-lg font-semibold tracking-tight">
            <span className="inline-flex h-9 w-9 items-center justify-center rounded-full bg-primary/20 text-primary">
              A
            </span>
            Ampairs
          </div>
          <p className="max-w-sm text-sm text-foreground/70">
            Ampairs is the workspace-native business management platform
            orchestrating operations, finance, and customer journeys across
            every device.
          </p>
          <div className="text-sm text-foreground/70">
            support@ampairs.in · +91 98765 43210
          </div>
        </div>
        <div className="grid gap-8 sm:grid-cols-3">
          {footerLinks.map((section) => (
            <div key={section.title} className="space-y-3">
              <h3 className="text-sm font-semibold uppercase tracking-wide text-foreground/60">
                {section.title}
              </h3>
              <ul className="space-y-2 text-sm text-foreground/70">
                {section.items.map((link) => (
                  <li key={link.href}>
                    <Link
                      href={link.href}
                      className="transition-colors hover:text-foreground"
                    >
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </div>
      <div className="border-t border-white/10 py-6">
        <div className="mx-auto flex max-w-6xl flex-col gap-4 px-6 text-xs text-foreground/60 sm:flex-row sm:items-center sm:justify-between">
          <p>© {new Date().getFullYear()} Ampairs Technologies. All rights reserved.</p>
          <div className="flex gap-4">
            <Link href="/privacy">Privacy</Link>
            <Link href="/terms">Terms</Link>
            <Link href="/security">Security</Link>
          </div>
        </div>
      </div>
    </footer>
  );
}

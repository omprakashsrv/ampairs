export const siteNav = [
  { label: "Platform", href: "/#platform" },
  { label: "Solutions", href: "/#solutions" },
  { label: "Product Tour", href: "/product-tour" },
  { label: "Security", href: "/security" },
  { label: "About", href: "/about" },
  { label: "Resources", href: "/#resources" },
];

export const heroCopy = {
  eyebrow: "Workspace-native SaaS",
  title: "Operate every workspace on one modern platform",
  subtitle:
    "Ampairs centralises orders, inventory, invoices, and customer engagement in a multi-tenant architecture that scales with your business.",
  primaryCta: { label: "Start free trial", href: "/#get-started" },
  secondaryCta: { label: "Book a demo", href: "/#contact" },
  videoCta: {
    label: "See how Ampairs works",
    href: "/product-tour",
  },
  metrics: [
    { value: "50+", label: "Workspaces launched per week" },
    { value: "99.95%", label: "Uptime across regions" },
    { value: "48hrs", label: "Average go-live time" },
  ],
};

export const featureList = [
  {
    title: "Workspace-first multi-tenancy",
    description:
      "Tenant isolation, RBAC, and invitation workflows are built into Ampairs core so you can onboard new teams without re-architecting.",
    icon: "layers",
  },
  {
    title: "Unified commerce engine",
    description:
      "Orders, products, pricing, and tax rules live in one catalog shared across channels, keeping sales, fulfillment, and finance aligned.",
    icon: "infinity",
  },
  {
    title: "Real-time automation",
    description:
      "Event streaming, notifications, and scheduled jobs orchestrate updates the moment inventory changes, invoices are issued, or customers act.",
    icon: "sparkles",
  },
  {
    title: "Composable data capture",
    description:
      "Dynamic form definitions let you tailor data models per workspace without redeploying clients across web, desktop, or mobile.",
    icon: "blocks",
  },
  {
    title: "GST-compliant finance",
    description:
      "Invoice and tax services stay audit ready with GST rate schedules, HSN catalogues, and PDF-ready payloads out of the box.",
    icon: "shield-check",
  },
  {
    title: "Cross-platform experiences",
    description:
      "Angular web and Kotlin multiplatform apps reuse shared logic so teams and field agents stay in sync on any device.",
    icon: "devices",
  },
];

export const platformHighlights = [
  {
    name: "Identity & Security",
    summary:
      "OTP and JWT authentication, device session controls, and workspace-aware permissions keep data scoped to every tenant.",
    modules: ["auth", "workspace", "core"],
  },
  {
    name: "Customer Operations",
    summary:
      "Customer CRM, product catalogs, orders, and invoicing stitch into one flow from quote to cash.",
    modules: ["customer", "product", "order", "invoice"],
  },
  {
    name: "Revenue Intelligence",
    summary:
      "Track payments, taxation, and discounting with audit trails and reconciliation-ready exports.",
    modules: ["invoice", "tax", "order"],
  },
  {
    name: "Engagement & Automation",
    summary:
      "Notifications, events, and form schemas adapt journeys per workspace while keeping experiences consistent.",
    modules: ["notification", "event", "form"],
  },
];

export const solutionPillars = [
  {
    title: "Launch fast",
    description:
      "Spin up regional workspaces in minutes with seed data, default permissions, and ready-to-use catalogs.",
  },
  {
    title: "Scale everywhere",
    description:
      "Extend experiences across web and native apps backed by the same APIs, security posture, and automation.",
  },
  {
    title: "Stay compliant",
    description:
      "Audit-ready GST reporting, secure data residency, and fine-grained access controls keep regulators and customers satisfied.",
  },
];

export const resourceItems = [
  {
    title: "API reference",
    description:
      "Explore REST endpoints for orders, invoices, notifications, and more with consistent API response envelopes.",
    href: "/api-reference",
  },
  {
    title: "Implementation Playbook",
    description:
      "Best practices for configuring workspaces, payment rules, and automation scripts during rollout.",
    href: "/implementation-playbook",
  },
  {
    title: "Customer Stories",
    description:
      "See how operations teams reduce manual reconciliation and launch new revenue lines with Ampairs.",
    href: "/customers",
  },
  {
    title: "Product tour",
    description:
      "Walk through Ampairs orchestration of orders, inventory, finance, and engagement with live UI snapshots.",
    href: "/product-tour",
  },
];

export const faqItems = [
  {
    question: "How does Ampairs handle multi-tenant data isolation?",
    answer:
      "Tenant context flows through every API request using workspace-scoped filters, ensuring data, automation, and notifications are isolated by workspace by default.",
  },
  {
    question: "Can I customise data capture for different teams?",
    answer:
      "Yes. The Forms service lets you version field definitions per entity and workspace, so you can tailor workflows without updating the clients.",
  },
  {
    question: "Do you support mobile field operations?",
    answer:
      "Ampairs ships with Kotlin Multiplatform apps for Android, iOS, and desktop that share business logic with the Angular web experience.",
  },
];

export const contactCta = {
  title: "Ready to modernise your operations?",
  description:
    "Join high-growth teams using Ampairs to orchestrate every workspace from onboarding to cash collection.",
  primary: { label: "Schedule a discovery call", href: "mailto:sales@ampairs.in" },
  secondary: { label: "Download product brief", href: "/brief" },
};

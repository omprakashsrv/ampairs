import {
  Blocks,
  Infinity,
  Layers,
  MonitorSmartphone,
  ShieldCheck,
  Sparkles,
} from "lucide-react";
import { featureList } from "@/lib/site";

const iconMap = {
  layers: Layers,
  infinity: Infinity,
  sparkles: Sparkles,
  blocks: Blocks,
  "shield-check": ShieldCheck,
  devices: MonitorSmartphone,
};

export function FeaturesSection() {
  return (
    <section
      id="platform"
      className="mx-auto max-w-6xl px-6 py-24"
      aria-labelledby="platform-heading"
    >
      <div className="mx-auto max-w-2xl text-center">
        <h2
          id="platform-heading"
          className="text-3xl font-semibold tracking-tight sm:text-4xl"
        >
          Built for modern operations teams
        </h2>
        <p className="mt-4 text-base text-foreground/70">
          Ampairs weaves together the services your teams rely on—from identity
          and orchestration to finance and engagement—so every workspace runs
          smoothly.
        </p>
      </div>
      <div className="mt-16 grid gap-6 md:grid-cols-2">
        {featureList.map((feature) => {
          const Icon = iconMap[feature.icon as keyof typeof iconMap];
          return (
            <div
              key={feature.title}
              className="group relative overflow-hidden rounded-3xl border border-white/10 bg-background px-6 py-8 shadow-sm transition-transform hover:-translate-y-1 hover:shadow-lg"
            >
              <div className="absolute -right-14 -top-14 h-36 w-36 rounded-full bg-primary/10 blur-3xl transition-opacity group-hover:opacity-100" />
              <div className="relative flex items-center gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                  {Icon ? <Icon className="h-6 w-6" /> : null}
                </div>
                <h3 className="text-lg font-semibold">{feature.title}</h3>
              </div>
              <p className="relative mt-4 text-sm leading-6 text-foreground/70">
                {feature.description}
              </p>
            </div>
          );
        })}
      </div>
    </section>
  );
}

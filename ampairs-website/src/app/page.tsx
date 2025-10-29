import { SiteFooter } from "@/components/layout/site-footer";
import { SiteHeader } from "@/components/layout/site-header";
import { ContactSection } from "@/components/sections/cta";
import { FaqSection } from "@/components/sections/faq";
import { FeaturesSection } from "@/components/sections/features";
import { HeroSection } from "@/components/sections/hero";
import { PlatformHighlightsSection } from "@/components/sections/platform";
import { ResourcesSection } from "@/components/sections/resources";
import { SolutionPillarsSection } from "@/components/sections/pillars";

export default function Home() {
  return (
    <div className="flex min-h-screen flex-col bg-background text-foreground">
      <SiteHeader />
      <main className="flex-1">
        <HeroSection />
        <FeaturesSection />
        <PlatformHighlightsSection />
        <SolutionPillarsSection />
        <ResourcesSection />
        <FaqSection />
        <ContactSection />
      </main>
      <SiteFooter />
    </div>
  );
}

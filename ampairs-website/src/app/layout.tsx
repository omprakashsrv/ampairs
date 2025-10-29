import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";

const defaultSiteUrl =
  process.env.NODE_ENV === "production"
    ? "https://www.ampairs.in"
    : "http://localhost:3000";
const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? defaultSiteUrl;

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
  display: "swap",
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
  display: "swap",
});

export const metadata: Metadata = {
  title: "Ampairs | Workspace-Native Business Management Platform",
  description:
    "Ampairs unifies orders, invoices, inventory, and customer journeys in a workspace-native platform with real-time automation across web and mobile.",
  icons: {
    icon: "/favicon.ico",
  },
  metadataBase: new URL(siteUrl),
  openGraph: {
    title: "Ampairs | Workspace-Native Business Management Platform",
    description:
      "Launch, scale, and automate every workspace with unified orders, invoices, inventory, GST compliance, and multi-channel notifications.",
    siteName: "Ampairs",
    url: siteUrl,
    locale: "en_US",
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: "Ampairs | Workspace-Native Business Management Platform",
    description:
      "Modern SaaS to orchestrate operations, finance, and engagement across every workspace.",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body
        className={`${geistSans.variable} ${geistMono.variable} bg-background text-foreground antialiased`}
      >
        {children}
      </body>
    </html>
  );
}

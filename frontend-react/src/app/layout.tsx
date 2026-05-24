import type { Metadata } from "next";
import "./globals.css";
import { ToastProvider } from "@/context/ToastContext";

export const metadata: Metadata = {
  title: "Football 501",
  description: "Competitive football trivia — darts 501 scoring mechanics",
  manifest: "/manifest.json",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="h-full">
      <body className="min-h-full flex flex-col bg-[var(--color-background)] text-[var(--color-on-background)]">
        <ToastProvider>{children}</ToastProvider>
      </body>
    </html>
  );
}

import type { Metadata } from "next";
import "./globals.css";
import { SessionProvider } from "@/lib/session";
import { Shell } from "@/components/shell";

export const metadata: Metadata = {
  title: "PerfumeStore",
  description: "Perfumes escolhidos para marcar momentos.",
  robots: { index: false, follow: false }
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="pt-BR"><body><SessionProvider><Shell>{children}</Shell></SessionProvider></body></html>;
}

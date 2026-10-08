import type { Metadata } from "next";
import { Barlow_Condensed, Inter } from "next/font/google";
import Script from "next/script";
import { AuthProvider } from "@/components/auth/AuthProvider";
import { TopNav } from "@/components/layout/TopNav";
import "./globals.css";

const barlowCondensed = Barlow_Condensed({
  subsets: ["latin"],
  weight: ["800"],
  variable: "--font-barlow-condensed",
  display: "swap",
});

const inter = Inter({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
  variable: "--font-inter",
  display: "swap",
});

export const metadata: Metadata = {
  title: "OlympusGYM",
  description: "Reservas de clases con aforo, entrenador con inteligencia artificial y gestión para un gimnasio real.",
};

// Se ejecuta antes de pintar para evitar el parpadeo del tema (DESIGN.md §2).
// Oscuro es el valor por defecto: solo marcamos data-theme cuando toca claro,
// ya sea por preferencia guardada o, la primera vez, por prefers-color-scheme.
const themeInitScript = `
(function () {
  try {
    var stored = localStorage.getItem('ogym-theme');
    var theme = stored === 'light' || stored === 'dark'
      ? stored
      : (window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark');
    if (theme === 'light') {
      document.documentElement.setAttribute('data-theme', 'light');
    }
  } catch (e) {}
})();
`;

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html
      lang="es"
      className={`${barlowCondensed.variable} ${inter.variable}`}
      // El script de theme-init cambia data-theme antes de hidratar según localStorage:
      // el HTML del servidor nunca puede saber esa preferencia, así que el mismatch es
      // esperado y se silencia aquí (no afecta a nada más del árbol).
      suppressHydrationWarning
    >
      <body className="min-h-screen bg-bg font-sans text-text antialiased">
        <Script id="theme-init" strategy="beforeInteractive">
          {themeInitScript}
        </Script>
        <AuthProvider>
          <TopNav />
          {children}
        </AuthProvider>
      </body>
    </html>
  );
}

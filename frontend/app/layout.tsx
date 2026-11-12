import type { Metadata } from "next";
import { Geist } from "next/font/google";
import "./globals.css";
import Providers from "./providers";
import Cabecalho from "./cabecalho";

const geist = Geist({ variable: "--font-geist-sans", subsets: ["latin"] });

export const metadata: Metadata = {
  title: "govtech-demo",
  description: "Portal de demonstração para solicitação de alvará. Dados fictícios.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="pt-BR" className={`${geist.variable} h-full antialiased`}>
      <body className="min-h-full flex flex-col">
        <Providers>
          <Cabecalho />
          <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-6">{children}</main>
        </Providers>
      </body>
    </html>
  );
}

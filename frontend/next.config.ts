import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // O navegador fala so com o Next (mesma origem) e /api/* segue para o Spring.
  // Assim o cookie de sessao e o token CSRF funcionam sem configurar CORS.
  async rewrites() {
    const api = process.env.API_URL ?? "http://localhost:8080";
    return [{ source: "/api/:path*", destination: `${api}/api/:path*` }];
  },
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;

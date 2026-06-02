import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Proxy /api/* requests to the Spring Boot backend.
  // In dev, defaults to localhost:8080. In production, set API_URL env var
  // to point at the backend (e.g. https://backend-rosy-cloud-4618.fly.dev).
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${process.env.API_URL || "http://localhost:8080"}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;

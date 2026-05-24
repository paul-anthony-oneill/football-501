import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Proxy /api/* requests to the Spring Boot backend in development
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: "http://localhost:8080/api/:path*",
      },
    ];
  },
};

export default nextConfig;

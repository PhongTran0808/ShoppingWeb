import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  allowedDevOrigins: ['172.25.96.1', 'localhost', '172.25.32.1', 'tienthienvienman.site.je', '127.0.0.1'],
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: 'http://127.0.0.1:8080/api/:path*'
      }
    ];
  }
};

export default nextConfig;

/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  async headers() {
    // CSP ships Report-Only first: surfaces violations in the browser console
    // without breaking the live app (inline theme script, hls.js blobs, Leaflet
    // tiles, PortOne pay SDK). Flip the header name to "Content-Security-Policy"
    // to enforce once the console is clean across the key flows.
    const csp = [
      "default-src 'self'",
      // 'unsafe-inline'/'unsafe-eval': Next.js runtime + inline theme script + hls.js.
      "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.iamport.kr https://service.iamport.kr",
      "style-src 'self' 'unsafe-inline'",
      "img-src 'self' data: blob: https:",
      "media-src 'self' blob: https:",
      "font-src 'self' data:",
      // https:/wss: kept broad for the API, CloudFront media, OSM tiles, PortOne.
      "connect-src 'self' https: wss:",
      // PortOne opens its pay UI in an iframe/popup.
      "frame-src 'self' https://service.iamport.kr https://cdn.iamport.kr",
      "object-src 'none'",
      "base-uri 'self'",
      "form-action 'self'",
      "frame-ancestors 'none'",
    ].join("; ");
    return [
      {
        source: "/(.*)",
        headers: [
          { key: "Content-Security-Policy-Report-Only", value: csp },
          { key: "X-Frame-Options", value: "DENY" },
          { key: "X-Content-Type-Options", value: "nosniff" },
          {
            key: "Referrer-Policy",
            value: "strict-origin-when-cross-origin",
          },
          {
            key: "Strict-Transport-Security",
            value: "max-age=31536000; includeSubDomains",
          },
          {
            // geolocation=(self): church finder uses the geolocation API.
            key: "Permissions-Policy",
            value: "camera=(), microphone=(), geolocation=(self)",
          },
        ],
      },
    ];
  },
};

export default nextConfig;

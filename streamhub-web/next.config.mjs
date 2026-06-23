/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  async headers() {
    // CSP ships Report-Only first: surfaces violations in the browser console
    // without breaking the admin (inline scripts, ApexCharts, AG Grid, CKEditor
    // blobs/workers). Flip the header name to "Content-Security-Policy" to
    // enforce once the console is clean across the key flows.
    const csp = [
      "default-src 'self'",
      // 'unsafe-inline'/'unsafe-eval': Next.js runtime + AG Grid + CKEditor.
      "script-src 'self' 'unsafe-inline' 'unsafe-eval' blob:",
      "style-src 'self' 'unsafe-inline'",
      "img-src 'self' data: blob: https:",
      "media-src 'self' blob: https:",
      "font-src 'self' data:",
      // worker-src blob: for CKEditor/AG Grid web workers.
      "worker-src 'self' blob:",
      // https:/wss: kept broad for the admin API and CloudFront media.
      "connect-src 'self' https: wss:",
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
            key: "Permissions-Policy",
            value: "camera=(), microphone=(), geolocation=()",
          },
        ],
      },
    ];
  },
};

export default nextConfig;

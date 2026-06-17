# Free deploy ($0/month) — Vercel frontends + one always-free VM backend

A portfolio-friendly, **no-AWS, no-cost** way to put this whole project online.

```
   Visitors ──▶  Vercel (Hobby, free)              ──▶  one always-free VM (e.g. Oracle Cloud)
                 ├ streamhub-web      (admin)            Caddy :443  (auto HTTPS, Let's Encrypt)
                 └ streamhub-user-web (public)            └─▶ api :8080
                       │  HTTPS fetch                          ├─ MySQL 8     (container)
                       └──────────────────────────────────────┤─ Redis       (container)
                                                               ├─ MinIO (S3)  (container)
                                                               └─ LocalStack  (SQS, container)
```

**Why this is free and simple:** the API **self-seeds on every boot** (deterministic demo data),
so it needs *no managed database* — the bundled MySQL container is enough. An always-free VM runs
the full `docker compose` stack 24/7 (no cold starts), and Vercel hosts the two Next.js apps for
free. External keys are not required: payment/SMS/chatbot/maps all run in mock/seed mode.

> Trade-off: the VM's MySQL is **ephemeral** — a VM reboot re-seeds the identical demo data. That's
> ideal for a portfolio. If you ever want persistence, the `mysql-data` volume already survives
> `docker compose down` (only a full volume wipe resets it).

---

## Part 1 — Backend on an always-free VM

Recommended host: **Oracle Cloud "Always Free"** (2 AMD micro VMs or up to 4 ARM cores / 24 GB —
genuinely free forever, always-on). Any free/cheap VM with Docker works (Google Cloud e2-micro
free tier, a spare box, etc.).

1. **Create the VM** (Ubuntu 22.04+), and in its network/security list **open inbound TCP 80 and
   443** (and 22 for SSH).
2. **Install Docker + Compose:**
   ```bash
   curl -fsSL https://get.docker.com | sh
   sudo usermod -aG docker $USER && newgrp docker
   ```
3. **Get a free HTTPS domain** (needed so Vercel's HTTPS pages can call the backend):
   - Sign up at <https://www.duckdns.org> (free), create a subdomain, point it at the VM's public IP.
   - Example: `streamhub-demo.duckdns.org`.
4. **Clone and launch:**
   ```bash
   git clone https://github.com/1mJeeHwan/streamhub-admin.git
   cd streamhub-admin
   export PUBLIC_DOMAIN=streamhub-demo.duckdns.org
   export JWT_SECRET="$(openssl rand -base64 48)"
   # (fill these in after Part 2 with the real Vercel URLs; localhost is fine for the first boot)
   export APP_CORS_ALLOWED_ORIGINS=https://your-admin.vercel.app,https://your-userweb.vercel.app
   docker compose -f docker-compose.yml -f docker-compose.deploy.yml up -d --build
   ```
   Caddy obtains a Let's Encrypt certificate automatically on first request.
5. **Verify:** open `https://streamhub-demo.duckdns.org/v3/api-docs` and
   `https://streamhub-demo.duckdns.org/pub/v1/home`. First boot takes ~1–2 min (build + seed).

Useful: `docker compose -f docker-compose.yml -f docker-compose.deploy.yml logs -f api`

---

## Part 2 — Frontends on Vercel (free)

Create **two** Vercel projects from the same GitHub repo (Add New → Project → import
`1mJeeHwan/streamhub-admin`), each with a different **Root Directory**:

| Vercel project | Root Directory | Env vars (Production) |
|---|---|---|
| admin | `streamhub-web` | `NEXT_PUBLIC_API_BASE_URL=https://<PUBLIC_DOMAIN>` · `NEXTAUTH_SECRET=<openssl rand -base64 32>` · `NEXTAUTH_URL=https://<this-project>.vercel.app` |
| public | `streamhub-user-web` | `NEXT_PUBLIC_API_BASE=https://<PUBLIC_DOMAIN>` |

Both projects already ship a `vercel.json` (framework=nextjs, `npm install --legacy-peer-deps`).
See `streamhub-web/.env.production.example` and `streamhub-user-web/.env.production.example`.

After the Vercel URLs exist, **update CORS on the VM** so the backend accepts them:

```bash
export APP_CORS_ALLOWED_ORIGINS=https://your-admin.vercel.app,https://your-userweb.vercel.app
docker compose -f docker-compose.yml -f docker-compose.deploy.yml up -d   # recreates api with new env
```

Admin demo login: `admin` / `admin1234`. Public site needs no login.

---

## Cost

| Piece | Service | Cost |
|---|---|---|
| 2 Next.js frontends | Vercel Hobby | **$0** |
| Backend + MySQL/Redis/MinIO/SQS | Oracle Cloud Always Free VM | **$0** |
| HTTPS domain | DuckDNS | **$0** |
| **Total** | | **$0 / month** |

---

## Alternative — even less setup (but cold starts)

Prefer a git-push PaaS over a VM? Run **only the API container** on a free tier
(**Render** / **Koyeb** — both give automatic HTTPS) and attach a free MySQL
(**TiDB Cloud Serverless** or **Aiven** free MySQL). Point `SPRING_PROFILES_ACTIVE=prod` with
`DB_HOST/DB_USER/DB_PASSWORD/JWT_SECRET/APP_CORS_ALLOWED_ORIGINS` env, set `JPA_DDL_AUTO=update`
for the first boot. Caveats: free web services **sleep when idle** (~30–60 s wake), and MinIO/SQS
aren't available there (admin file uploads + the action-log queue degrade — everything else works,
since thumbnails are external URLs and the rest is in-DB).

---

## Notes
- Everything runs in **demo / test mode** (payments, SMS, chatbot are simulated; no real charges or
  sends). To wire a real provider later, inject the matching key via the seams documented in the
  README (`app.payment.provider`, `app.sms.sender`, `app.chat.provider`, `church.geocode.provider`,
  `app.music.provider`).
- The map uses Leaflet + OpenStreetMap (no key). 
- To stop billing-free isn't a concern, but to tear down: `docker compose -f docker-compose.yml -f
  docker-compose.deploy.yml down -v` on the VM and delete the Vercel projects.

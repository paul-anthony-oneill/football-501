# Football 501 — Deployment Plan (MVP)

> Generated from `docs/plans/HOSTING_BRIEF.md` — this is the concrete deployment roadmap.

## Architecture

```
Browser / PWA (Next.js) → Vercel
        │
        │ HTTPS + WSS
        ▼
Spring Boot Backend → Fly.io (Docker container)
        │
        ▼
PostgreSQL → Supabase (already deployed, EU West)
        ▲
        │ batch (GitHub Actions, weekly)
Python Scraper (FBref → DB)
```

## Hosting

| Component | Service | Why |
|---|---|---|
| Database | Supabase | Already deployed; free tier; `pg_trgm` + `unaccent` supported |
| Backend | Fly.io | Docker-based (Java 25); WebSocket support; free tier; EU regions |
| Frontend | Vercel | Native Next.js 16; middleware needed for Supabase Auth; free tier |
| Auth | Supabase Auth (Google OAuth) | Already wired in frontend; built-in Google provider |
| Scraper | GitHub Actions (weekly) | Free; runs Python scripts directly |
| CI/CD | GitHub Actions | Free; PR build + deploy on merge to master |

## Cost

$0/mo on free tiers. Upgrade Supabase to Pro ($25/mo) when backups or >500MB DB needed.

## Environment Variables

### Backend (Fly.io)

| Variable | Purpose |
|---|---|
| `DB_URL` | Supabase pooler JDBC URL (port 6543, `?prepareThreshold=0`) |
| `DB_USERNAME` | Supabase database user |
| `DB_PASSWORD` | Supabase database password |
| `SUPABASE_JWT_ISSUER` | `https://<project>.supabase.co/auth/v1` |
| `SUPABASE_JWT_SECRET` | JWT signing secret from Supabase dashboard |
| `FOOTBALL501_FRONTEND_ORIGIN` | Production frontend URL (for CORS) |
| `PORT` | 8080 |

### Frontend (Vercel)

| Variable | Purpose |
|---|---|
| `NEXT_PUBLIC_SUPABASE_URL` | `https://<project>.supabase.co` |
| `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY` | Supabase publishable (anon) key |

## Go-Live Checklist

- [ ] Create second Supabase project for production, run Flyway migrations
- [ ] Enable Google OAuth in Supabase dashboard (Auth → Providers → Google)
- [ ] Set `NEXT_PUBLIC_SUPABASE_URL` and `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY` in Vercel
- [ ] Set all backend env vars in Fly.io (`fly secrets set`)
- [ ] Create Vercel project and deploy frontend (`vercel --prod`)
- [ ] Create Fly.io app and deploy backend (`fly deploy`)
- [ ] Set `FOOTBALL501_FRONTEND_ORIGIN` to Vercel domain on Fly.io
- [ ] Set `SUPABASE_JWT_ISSUER` and `SUPABASE_JWT_SECRET` on Fly.io
- [ ] Run scraper against production Supabase (manual trigger of GitHub Action)
- [ ] Run `backfill_difficulty_scores.sql` against production Supabase
- [ ] Run `select entity_search_service_populate_entities()` to seed autocomplete
- [ ] Test full gameplay flow on staging
- [ ] Configure DNS (domain → Vercel for frontend, domain → Fly.io for API)
- [ ] Set up monitoring alerts (Supabase Grafana, Fly.io metrics)
- [ ] Enable row-level security on Supabase tables

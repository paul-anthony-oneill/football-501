# How to Deploy & Test

## Local Development

### One-time setup

Add this to your `~/.zshrc` (or `~/.bashrc`):

```bash
export DB_PASSWORD="your-supabase-password"  # get from Supabase dashboard → Database → Connection string
```

Start a new terminal or run `source ~/.zshrc`.

That's it. The URL and username already default to the Supabase dev project in `application.yml`. No other env vars needed for daily work.

### Option B: Local Docker Postgres (offline / schema experiments)

If you need to work offline or test schema changes in isolation:

```bash
docker run -d --name f501-pg \
  -e POSTGRES_DB=football501 \
  -e POSTGRES_USER=football501 \
  -e POSTGRES_PASSWORD=dev_password \
  -p 5432:5432 postgres:15

docker exec f501-pg psql -U football501 -d football501 \
  -c "CREATE EXTENSION IF NOT EXISTS pg_trgm; CREATE EXTENSION IF NOT EXISTS unaccent;"

# Override the defaults to point at localhost
export DB_URL="jdbc:postgresql://localhost:5432/football501"
export DB_USERNAME="football501"
export DB_PASSWORD="dev_password"
```

### Backend

```bash
cd backend
mvn spring-boot:run
# Runs on http://localhost:8080
```

### Frontend

```bash
cd frontend-react
npm run dev
# Runs on http://localhost:3000
# API calls proxy to localhost:8080 automatically
```

### Both together

```bash
# Terminal 1
cd backend && mvn spring-boot:run

# Terminal 2
cd frontend-react && npm run dev

# Open http://localhost:3000
```

---

## Before You Push

```bash
# Backend — compile check
cd backend && mvn compile

# Frontend — build check
cd frontend-react && npm run build
```

Both must pass before pushing. CI runs these same checks on every PR.

---

## Deploying

Deploys happen automatically when you merge to `master`. You do not need to run anything manually.

### What happens on merge

| Component | Deploys to | Trigger |
|---|---|---|
| Frontend | Vercel | Push to master (changes in `frontend-react/`) |
| Backend | Fly.io | Push to master (changes in `backend/`) |

The scraper runs on its own schedule (Monday 3am UTC) — no manual deploy needed.

### To deploy manually (skip CI)

```bash
# Frontend
cd frontend-react
npx vercel --prod

# Backend
cd backend
fly deploy
```

---

## Environment Variables Reference

### Backend (set on Fly.io via `fly secrets set`)

| Variable | Example value | Has default in dev? |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://aws-0-eu-west-1.pooler.supabase.com:6543/postgres?prepareThreshold=0&sslmode=require` | Yes (Supabase dev) |
| `DB_USERNAME` | `postgres.nnzwructancxzexbcvdz` | Yes (Supabase dev) |
| `DB_PASSWORD` | (from Supabase dashboard → Database → Connection string) | No — must be set |
| `SUPABASE_JWT_ISSUER` | `https://nnzwructancxzexbcvdz.supabase.co/auth/v1` | No (JWT auth only active in prod) |
| `SUPABASE_JWT_SECRET` | (from Supabase dashboard → Settings → API → JWT Secret) | No (JWT auth only active in prod) |
| `FOOTBALL501_FRONTEND_ORIGIN` | `https://football501.vercel.app` | No (CORS allows localhost by default) |
| `PORT` | `8080` | Yes (8080) |

### Frontend (set in Vercel dashboard → Environment)

| Variable | Example value |
|---|---|
| `NEXT_PUBLIC_SUPABASE_URL` | `https://nnzwructancxzexbcvdz.supabase.co` |
| `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY` | (from Supabase dashboard → Settings → API → anon public key) |

---

## Testing a PR Before Merging

1. Push your branch
2. Open a PR against `master`
3. CI runs `mvn compile` (backend) and `npm run build` (frontend)
4. If either fails, fix and push again
5. Merge when green

No staging server yet — test locally before pushing.

---

## Running the Scraper

To populate the database with questions and answers:

```bash
cd football-501-scraper
pip install -r requirements.txt

# Set the DB connection
export DATABASE_URL="postgresql://postgres.nnzwructancxzexbcvdz:password@aws-0-eu-west-1.pooler.supabase.com:6543/postgres"

python scrape_current_season.py
python init_questions_v2.py
python populate_answers_v2.py
```

Or trigger it remotely: go to GitHub → Actions → "Scraper — Weekly Data Refresh" → Run workflow.

# Football 501 - Scraping Service

Python microservice for populating Football 501's database with player statistics from FBref.com.

## Overview

This service:
- Scrapes player statistics from FBref using ScraperFC
- Transforms data to Football 501's database schema
- Writes directly to PostgreSQL database
- Runs automated weekly updates
- Provides admin API for manual operations

**Important**: This service runs **independently** from the Spring Boot backend. It only populates the database; the game reads from it.

## Quick Start

### Prerequisites

- Python 3.11+
- PostgreSQL 15+
- pip

### Local Development

1. **Install dependencies**:
   ```bash
   pip install -r requirements.txt
   ```

2. **Configure environment**:
   ```bash
   cp .env.example .env
   # Edit .env with your database credentials
   ```

3. **Create database tables**:
   ```python
   from database import DatabaseManager
   db = DatabaseManager()
   db.create_tables()
   ```

4. **Run the service**:
   ```bash
   uvicorn api.main:app --host 0.0.0.0 --port 8001 --reload
   ```

5. **Check health**:
   ```bash
   curl http://localhost:8001/health
   ```

### Docker Deployment

1. **Start services**:
   ```bash
   docker-compose up -d
   ```

2. **View logs**:
   ```bash
   docker-compose logs -f scraper
   ```

3. **Stop services**:
   ```bash
   docker-compose down
   ```

## Project Structure

```
football-501-scraper/
├── api/
│   ├── __init__.py
│   └── main.py              # FastAPI application
├── database/
│   ├── __init__.py
│   ├── models.py            # SQLAlchemy models
│   └── crud.py              # Database operations
├── jobs/
│   ├── __init__.py
│   ├── populate_questions.py  # Scraping logic
│   └── scheduler.py         # APScheduler jobs
├── scrapers/
│   ├── __init__.py
│   ├── fbref_scraper.py     # ScraperFC wrapper
│   └── data_transformer.py  # Data transformation
├── tests/
│   └── test_scraper.py
├── config.py                # Configuration
├── requirements.txt         # Python dependencies
├── Dockerfile
├── docker-compose.yml
├── .env.example
└── README.md                # This file
```

## API Endpoints

### Health Check

```http
GET /health
```

Returns service health status.

### Populate Single Question

```http
POST /api/admin/scrape-question
Content-Type: application/json

{
  "question_id": 1234
}
```

Populates answers for one question (~10 seconds).

### Populate Season/League

```http
POST /api/admin/scrape-season
Content-Type: application/json

{
  "season": "2023-2024",
  "league": "England Premier League"
}
```

Populates all questions for a season/league (~5-10 minutes).

### Initial Population

```http
POST /api/admin/populate-initial
Content-Type: application/json

{
  "seasons": ["2023-2024", "2022-2023"],
  "leagues": ["England Premier League", "Spain La Liga"]
}
```

Initial database population (~20-30 minutes).

### Trigger Weekly Update

```http
POST /api/admin/trigger-weekly-update
```

Manually trigger weekly update job.

### Get Job History

```http
GET /api/admin/jobs?limit=20
```

Get scraping job history.

### Get Job Details

```http
GET /api/admin/jobs/{job_id}
```

Get specific job details.

## Usage Examples

### Example 1: Populate a Single Question

```python
import requests

response = requests.post(
    "http://localhost:8001/api/admin/scrape-question",
    json={"question_id": 1234}
)

print(response.json())
# Output:
# {
#     "question_id": 1234,
#     "status": "success",
#     "players_added": 27,
#     "duration": 8.3
# }
```

### Example 2: Check Job Status

```python
response = requests.get("http://localhost:8001/api/admin/jobs/42")
print(response.json())
# Output:
# {
#     "job_id": 42,
#     "job_type": "weekly",
#     "status": "success",
#     "rows_updated": 237,
#     "duration": 890.5
# }
```

### Example 3: Programmatic Access

```python
from jobs import QuestionPopulator

# Initialize populator
populator = QuestionPopulator()

# Populate single question
result = populator.populate_single_question(1234)
print(f"Added {result['players_added']} players")

# Update current season
result = populator.update_current_season()
print(f"Updated {result['rows_updated']} rows")
```

## Configuration

Key configuration options in `config.py` or `.env`:

| Variable | Default | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `postgresql://...` | PostgreSQL connection string |
| `FBREF_WAIT_TIME` | `7` | Seconds between FBref requests |
| `WEEKLY_UPDATE_CRON` | `0 3 * * SUN` | Weekly update schedule (Sunday 3 AM) |
| `ENABLE_SCHEDULER` | `true` | Enable automated jobs |
| `CURRENT_SEASON` | `2024-2025` | Current season to update |
| `LOG_LEVEL` | `INFO` | Logging level |

## Scheduled Jobs

### Weekly Update (Automated)

- **When**: Every Sunday at 3:00 AM UTC
- **What**: Updates current season statistics
- **Duration**: ~15 minutes
- **Impact**: Zero (game continues running)

### Manual Triggers

Admin can trigger jobs anytime via API:
- Populate new question (10 seconds)
- Update season/league (5-10 minutes)
- Weekly update (15 minutes)

## Database Schema

### questions
```sql
CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    text TEXT NOT NULL,
    league VARCHAR(100),
    season VARCHAR(20),
    team VARCHAR(100),
    stat_type VARCHAR(50),
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT NOW()
);
```

### answers
```sql
CREATE TABLE answers (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT REFERENCES questions(id),
    player_name VARCHAR(255) NOT NULL,
    statistic_value INTEGER NOT NULL,
    is_valid_darts_score BOOLEAN NOT NULL,
    is_bust BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
```

### scrape_jobs
```sql
CREATE TABLE scrape_jobs (
    id BIGSERIAL PRIMARY KEY,
    job_type VARCHAR(50) NOT NULL,
    season VARCHAR(20),
    league VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    rows_inserted INTEGER DEFAULT 0,
    rows_updated INTEGER DEFAULT 0,
    started_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP
);
```

## Testing

Run tests:
```bash
pytest tests/
```

Run specific test:
```bash
pytest tests/test_scraper.py::test_scrape_player_stats
```

## Troubleshooting

### Issue: "Database connection failed"

**Solution**: Check PostgreSQL is running and credentials are correct:
```bash
psql -U football501 -d football501 -h localhost
```

### Issue: "FBref rate limit exceeded"

**Solution**: Wait 60 seconds and retry. Ensure `FBREF_WAIT_TIME >= 7`.

### Issue: "Invalid league name"

**Solution**: Use full ScraperFC league names:
- ❌ "EPL"
- ✅ "England Premier League"

See `config.py` for league name mapping.

### Issue: "Scheduler not running"

**Solution**: Check `ENABLE_SCHEDULER=true` in `.env`.

## Monitoring

### Logs

View logs:
```bash
# Docker
docker-compose logs -f scraper

# Local
tail -f /var/log/football501-scraper.log
```

### Metrics

If `ENABLE_METRICS=true`, Prometheus metrics available at:
```
http://localhost:9090/metrics
```

## Performance

### Benchmarks

- Single question: ~10 seconds
- Single season/league: ~5-10 minutes
- Initial population (3 leagues × 3 seasons): ~30 minutes
- Weekly update: ~15 minutes

### Optimization

- Batch operations use bulk inserts (> 1000 rows/sec)
- Connection pooling enabled
- Indexes on `question_id` and `player_name`

## Production Deployment

### Requirements

- Python 3.11+
- PostgreSQL 15+
- 1 GB RAM minimum
- 10 GB disk space

### Deployment Steps

1. Build Docker image:
   ```bash
   docker build -t football501-scraper:latest .
   ```

2. Deploy with docker-compose:
   ```bash
   docker-compose -f docker-compose.prod.yml up -d
   ```

3. Verify health:
   ```bash
   curl https://scraper.football501.com/health
   ```

## Contributing

See main repository for contribution guidelines.

## License

See main repository for license information.

## Support

For issues or questions:
- Documentation: `../docs/design/SCRAPING_SERVICE_OPERATIONS.md`
- Issues: GitHub Issues
- Project Log: `../docs/PROJECT_LOG.md`

---

**Version**: 1.0.0
**Last Updated**: 2026-01-18
**Status**: MVP Implementation

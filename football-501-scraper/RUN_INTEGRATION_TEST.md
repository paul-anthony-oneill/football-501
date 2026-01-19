# Running the Integration Test

Follow these steps to validate the complete Football 501 scraping service.

## Prerequisites

- Docker Desktop installed and running
- Python 3.11+ installed
- Terminal/Command Prompt

## Step-by-Step Guide

### Step 1: Start PostgreSQL (2 minutes)

```bash
# Navigate to the scraper directory
cd C:\Users\Paul\Repos\football-501\football-501-scraper

# Start PostgreSQL with docker-compose
docker-compose up -d postgres

# Wait for PostgreSQL to be ready (~10 seconds)
# Check logs
docker-compose logs postgres
```

**Expected Output**: `database system is ready to accept connections`

### Step 2: Initialize Database (1 minute)

```bash
# Option A: Using docker exec (recommended)
docker exec -i football501-postgres psql -U football501 -d football501 < init_db.sql

# Option B: Using psql directly (if installed)
psql -h localhost -U football501 -d football501 -f init_db.sql
# Password: dev_password
```

**Expected Output**:
```
CREATE EXTENSION
CREATE TABLE
CREATE TABLE
CREATE TABLE
INSERT 0 3
```

### Step 3: Install Python Dependencies (2 minutes)

```bash
# Install requirements
pip install -r requirements.txt
```

**Expected Output**: `Successfully installed ScraperFC-4.0.1 ...`

### Step 4: Configure Environment (1 minute)

```bash
# Copy environment template
cp .env.example .env

# The defaults should work for local testing:
# DATABASE_URL=postgresql://football501:dev_password@localhost:5432/football501
```

### Step 5: Run Integration Test (1-2 minutes)

```bash
# Run the test
python test_integration.py
```

**Expected Output**:
```
======================================================================
  FOOTBALL 501 - INTEGRATION TEST
======================================================================

TEST 1: Module Imports
✅ Database modules imported
✅ Scraper modules imported
...

TEST 5: FBref Scraping
ℹ️  Scraping England Premier League 2023-2024...
ℹ️  This will take ~7 seconds (FBref rate limit)...
✅ Scraped 25 players
...

SUMMARY
✅ Imports
✅ Connection
✅ Tables
✅ Question
✅ Scraping
✅ Transformation
✅ Insertion
✅ Query
✅ Job Log

Tests Passed: 9/9
✅ 🎉 ALL TESTS PASSED!
```

---

## Troubleshooting

### Issue: "Database connection failed"

**Solution**: Ensure PostgreSQL is running
```bash
docker-compose up -d postgres
docker-compose ps
```

### Issue: "Module not found: ScraperFC"

**Solution**: Install dependencies
```bash
pip install -r requirements.txt
```

### Issue: "No test question found"

**Solution**: Initialize database
```bash
docker exec -i football501-postgres psql -U football501 -d football501 < init_db.sql
```

### Issue: "FBref scraping failed"

**Possible Causes**:
1. Internet connection issue
2. FBref.com is down (rare)
3. ScraperFC league name incorrect

**Solution**: Check error message, retry after 60 seconds

---

## What the Test Validates

1. ✅ **Module Imports** - All Python packages installed
2. ✅ **Database Connection** - PostgreSQL is accessible
3. ✅ **Database Tables** - questions, answers, scrape_jobs exist
4. ✅ **Question Retrieval** - Can query test question
5. ✅ **FBref Scraping** - Can fetch player data from FBref
6. ✅ **Data Transformation** - Converts to Football 501 schema
7. ✅ **Database Insertion** - Bulk insert works
8. ✅ **Query Verification** - Can retrieve and fuzzy match players
9. ✅ **Job Audit Log** - Scrape jobs are tracked

---

## After Successful Test

You'll have:
- ✅ PostgreSQL database with 3 test questions
- ✅ ~25 Manchester City players in answers table
- ✅ Scrape job audit log entries
- ✅ Confidence the service works!

### Next Steps:

1. **View the data**:
   ```bash
   docker exec -it football501-postgres psql -U football501 -d football501

   SELECT player_name, statistic_value
   FROM answers
   WHERE question_id = 1
   ORDER BY statistic_value DESC
   LIMIT 10;
   ```

2. **Test the REST API**:
   ```bash
   # Start the FastAPI service
   uvicorn api.main:app --host 0.0.0.0 --port 8001

   # In another terminal, test endpoints
   curl http://localhost:8001/health
   curl http://localhost:8001/api/admin/jobs
   ```

3. **Test Spring Boot integration**:
   - Configure Spring Boot to connect to same database
   - Query answers table from Java
   - Test game engine with scraped data

---

## Quick Commands Reference

```bash
# Start PostgreSQL
docker-compose up -d postgres

# Initialize database
docker exec -i football501-postgres psql -U football501 -d football501 < init_db.sql

# Run test
python test_integration.py

# View logs
docker-compose logs -f postgres

# Stop services
docker-compose down

# Clean up (delete data)
docker-compose down -v
```

---

## Success Criteria

✅ **Test passes with 9/9 tests**
✅ **~25 players inserted for Manchester City**
✅ **Can query players by name (fuzzy match)**
✅ **Scrape job logged in database**

If all above are true → **Integration test complete! 🎉**

---

**Need help?** Check `README.md` or `docs/design/SCRAPING_SERVICE_OPERATIONS.md`

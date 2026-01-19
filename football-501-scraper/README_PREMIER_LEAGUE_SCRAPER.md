## Premier League Complete Career Scraper

Comprehensive system to scrape all players who appeared in Premier League (2020-2025) and fetch their complete career histories across all clubs and competitions.

## ✨ Features

- ✅ **Complete Career Data**: All clubs, all competitions, national teams
- ✅ **Error Tracking**: Every player tracked with success/failure status
- ✅ **Automatic Retry**: Failed players can be retried automatically
- ✅ **Progress Monitoring**: Real-time status and detailed reporting
- ✅ **Safe Interruption**: Resume from where you left off
- ✅ **Parallel Scraping**: ~15% faster with thread-safe rate limiting

## 🚀 Quick Start

### 1. Database Migration

Add the error tracking table:

```bash
python migrate_database_v3.py
```

### 2. Extract FBref IDs

Get player IDs from existing league data:

```bash
python update_fbref_ids.py
```

### 3. Run Complete Scrape

Scrape Premier League 2020-2025 (all players, all careers):

```bash
python scrape_premier_league_complete.py
```

**Estimated time**: ~3.5 hours (parallel) or ~4 hours (sequential)

**Progress**: Watch the log file `premier_league_scrape.log`

## 📊 Monitoring & Management

### Check Status

```bash
python monitor_scraping_status.py
```

Shows:
- Overall statistics (success/failed/pending)
- Database contents summary
- Failed players with error details
- Recent scrape jobs
- Recommendations

### Retry Failed Players

```bash
python retry_failed_players.py
```

Automatically retries all failed players (up to 3 attempts).

### Export Failed Players

```bash
python monitor_scraping_status.py --export-failed
```

Creates CSV with all failed players for manual review.

## 🎯 What Gets Scraped

For each player who appeared in Premier League 2020-2025:

```
Player: Erling Haaland
├── Bryne FK (2016-2017) - Norwegian league - 16 apps, 0 goals
├── Molde FK (2017-2019) - Norwegian league - 50 apps, 20 goals
├── Red Bull Salzburg (2019-2020) - Austrian league - 27 apps, 29 goals
├── Red Bull Salzburg (2019-2020) - Champions League - 8 apps, 8 goals
├── Borussia Dortmund (2020-2022) - Bundesliga - 67 apps, 62 goals
├── Borussia Dortmund (2020-2022) - Champions League - 13 apps, 15 goals
├── Manchester City (2022-2025) - Premier League - 98 apps, 91 goals
├── Manchester City (2022-2024) - Champions League - 23 apps, 18 goals
└── Norway National Team (2019-2025) - International - 36 apps, 31 goals
```

**Data includes:**
- All clubs (youth, loans, transfers)
- All competitions (domestic leagues, Champions League, cups, etc.)
- National team appearances
- Appearances, goals, assists per season/team/competition

## 📁 Database Structure

### New Table: `player_scrape_logs`

Tracks every player's scraping status:

```sql
CREATE TABLE player_scrape_logs (
    id BIGINT PRIMARY KEY,
    player_id BIGINT REFERENCES players(id),
    scrape_job_id BIGINT REFERENCES scrape_jobs(id),
    status VARCHAR(20),  -- 'pending', 'success', 'failed', 'skipped'
    attempt_count INT,
    last_attempt_at TIMESTAMP,
    stats_stored INT,
    error_message TEXT,
    error_type VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### Status Values

- **`pending`**: Not yet scraped
- **`success`**: Scraped successfully, data stored
- **`failed`**: Scraping failed (see error_message)
- **`skipped`**: Skipped (e.g., no FBref ID, recently scraped)

## 🔄 Workflow

### Initial Scrape

```
1. Scrape Premier League seasons (2020-2025)
   ├── Get all players who appeared
   └── Extract FBref IDs

2. Create scrape logs for all players
   └── Status: pending

3. Scrape complete careers (parallel)
   ├── Success: status → success
   ├── Error: status → failed, log error
   └── Skip: status → skipped

4. Report summary
   └── Show success/failed counts
```

### Retry Failed

```
1. Get all players with status=failed
2. Filter by attempt_count < 3
3. Re-attempt scraping
4. Update status and attempt_count
```

## ⚙️ Advanced Usage

### Custom Seasons

```bash
python scrape_premier_league_complete.py --seasons 2018-2019 2019-2020
```

### Force Rescrape

```bash
python scrape_premier_league_complete.py --force
```

Rescrapes even if already successful.

### Sequential Mode

```bash
python scrape_premier_league_complete.py --sequential
```

Slower but uses less resources.

### Retry with Custom Attempts

```bash
python retry_failed_players.py --max-attempts 5
```

## 📈 Performance

### Estimated Times (2020-2025, ~2,000 players)

| Mode | Time | Players/hour |
|------|------|--------------|
| Parallel (5 workers) | ~3.5 hours | ~570 |
| Sequential | ~4 hours | ~500 |

### Rate Limiting

- **1 request per 7 seconds** (FBref safe limit)
- **Parallel mode**: Still respects rate limit, overlaps CPU work
- **Thread-safe**: No race conditions or conflicts

## 🛡️ Safety Features

### 1. Error Recovery

Every player tracked individually:
- Failed players don't stop the batch
- Errors logged with full details
- Easy retry without re-scraping successful players

### 2. Resume Support

Interrupted scraping can resume:
```bash
# If interrupted, just run again
python scrape_premier_league_complete.py
# Skips already successful players automatically
```

### 3. Data Integrity

- Atomic database operations
- No duplicate career records (upsert)
- Foreign key constraints enforced

### 4. Monitoring

Real-time logging to:
- Console (stdout)
- Log file (`premier_league_scrape.log`)

## 🐛 Troubleshooting

### "No players have FBref IDs"

**Solution**: Run `python update_fbref_ids.py` first

### HTTP 403 Errors

**Possible causes:**
- FBref rate limiting
- IP blocked temporarily

**Solutions:**
1. Wait 10-15 minutes
2. Retry with slower rate: Edit `config.py` → increase `fbref_wait_time` to 10
3. Use sequential mode: `--sequential`

### Parse Errors

**Cause**: FBref changed HTML structure

**Solution**:
1. Check FBref player page manually
2. Update `_parse_career_row()` in `parallel_player_scraper.py`

### Missing National Team Data

**Cause**: FBref doesn't have all international data

**Solution**: This is expected, some players have incomplete international records on FBref

## 📝 Example Output

```
================================================================================
PREMIER LEAGUE COMPLETE CAREER SCRAPER
================================================================================
Seasons: 2020-2021, 2021-2022, 2022-2023, 2023-2024, 2024-2025
Mode: Parallel
Force rescrape: False

================================================================================
STEP 1: Scraping Premier League Seasons
================================================================================

Scraping 2020-2021...
  ✅ 2020-2021: 523 players, 523 stats

Scraping 2021-2022...
  ✅ 2021-2022: 531 players, 531 stats

...

================================================================================
STEP 2: Identifying All Premier League Players
================================================================================
Found 1,847 unique Premier League players

================================================================================
STEP 3: Extracting FBref Player IDs
================================================================================
Players with FBref IDs: 1,847/1,847

================================================================================
STEP 5: Scraping Complete Player Careers
================================================================================
Estimated time: ~3.5 hours
Starting scrape in 5 seconds... (Ctrl+C to cancel)

--- Batch 1/37 (50 players) ---
[1/50] Phil Foden: 12 stats stored
[2/50] Erling Haaland: 15 stats stored
...

================================================================================
SCRAPE COMPLETE
================================================================================
Total time: 3.4 hours
Players processed: 1,823
Players skipped: 18
Total stats stored: 24,567
Errors: 6

Scrape Status Summary:
  success: 1823
  skipped: 18
  failed: 6

⚠️  6 players failed. Run retry_failed_players.py to retry.
```

## 🔍 Verifying Results

After scraping, verify data:

```python
from database.crud_v2 import DatabaseManager

db = DatabaseManager()

# Check total stats
with db.get_session() as session:
    from database.models_v2 import PlayerCareerStats
    total = session.query(PlayerCareerStats).count()
    print(f"Total career records: {total}")

# Check specific player
stats = db.query_player_stats(player_name="Erling Haaland")
for stat in stats:
    print(f"{stat['team_name']} - {stat['competition_name']} - {stat['season']}: "
          f"{stat['appearances']} apps, {stat['goals']} goals")
```

## 📞 Support

### Check Logs

```bash
# View last 50 lines of log
tail -50 premier_league_scrape.log

# Search for errors
grep "ERROR" premier_league_scrape.log

# Watch live progress
tail -f premier_league_scrape.log
```

### Common Issues

| Issue | Solution |
|-------|----------|
| No FBref IDs | Run `update_fbref_ids.py` |
| HTTP 403 | Wait 15 min, increase `fbref_wait_time` |
| Parse errors | FBref changed, update parser |
| Database errors | Run `migrate_database_v3.py` |
| Incomplete data | Check FBref manually, may be missing |

## 🎯 Next Steps

After scraping completes:

1. **Verify data**: `python monitor_scraping_status.py`
2. **Retry failures**: `python retry_failed_players.py`
3. **Create questions**: Use complete career data to generate diverse questions
4. **Expand scope**: Add more seasons or other leagues

## 📚 Files Reference

| File | Purpose |
|------|---------|
| `scrape_premier_league_complete.py` | Main scraping script |
| `retry_failed_players.py` | Retry failed players |
| `monitor_scraping_status.py` | Status monitoring & reporting |
| `migrate_database_v3.py` | Database migration |
| `update_fbref_ids.py` | Extract FBref player IDs |
| `premier_league_scrape.log` | Detailed scraping log |
| `RATE_LIMITING_EXPLAINED.md` | Rate limiting strategies |

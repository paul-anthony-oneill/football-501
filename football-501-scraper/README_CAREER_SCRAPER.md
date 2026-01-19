# Player Career Scraper - Complete Guide

## ⚡ Quick Start: Parallel Scraping (Recommended)

**5x faster** than sequential scraping! Scrape multiple players concurrently:

```bash
cd football-501-scraper

# Run parallel scraping demo
python example_parallel_scraping.py

# Or scrape all players
python example_parallel_scraping.py --all
```

**Performance:**
- 737 players in ~73 minutes (vs ~86 minutes sequential)
- ~15% speedup from overlapping CPU work with network waiting
- Uses 5 concurrent workers with thread-safe rate limiting
- Still respects 1 request per 7 seconds (safe for FBref)

## Overview

The enhanced player scraper supports scraping **complete career histories** for individual players from their FBref profile pages. This includes:

- **All clubs played for** (Chelsea, Man City, etc.)
- **All competitions** (Premier League, Champions League, etc.)
- **National team appearances** (England, Brazil, etc.)
- **Season-by-season breakdown** of goals, assists, appearances

## Key Features

### 1. Individual Player Career Scraping

Scrapes a single player's complete career from their FBref page:

```python
from scrapers.player_career_scraper import PlayerCareerScraper

scraper = PlayerCareerScraper()

result = scraper.scrape_full_player_career(
    player_id=123,           # Database player ID
    fbref_id="1f44ac21",    # FBref player ID (from URL)
    force_rescrape=False    # Skip if recently scraped
)

print(f"Stored {result['stats_stored']} career records")
```

### 2. Batch Player Scraping (Sequential)

Scrape multiple players one at a time:

```python
# Scrape specific players
player_ids = [1, 2, 3, 4, 5]
result = scraper.scrape_multiple_player_careers(player_ids)

# Or scrape ALL players with FBref IDs
result = scraper.scrape_all_stored_players(
    force_rescrape=False,
    max_players=10  # Optional: limit for testing
)

print(f"Processed: {result['players_processed']}")
print(f"Stats stored: {result['total_stats_stored']}")
```

**⚠️ Note:** This is sequential (one at a time). For slightly faster scraping, use the parallel scraper below.

### 3. Pipeline Parallel Scraping (~15% Faster) ⚡

Scrape multiple players using thread pools with overlapping work:

```python
from scrapers.parallel_player_scraper import ParallelPlayerScraper

# Initialize with 5 concurrent workers
scraper = ParallelPlayerScraper(max_workers=5)

# Scrape players in parallel
player_ids = [1, 2, 3, 4, 5]
result = scraper.scrape_players_parallel(
    player_ids=player_ids,
    force_rescrape=False
)

print(f"Processed: {result['players_processed']}")
print(f"Total stats: {result['total_stats_stored']}")
print(f"Errors: {result['errors']}")
```

**Performance comparison:**
- 10 players: Sequential ~70s, Pipeline Parallel ~60s (~15% faster)
- 737 players: Sequential ~86 min, Pipeline Parallel ~73 min (~15% faster)

**How it works:**
- Still makes only **1 request per 7 seconds** (safe rate limiting)
- But **overlaps CPU work** (HTML parsing, database operations) with network waiting
- Multiple threads work in pipeline: fetch → parse → store
- Better resource utilization = modest speedup

**Thread safety:**
- Rate limiting is thread-safe (uses locks to ensure 1 req/7s)
- Database operations are isolated per thread
- No race conditions or conflicts

## Prerequisites

### Required: FBref Player IDs

Players must have their `fbref_id` field populated before scraping. The FBref ID is extracted from player URLs:

```
https://fbref.com/en/players/1f44ac21/Erling-Haaland
                              ^^^^^^^^
                              FBref ID
```

### Getting FBref IDs

**Option 1: Extract from existing scraped data**

Run the included utility script:

```bash
cd football-501-scraper
python update_fbref_ids.py
```

This extracts FBref IDs from previously scraped league data and updates the database.

**Option 2: Manual update**

Update a player directly in the database:

```python
from database.crud_v2 import DatabaseManager

db = DatabaseManager()
with db.get_session() as session:
    from database.models_v2 import Player

    player = session.query(Player).filter_by(name="Erling Haaland").first()
    player.fbref_id = "1f44ac21"
    session.commit()
```

### Common FBref IDs

For testing, here are some Premier League player IDs:

- **Erling Haaland**: `1f44ac21`
- **Kevin De Bruyne**: `b8a3ad0c`
- **Phil Foden**: `ed1e53f3`
- **Mohamed Salah**: `e342ad68`
- **Harry Kane**: `21a66f6a`

## Complete Workflow

### Step 1: Scrape League Stats (Existing)

This gives you a player list:

```python
from scrapers.player_career_scraper import PlayerCareerScraper

scraper = PlayerCareerScraper()

# Scrape Premier League 2023-2024
scraper.scrape_league_players(
    league="Premier League",
    season="2023-2024",
    min_appearances=5
)
```

### Step 2: Extract FBref IDs

```bash
python update_fbref_ids.py
```

### Step 3: Scrape Full Career Histories

```python
# Test with a single player first
scraper.scrape_full_player_career(
    player_id=1,
    fbref_id="1f44ac21",
    force_rescrape=True
)

# Then batch scrape all players
scraper.scrape_all_stored_players(max_players=10)
```

### Step 4: Query Comprehensive Data

```python
from database.crud_v2 import DatabaseManager

db = DatabaseManager()

# Query all career stats for a player
stats = db.query_player_stats(team_name="Manchester City")

for stat in stats:
    print(f"{stat['player_name']} - {stat['team_name']} - {stat['season']}")
```

## Data Structure

The scraper populates the following database tables:

### `players`
- Player name, nationality, FBref ID

### `teams`
- All clubs (Manchester City, Chelsea, etc.)
- National teams (England, Brazil, etc.)

### `competitions`
- Domestic leagues (Premier League, La Liga, etc.)
- Continental (Champions League, Europa League)
- International (World Cup, Euros, etc.)

### `player_career_stats`
- Complete season-by-season records
- Links: player → team → competition → season
- Stats: appearances, goals, assists, clean sheets

## Rate Limiting

**Critical:** FBref enforces rate limiting. The scraper respects this:

- **Default wait time**: 7 seconds between requests
- **Configurable** in `config.py`: `fbref_wait_time`
- **Automatic**: Built into `scrape_full_player_career()`

**Estimated times:**
- 1 player: ~7 seconds
- 10 players: ~70 seconds (~1.2 minutes)
- 100 players: ~700 seconds (~11.7 minutes)
- 737 players (current DB): ~5,159 seconds (~86 minutes)

## Error Handling

The scraper handles common errors gracefully:

### Missing FBref ID
```python
# Skips player automatically
# Logs: "Player {name} has no FBref ID, skipping"
```

### 403 Forbidden / Rate Limiting
```python
# Raises RequestException
# You should: increase wait time or retry later
```

### Parse Errors
```python
# Logs warning and continues
# Stats stored: 0 for that player
```

## Testing

### Quick Test (Single Player)

```bash
cd football-501-scraper
python test_player_career.py
```

### Full Example Workflow

```bash
python example_full_career_scrape.py
```

This demonstrates:
1. Checking database state
2. Scraping a single player
3. Viewing results
4. Batch scraping guidance

## Advanced Usage

### Custom Rate Limiting

```python
from scrapers.fbref_scraper import FBrefScraper

# Slower scraping (safer)
scraper = PlayerCareerScraper()
scraper.scraper.wait_time = 10  # 10 seconds between requests
```

### Rescrape Recently Updated Players

```python
# Default: skips players scraped in last 30 days
# To force rescrape:
scraper.scrape_full_player_career(
    player_id=1,
    fbref_id="1f44ac21",
    force_rescrape=True  # Ignore last_scraped_at
)
```

### Query Multi-Club Players

```python
from database.crud_v2 import DatabaseManager
from sqlalchemy import func

db = DatabaseManager()

with db.get_session() as session:
    from database.models_v2 import Player, PlayerCareerStats

    # Find players who played for multiple teams
    multi_team = session.query(
        Player.name,
        func.count(func.distinct(PlayerCareerStats.team_id)).label('team_count')
    ).join(PlayerCareerStats).group_by(Player.id, Player.name).having(
        func.count(func.distinct(PlayerCareerStats.team_id)) > 1
    ).all()

    for name, count in multi_team:
        print(f"{name}: {count} teams")
```

## Troubleshooting

### "No FBref IDs found"

**Solution:** Run `python update_fbref_ids.py` first

### "403 Forbidden" errors

**Solutions:**
1. Increase wait time: `scraper.scraper.wait_time = 10`
2. Try again later (FBref may have temporary blocks)
3. Check your IP hasn't been rate-limited

### "No tables found" or "0 stats stored"

**Possible causes:**
1. FBref changed their HTML structure
2. Player page doesn't exist
3. Player has no career stats (youth player)

**Solution:** Check the player URL manually in a browser

### BeautifulSoup import error

**Solution:** Install required packages:

```bash
pip install beautifulsoup4 requests
```

## Next Steps

After scraping complete career data, you can:

1. **Create diverse questions** spanning multiple competitions
2. **Add national team questions** (World Cup appearances, etc.)
3. **Query player transfers** (teams played for)
4. **Build career milestone questions** (100+ appearances for X club)

## API Reference

### `scrape_full_player_career(player_id, fbref_id, force_rescrape=False)`

Scrape a single player's complete career.

**Returns:** `Dict[str, int]` with `stats_stored`, `skipped`, `player_name`

### `scrape_multiple_player_careers(player_ids, force_rescrape=False)`

Batch scrape multiple players.

**Returns:** `Dict[str, int]` with `players_processed`, `players_skipped`, `total_stats_stored`, `errors`

### `scrape_all_stored_players(force_rescrape=False, max_players=None)`

Scrape all players with FBref IDs in the database.

**Returns:** Same as `scrape_multiple_player_careers`

## Support

For issues or questions:
1. Check the logs (set `logging.DEBUG` for verbose output)
2. Verify FBref page structure hasn't changed
3. Test with a known working FBref ID (e.g., Haaland: `1f44ac21`)

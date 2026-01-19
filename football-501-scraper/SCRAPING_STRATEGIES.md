# Scraping Strategies Comparison

## Overview

There are **two main approaches** to populate comprehensive player career data:

1. **Individual Player Pages** (player-by-player)
2. **League-Based Bulk Scraping** (season-by-season)

## Comparison Table

| Feature | Individual Pages | League-Based |
|---------|-----------------|--------------|
| **Speed (737 players)** | ~86 min sequential, ~17 min parallel (5x) | ~10-15 min total |
| **Completeness** | ✅ Full career, all clubs, national team | ⚠️ Only players in scraped leagues |
| **Data breadth** | ✅ All competitions player appeared in | ⚠️ Limited to scraped competitions |
| **National teams** | ✅ Included automatically | ❌ Not included |
| **Historical data** | ✅ Complete career history | ⚠️ Only scraped seasons |
| **Requests needed** | 737 (one per player) | ~50-100 (leagues × seasons × stat types) |
| **Best for** | Complete player profiles | Bulk population, specific leagues |

## Strategy 1: Individual Player Pages (Current Implementation)

### How It Works

Scrapes each player's FBref profile page (e.g., `fbref.com/en/players/1f44ac21/Erling-Haaland`).

### Pros
- ✅ **Complete career history** - All clubs, all competitions, all seasons
- ✅ **National team data** - Automatically included
- ✅ **Transfers tracked** - Shows full club history
- ✅ **One request per player** - Predictable

### Cons
- ❌ Slower for bulk scraping (737 players = 737 requests)
- ❌ Requires FBref IDs upfront

### Performance

**Sequential (existing scraper):**
```
737 players × 7 seconds = 5,159 seconds (~86 minutes)
```

**Parallel (new scraper):**
```
737 players × 7 seconds / 5 workers = 1,032 seconds (~17 minutes)
```

### Use Cases
- Getting complete player profiles
- Tracking player careers across multiple clubs
- Including national team appearances
- Building questions that span multiple competitions

### Example Code

```python
from scrapers.parallel_player_scraper import ParallelPlayerScraper

scraper = ParallelPlayerScraper(max_workers=5)
result = scraper.scrape_players_parallel(player_ids, force_rescrape=False)
```

## Strategy 2: League-Based Bulk Scraping

### How It Works

Scrapes league tables multiple times for different seasons/competitions:
- Premier League 2020-2021, 2021-2022, 2022-2023, 2023-2024
- La Liga 2020-2021, 2021-2022, ...
- Bundesliga 2020-2021, ...
- Champions League 2020-2021, ...

### Pros
- ✅ **Fast** - Get hundreds of players per request
- ✅ **Efficient** - Fewer total requests
- ✅ **Historical seasons** - Easy to add previous seasons

### Cons
- ❌ **Incomplete careers** - Only get stats for scraped leagues
- ❌ **No national teams** - Domestic/club competitions only
- ❌ **Transfer gaps** - Miss clubs not in scraped leagues
- ❌ **More requests for coverage** - Need many league×season combinations

### Performance

**For comprehensive coverage:**
```
5 major leagues × 5 seasons × 2 stat types = 50 requests
50 × 7 seconds = 350 seconds (~6 minutes)

But: Only covers those 5 leagues. No national teams.
```

### Use Cases
- Quick database population for specific leagues
- Getting current season data across multiple leagues
- Building league-specific questions
- Backfilling historical seasons

### Example Code

```python
from scrapers.player_career_scraper import PlayerCareerScraper

scraper = PlayerCareerScraper()

# Scrape multiple seasons for multiple leagues
leagues = ["Premier League", "La Liga", "Bundesliga", "Serie A", "Ligue 1"]
seasons = ["2020-2021", "2021-2022", "2022-2023", "2023-2024"]

for league in leagues:
    for season in seasons:
        scraper.scrape_league_players(league=league, season=season)
```

## Hybrid Strategy (Recommended)

**Combine both approaches** for best results:

### Phase 1: Bulk League Scraping (Fast)
1. Scrape major leagues for recent seasons (5 leagues × 5 seasons = 25 requests)
2. This populates the database with ~2,000+ players quickly
3. Get FBref IDs automatically from league data

### Phase 2: Complete Career Scraping (Thorough)
1. Run parallel player scraper on high-value players
2. Focus on players with 50+ appearances (likely to be in questions)
3. Get complete careers including national teams

### Implementation

```python
# Phase 1: Quick bulk population
from scrapers.player_career_scraper import PlayerCareerScraper

scraper = PlayerCareerScraper()

leagues = ["Premier League", "La Liga", "Bundesliga", "Serie A", "Ligue 1"]
seasons = ["2021-2022", "2022-2023", "2023-2024"]

for league in leagues:
    for season in seasons:
        print(f"Scraping {league} {season}...")
        scraper.scrape_league_players(league, season, min_appearances=5)

# Phase 2: Complete career for active players
from scrapers.parallel_player_scraper import ParallelPlayerScraper
from database.crud_v2 import DatabaseManager

db = DatabaseManager()
with db.get_session() as session:
    from database.models_v2 import Player, PlayerCareerStats
    from sqlalchemy import func

    # Get players with most appearances (high value)
    top_players = session.query(Player.id).join(PlayerCareerStats).group_by(
        Player.id
    ).having(
        func.sum(PlayerCareerStats.appearances) >= 50
    ).all()

    player_ids = [p.id for p in top_players]

print(f"Scraping complete careers for {len(player_ids)} high-value players...")

parallel_scraper = ParallelPlayerScraper(max_workers=5)
result = parallel_scraper.scrape_players_parallel(player_ids)

print(f"Complete! {result['total_stats_stored']} stats stored")
```

## Recommendations by Use Case

### For MVP/Testing
**Use:** League-based scraping
- Fast population
- Good enough for initial questions
- ~10 minutes total time

### For Production/Complete Data
**Use:** Hybrid approach
- Bulk scrape major leagues first
- Then parallel scrape player pages for completeness
- Best of both worlds

### For Specific Player Deep Dives
**Use:** Individual player scraping
- Complete career history
- All competitions
- National teams included

## Performance Summary

### Time to populate 737 players with complete careers:

| Method | Time | Requests |
|--------|------|----------|
| Sequential individual | ~86 min | 737 |
| Parallel (5 workers) | ~17 min | 737 |
| League bulk (5 leagues × 5 seasons) | ~6 min | 25 |
| **Hybrid (bulk + parallel top 200)** | **~12 min** | **25 + 200** |

## Conclusion

**Best approach:** Use parallel player scraping for complete career data.

- **5x faster** than sequential (17 min vs 86 min)
- **Complete data** including all clubs and national teams
- **Simple to use** - just run the parallel scraper

If you only need league-specific data quickly, use league-based scraping. But for comprehensive player profiles across all competitions, parallel player scraping is the way to go.

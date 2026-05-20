"""
Scrape Current Season Statistics
---------------------------------
Fetches player statistics from FBref using ScraperFC and updates
the 'players' table (career_stats JSONB) and 'teams' table.

After this script completes, run:
    python init_questions_v2.py
    python populate_answers_v2.py
"""

import os
import sys
import pandas as pd
from datetime import datetime
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from config import settings
from database.models_v4 import Player, Team

# FBref uses abbreviated names. Map them to popularity ranks for difficulty scoring.
# Teams not in this map get rank 10 (difficulty = hard).
TEAM_POPULARITY_RANKS = {
    "Manchester City": 1,
    "Arsenal": 2,
    "Liverpool": 3,
    "Chelsea": 4,
    "Manchester Utd": 5,
    "Tottenham": 6,
    "Newcastle Utd": 7,
    "Aston Villa": 8,
    "West Ham": 9,
    "Brighton": 10,
    "Brentford": 11,
    "Fulham": 12,
    "Wolves": 13,
    "Crystal Palace": 14,
    "Everton": 15,
    "Nott'ham Forest": 16,
    "Bournemouth": 17,
    "Leicester City": 18,
    "Ipswich Town": 19,
    "Southampton": 20,
}


def normalize_name(name: str) -> str:
    return "".join(c for c in name.lower() if c.isalnum())


def upsert_teams(session, squad_names: set) -> None:
    """Create Team entries for any squad name not already in the database."""
    existing = {t.name for t in session.query(Team.name).all()}
    new_teams = squad_names - existing

    for name in sorted(new_teams):
        rank = TEAM_POPULARITY_RANKS.get(name, 10)
        team = Team(
            name=name,
            normalized_name=normalize_name(name),
            team_type="club",
            country="England",
            popularity_rank=rank,
        )
        session.add(team)

    if new_teams:
        session.commit()
        print(f"Added {len(new_teams)} teams to database: {sorted(new_teams)}")
    else:
        print("All teams already exist in database.")


def run_scrape():
    from ScraperFC.fbref import FBref
    import undetected_chromedriver as uc
    import time


    print(f"Starting scrape for {settings.current_season}...")
    print("Launching Chrome (undetected) to bypass Cloudflare...")

    driver = uc.Chrome(headless=False)

    engine = create_engine(settings.database_url)
    Session = sessionmaker(bind=engine)
    session = Session()

    fb = FBref(wait_time=settings.fbref_wait_time)

    def _wait_for_cloudflare(url: str) -> None:
        driver.get(url)
        for _ in range(30):
            if "Just a moment" not in driver.title:
                break
            time.sleep(1)
        time.sleep(fb.wait_time)

    def chrome_get(url: str):
        class R: pass
        _wait_for_cloudflare(url)
        r = R()
        r.status_code = 200
        r.content = driver.page_source.encode("utf-8")
        return r

    # Patch all HTTP methods so every FBref request goes through undetected Chrome
    fb._get = chrome_get
    fb._driver_init = lambda: None        # prevent ScraperFC opening its own Chrome
    fb.driver = driver                    # hand our driver to ScraperFC
    fb._driver_get = _wait_for_cloudflare # ScraperFC's driver.get goes through ours
    fb._driver_close = lambda: None       # don't let ScraperFC close our driver


    # ScraperFC v3 requires the short league code, not the full name
    leagues = [
        ("EPL", "EPL"),
    ]

    season = settings.current_season

    for league_name, league_slug in leagues:
        print(f"\n--- Scraping {league_name} ({season}) ---")
        try:
            result = fb.scrape_stats(season, league_name, "standard")

            # Handle varying return formats across ScraperFC versions
            if isinstance(result, dict):
                player_df = result.get("player")
            elif isinstance(result, tuple) and len(result) == 3:
                _, _, player_df = result
            else:
                player_df = result

            if not isinstance(player_df, pd.DataFrame):
                player_df = pd.DataFrame(player_df)

            # Flatten multi-level columns produced by FBref HTML tables
            if isinstance(player_df.columns, pd.MultiIndex):
                player_df.columns = [
                    f"{col[0]}_{col[1]}" if not col[0].startswith("Unnamed") else col[1]
                    for col in player_df.columns.values
                ]

            # Drop repeated header rows FBref injects mid-table
            player_df = player_df[player_df["Player"] != "Player"].reset_index(drop=True)

            print(f"Found {len(player_df)} players. Processing...")

            # Upsert teams before processing players so foreign references are clean
            squad_names = {str(r) for r in player_df["Squad"].dropna().unique()}
            upsert_teams(session, squad_names)

            players_updated = 0
            for _, row in player_df.iterrows():
                name = str(row["Player"]).strip()
                if not name:
                    continue

                squad = str(row["Squad"]).strip()
                nation = str(row.get("Nation", "")).strip()

                # Column names vary with FBref layout; try both flattened forms
                apps = int(row.get("Playing Time_MP", row.get("Playing_Time_MP", 0)) or 0)
                goals = int(row.get("Performance_Gls", 0) or 0)
                assists = int(row.get("Performance_Ast", 0) or 0)
                minutes = int(row.get("Playing Time_Min", row.get("Playing_Time_Min", 0)) or 0)

                if apps == 0:
                    continue

                norm_name = normalize_name(name)

                new_stat = {
                    "season": season,
                    "team": squad,
                    "competition": "Premier League",
                    "appearances": apps,
                    "goals": goals,
                    "assists": assists,
                    "minutes_played": minutes,
                    "last_updated": datetime.utcnow().isoformat(),
                }

                player = session.query(Player).filter_by(normalized_name=norm_name).first()

                if player:
                    stats = [
                        s for s in list(player.career_stats)
                        if not (s.get("season") == season and s.get("team") == squad)
                    ]
                    stats.append(new_stat)
                    player.career_stats = stats
                    player.last_scraped_at = datetime.utcnow()
                else:
                    player = Player(
                        fbref_id=f"gen_{norm_name}",
                        name=name,
                        normalized_name=norm_name,
                        nationality=nation,
                        career_stats=[new_stat],
                        last_scraped_at=datetime.utcnow(),
                    )
                    session.add(player)

                players_updated += 1
                if players_updated % 50 == 0:
                    session.commit()
                    print(f"  Processed {players_updated} players...")

            session.commit()
            print(f"Finished {league_name}: {players_updated} players updated.")

        except Exception as e:
            print(f"Error scraping {league_name}: {e}")
            session.rollback()
            driver.quit()
            raise

    driver.quit()

    print("\nScrape complete.")
    print("Next steps:")
    print("  python init_questions_v2.py")
    print("  python populate_answers_v2.py")
    print("  python verify_2526.py")


if __name__ == "__main__":
    run_scrape()

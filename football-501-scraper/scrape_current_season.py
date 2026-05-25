"""
Scrape Current Season Statistics
---------------------------------
Fetches player statistics from FBref using ScraperFC and writes directly
to ``player_season_stints`` (V6 schema), bypassing the legacy
``players.career_stats`` JSONB column.

After this script completes the stints table is the source of truth.
The legacy JSONB column is kept until V9 but is **no longer written to** by
this script — existing data in career_stats persists until V9 drops it.

After this script completes, run:
    python init_questions_v2.py     (if creating new question rows)
    python populate_answers_v2.py   (manual answer population, pre-materializer)
"""

import os
import sys
import logging
from datetime import datetime
from typing import Optional, Tuple

import pandas as pd
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from config import settings
from database.models_v6 import (
    Player,
    Team,
    Competition,
    Season,
    PlayerSeasonStint,
    PlayerExternalId,
    TeamExternalId,
    ScrapeJob,
    ScrapeRunLog,
)
from backfill_season_stints import (
    normalise_season_label,
    parse_start_year,
    parse_end_year,
    get_or_create_season,
    ensure_player_external_id,
    ensure_team_external_id,
)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# FBref team-name → DB name mapping
# ---------------------------------------------------------------------------
# FBref uses abbreviated squad names; the DB uses the full display name.
FBREF_TEAM_NAME_MAP = {
    "Manchester City":  "Manchester City",
    "Arsenal":          "Arsenal",
    "Liverpool":        "Liverpool",
    "Chelsea":          "Chelsea",
    "Manchester Utd":   "Manchester United",
    "Tottenham":        "Tottenham Hotspur",
    "Newcastle Utd":    "Newcastle United",
    "Aston Villa":      "Aston Villa",
    "West Ham":         "West Ham United",
    "Brighton":         "Brighton & Hove Albion",
    "Brentford":        "Brentford",
    "Fulham":           "Fulham",
    "Wolves":           "Wolverhampton Wanderers",
    "Crystal Palace":   "Crystal Palace",
    "Everton":          "Everton",
    "Nott'ham Forest":  "Nottingham Forest",
    "Bournemouth":      "AFC Bournemouth",
    "Leicester City":   "Leicester City",
    "Ipswich Town":     "Ipswich Town",
    "Southampton":      "Southampton",
}

# FBref competition name → DB competition name
FBREF_COMP_NAME_MAP = {
    "EPL":             "Premier League",
    "La Liga":         "La Liga",
    "Serie A":         "Serie A",
    "Bundesliga":      "Bundesliga",
    "Ligue 1":         "Ligue 1",
    "UCL":             "Champions League",
    "UEL":             "Europa League",
    "UECL":            "UEFA Conference League",
}

TEAM_POPULARITY_RANKS = {
    "Manchester City":        1,
    "Arsenal":                2,
    "Liverpool":              3,
    "Chelsea":                4,
    "Manchester United":      5,
    "Tottenham Hotspur":      6,
    "Newcastle United":       7,
    "Aston Villa":            8,
    "West Ham United":        9,
    "Brighton & Hove Albion": 10,
    "Brentford":              11,
    "Fulham":                 12,
    "Wolverhampton Wanderers":13,
    "Crystal Palace":         14,
    "Everton":                15,
    "Nottingham Forest":      16,
    "AFC Bournemouth":        17,
    "Leicester City":         18,
    "Ipswich Town":           19,
    "Southampton":            20,
}


def normalize_name(name: str) -> str:
    return "".join(c for c in name.lower() if c.isalnum())


# ---------------------------------------------------------------------------
# Team upsert
# ---------------------------------------------------------------------------
def upsert_team(session, fbref_squad_name: str) -> Optional[Team]:
    """
    Find or create a Team row for the given FBref squad name.
    Returns the Team or None if the name cannot be resolved.
    """
    canonical_name = FBREF_TEAM_NAME_MAP.get(fbref_squad_name, fbref_squad_name)

    team = session.query(Team).filter_by(name=canonical_name).first()
    if team is None:
        rank = TEAM_POPULARITY_RANKS.get(canonical_name, 99)
        team = Team(
            name=canonical_name,
            normalized_name=normalize_name(canonical_name),
            team_type="club",
            country="England",   # default; update for non-EPL leagues
            popularity_rank=rank,
        )
        session.add(team)
        session.flush()
        log.info("  Created team: %s", canonical_name)

    return team


# ---------------------------------------------------------------------------
# Player upsert
# ---------------------------------------------------------------------------
def upsert_player(session, fbref_id: str, name: str, nationality: str) -> Player:
    """Find or create a Player row; update last_scraped_at."""
    norm = normalize_name(name)
    player = session.query(Player).filter_by(fbref_id=fbref_id).first()
    if player is None:
        # Also check by normalized name (for scraper-generated IDs)
        player = session.query(Player).filter_by(normalized_name=norm).first()

    if player is None:
        player = Player(
            fbref_id=fbref_id or f"gen_{norm}",
            name=name,
            normalized_name=norm,
            nationality=nationality,
            career_stats=[],      # kept for backward compat until V9
            last_scraped_at=datetime.utcnow(),
        )
        session.add(player)
        session.flush()
    else:
        player.last_scraped_at = datetime.utcnow()
        if player.name != name:
            player.name = name    # update display name if changed

    ensure_player_external_id(session, player)
    return player


# ---------------------------------------------------------------------------
# Stint upsert
# ---------------------------------------------------------------------------
def upsert_stint(
    session,
    player: Player,
    season: Season,
    team: Team,
    competition: Competition,
    *,
    appearances: int,
    starts: int = 0,
    sub_appearances: int = 0,
    minutes: int = 0,
    goals: int = 0,
    assists: int = 0,
    yellow_cards: int = 0,
    red_cards: int = 0,
    clean_sheets: int = 0,
    goals_conceded: int = 0,
    is_goalkeeper: bool = False,
) -> Tuple[PlayerSeasonStint, bool]:
    """
    Upsert a ``player_season_stints`` row.

    Returns (stint, created_flag).
    """
    existing = (
        session.query(PlayerSeasonStint)
        .filter_by(
            player_id=player.id,
            season_id=season.id,
            team_id=team.id,
            competition_id=competition.id,
        )
        .first()
    )

    now = datetime.utcnow()

    if existing is None:
        stint = PlayerSeasonStint(
            player_id=player.id,
            season_id=season.id,
            team_id=team.id,
            competition_id=competition.id,
            appearances=appearances,
            starts=starts,
            sub_appearances=sub_appearances,
            minutes=minutes,
            goals=goals,
            assists=assists,
            yellow_cards=yellow_cards,
            red_cards=red_cards,
            clean_sheets=clean_sheets,
            goals_conceded=goals_conceded,
            is_goalkeeper=is_goalkeeper,
            source="fbref",
            source_scraped_at=now,
        )
        session.add(stint)
        return stint, True
    else:
        existing.appearances = appearances
        existing.starts = starts
        existing.sub_appearances = sub_appearances
        existing.minutes = minutes
        existing.goals = goals
        existing.assists = assists
        existing.yellow_cards = yellow_cards
        existing.red_cards = red_cards
        existing.clean_sheets = clean_sheets
        existing.goals_conceded = goals_conceded
        existing.is_goalkeeper = is_goalkeeper
        existing.source_scraped_at = now
        existing.updated_at = now
        return existing, False


# ---------------------------------------------------------------------------
# Main scrape entry-point
# ---------------------------------------------------------------------------
def run_scrape() -> None:
    from ScraperFC.fbref import FBref
    import undetected_chromedriver as uc
    import time

    log.info("Starting scrape for %s…", settings.current_season)
    log.info("Launching Chrome (undetected) to bypass Cloudflare…")

    driver = uc.Chrome(headless=False)

    engine = create_engine(settings.database_url)
    Session = sessionmaker(bind=engine)
    session = Session()

    season_label = normalise_season_label(settings.current_season)

    # Record the scrape job
    job = ScrapeJob(
        job_type="weekly_update",
        season=season_label,
        status="running",
        started_at=datetime.utcnow(),
    )
    session.add(job)
    session.flush()

    fb = FBref(wait_time=settings.fbref_wait_time)

    def _wait_for_cloudflare(url: str) -> None:
        driver.get(url)
        for _ in range(30):
            if "Just a moment" not in driver.title:
                break
            time.sleep(1)
        time.sleep(fb.wait_time)

    def chrome_get(url: str):
        class R:
            pass

        _wait_for_cloudflare(url)
        r = R()
        r.status_code = 200
        r.content = driver.page_source.encode("utf-8")
        return r

    # Patch FBref to use our undetected-chromedriver instance
    fb._get = chrome_get
    fb._driver_init = lambda: None
    fb.driver = driver
    fb._driver_get = _wait_for_cloudflare
    fb._driver_close = lambda: None

    # Get or create the current season
    season_row = get_or_create_season(session, settings.current_season)
    session.commit()

    # Leagues to scrape (FBref code → competition DB name)
    leagues = [("EPL", "Premier League")]

    players_created = 0
    players_updated = 0
    players_failed = 0

    for league_fbref_code, comp_db_name in leagues:
        log.info("\n--- Scraping %s (%s) ---", league_fbref_code, season_label)

        competition_row = session.query(Competition).filter_by(name=comp_db_name).first()
        if competition_row is None:
            log.error("Competition not found in DB: %s — skipping.", comp_db_name)
            session.add(ScrapeRunLog(
                job_id=job.id, level="ERROR",
                message=f"Competition not found: {comp_db_name}",
                context={"league": league_fbref_code},
            ))
            continue

        job.competition_id = competition_row.id

        try:
            result = fb.scrape_stats(settings.current_season, league_fbref_code, "standard")

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

            log.info("Found %d player rows. Processing…", len(player_df))

            for _, row in player_df.iterrows():
                name = str(row.get("Player", "")).strip()
                if not name:
                    continue

                fbref_squad = str(row.get("Squad", "")).strip()
                nation = str(row.get("Nation", "")).strip()

                # Stat extraction (handles both flat and multi-level column names)
                def col(key1, key2=None, default=0):
                    for k in ([f"{key1}_{key2}", key1] if key2 else [key1]):
                        if k in row.index:
                            try:
                                return int(float(row[k])) if row[k] else default
                            except (ValueError, TypeError):
                                pass
                    return default

                apps = col("Playing Time", "MP") or col("Playing_Time", "MP")
                starts = col("Playing Time", "Starts") or col("Playing_Time", "Starts")
                mins = col("Playing Time", "Min") or col("Playing_Time", "Min")
                goals = col("Performance", "Gls")
                assists = col("Performance", "Ast")
                yellows = col("Performance", "CrdY")
                reds = col("Performance", "CrdR")

                if apps == 0:
                    continue  # Skip players with no appearances

                try:
                    team_row = upsert_team(session, fbref_squad)
                    if team_row is None:
                        log.warning("Could not resolve team for: %s", fbref_squad)
                        players_failed += 1
                        continue

                    ensure_team_external_id(session, team_row)

                    # FBref uses player-id in the URL; we fall back to a generated ID.
                    fbref_player_id = f"gen_{normalize_name(name)}"
                    player_row = upsert_player(session, fbref_player_id, name, nation)

                    _, created = upsert_stint(
                        session,
                        player=player_row,
                        season=season_row,
                        team=team_row,
                        competition=competition_row,
                        appearances=apps,
                        starts=starts,
                        minutes=mins,
                        goals=goals,
                        assists=assists,
                        yellow_cards=yellows,
                        red_cards=reds,
                    )

                    if created:
                        players_created += 1
                    else:
                        players_updated += 1

                    if (players_created + players_updated) % 50 == 0:
                        session.commit()
                        log.info("  Processed %d players…",
                                 players_created + players_updated)

                except Exception as exc:
                    log.exception("  Error for player %s: %s", name, exc)
                    session.add(ScrapeRunLog(
                        job_id=job.id, level="ERROR",
                        message=f"Error processing player {name!r}: {exc}",
                        context={"player": name, "squad": fbref_squad},
                    ))
                    players_failed += 1

            session.commit()
            log.info("Finished %s: %d created, %d updated, %d failed.",
                     league_fbref_code, players_created, players_updated, players_failed)

        except Exception as exc:
            log.exception("Error scraping %s: %s", league_fbref_code, exc)
            session.rollback()
            driver.quit()
            raise

    driver.quit()

    job.status = "success" if players_failed == 0 else "partial"
    job.players_scraped = players_created + players_updated
    job.players_failed = players_failed
    job.completed_at = datetime.utcnow()
    session.commit()

    log.info("\nScrape complete.")
    log.info("Next steps:")
    log.info("  python verify_parity.py")
    log.info("  (then trigger the template generator via the admin API or QuestionGeneratorService)")


if __name__ == "__main__":
    run_scrape()

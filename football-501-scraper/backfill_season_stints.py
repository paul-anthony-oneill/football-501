"""
V8 Python Backfill — career_stats JSONB → player_season_stints
==============================================================

This is a one-off script (not a Flyway migration) that drains the legacy
``players.career_stats`` JSONB column into the normalised
``player_season_stints`` table introduced in V6.

It also back-fills:
  - ``player_external_ids``  (source='fbref', external_id=players.fbref_id)
  - ``team_external_ids``    (source='fbref', external_id=teams.fbref_id)

Run order:
  1. python backfill_season_stints.py          ← this file
  2. python verify_parity.py                   ← confirm zero-row diff
  3. Apply V9 Flyway migration                 ← drops career_stats, fbref_id cols

Usage::

    python backfill_season_stints.py [--dry-run]

Flags:
  --dry-run   Print what would be written but do not commit.
"""

import sys
import re
import logging
import argparse
from datetime import datetime

from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker

sys.path.insert(0, ".")
from config import settings
from database.models_v6 import (
    Base,
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
from utils.darts import is_valid_darts_score

# ---------------------------------------------------------------------------
# Logging
# ---------------------------------------------------------------------------
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Season label normalisation
# ---------------------------------------------------------------------------
def normalise_season_label(raw: str) -> str:
    """
    Convert FBref / config season strings to the 'YYYY-YY' label used in
    the ``seasons`` table.

    Examples
    --------
    "2025-2026"  → "2025-26"
    "2024-2025"  → "2024-25"
    "2023-24"    → "2023-24"   (already correct)
    "2023-2024"  → "2023-24"
    "2022-2023"  → "2022-23"
    """
    # Already short-form?
    if re.match(r"^\d{4}-\d{2}$", raw):
        return raw

    # Long-form YYYY-YYYY
    m = re.match(r"^(\d{4})-(\d{4})$", raw)
    if m:
        year1, year2 = m.group(1), m.group(2)
        return f"{year1}-{year2[2:]}"

    raise ValueError(f"Cannot parse season label: {raw!r}")


def parse_start_year(label: str) -> int:
    """Extract the start year from a normalised label like '2023-24'."""
    return int(label.split("-")[0])


def parse_end_year(label: str) -> int:
    """Extract the end year from a normalised label like '2023-24'."""
    start = parse_start_year(label)
    suffix = int(label.split("-")[1])
    # '2023-24' → end = 2024;  handles century roll-over e.g. '1999-00' → 2000
    century = (start // 100) * 100
    end = century + suffix
    if end <= start:
        end += 100
    return end


# ---------------------------------------------------------------------------
# DB helpers
# ---------------------------------------------------------------------------
def get_or_create_season(session, raw_label: str) -> Season:
    """Return the Season row for *raw_label*, creating it if absent."""
    label = normalise_season_label(raw_label)
    season = session.query(Season).filter_by(label=label).first()
    if season is None:
        start_y = parse_start_year(label)
        end_y = parse_end_year(label)
        season = Season(
            label=label,
            start_year=start_y,
            end_year=end_y,
            is_current=(label == normalise_season_label(settings.current_season)),
        )
        session.add(season)
        session.flush()
        log.info("  Created season: %s", label)
    return season


def get_team_by_name(session, name: str) -> Team | None:
    """Look up a team by exact name."""
    return session.query(Team).filter_by(name=name).first()


def get_competition_by_name(session, name: str) -> Competition | None:
    """Look up a competition by exact name."""
    return session.query(Competition).filter_by(name=name).first()


# ---------------------------------------------------------------------------
# External-ID back-fill helpers
# ---------------------------------------------------------------------------
def ensure_player_external_id(session, player: Player) -> None:
    """Upsert a player_external_ids row for the FBref source."""
    if not player.fbref_id or player.fbref_id.startswith("gen_"):
        return  # synthetic ID — skip
    existing = (
        session.query(PlayerExternalId)
        .filter_by(source="fbref", external_id=player.fbref_id)
        .first()
    )
    if existing is None:
        session.add(PlayerExternalId(
            player_id=player.id,
            source="fbref",
            external_id=player.fbref_id,
            confidence=100,
        ))


def ensure_team_external_id(session, team: Team) -> None:
    """Upsert a team_external_ids row for the FBref source."""
    if not team.fbref_id:
        return
    existing = (
        session.query(TeamExternalId)
        .filter_by(source="fbref", external_id=team.fbref_id)
        .first()
    )
    if existing is None:
        session.add(TeamExternalId(
            team_id=team.id,
            source="fbref",
            external_id=team.fbref_id,
            confidence=100,
        ))


# ---------------------------------------------------------------------------
# Scrape-job / run-log helpers
# ---------------------------------------------------------------------------
def emit_log(session, job: ScrapeJob, level: str, message: str, ctx: dict | None = None) -> None:
    session.add(ScrapeRunLog(
        job_id=job.id,
        level=level,
        message=message,
        context=ctx or {},
    ))


# ---------------------------------------------------------------------------
# Main backfill logic
# ---------------------------------------------------------------------------
def run(dry_run: bool = False) -> None:
    engine = create_engine(settings.database_url, echo=False)
    Session = sessionmaker(bind=engine)
    session = Session()

    # Create a ScrapeJob row to track this run.
    job = ScrapeJob(
        job_type="backfill",
        status="running",
        started_at=datetime.utcnow(),
    )
    session.add(job)
    session.flush()

    log.info("=== V8 Backfill: career_stats → player_season_stints ===")
    if dry_run:
        log.warning("DRY-RUN mode: no changes will be committed.")

    players = session.query(Player).all()
    log.info("Found %d players.", len(players))

    stints_created = 0
    stints_updated = 0
    players_ok = 0
    players_skipped = 0
    players_failed = 0

    for player in players:
        ensure_player_external_id(session, player)

        if not player.career_stats:
            players_skipped += 1
            continue

        player_ok = True

        for raw_stat in player.career_stats:
            try:
                raw_season = raw_stat.get("season")
                team_name = raw_stat.get("team")
                comp_name = raw_stat.get("competition")
                if not (raw_season and team_name and comp_name):
                    emit_log(session, job, "WARN",
                             f"Incomplete stat entry for player {player.name!r}: {raw_stat}",
                             {"player_id": str(player.id)})
                    continue

                # Resolve season
                season_obj = get_or_create_season(session, raw_season)

                # Resolve team
                team_obj = get_team_by_name(session, team_name)
                if team_obj is None:
                    emit_log(session, job, "WARN",
                             f"Team not found: {team_name!r} (player={player.name!r})",
                             {"player_id": str(player.id), "team": team_name})
                    player_ok = False
                    continue

                ensure_team_external_id(session, team_obj)

                # Resolve competition
                comp_obj = get_competition_by_name(session, comp_name)
                if comp_obj is None:
                    emit_log(session, job, "WARN",
                             f"Competition not found: {comp_name!r} (player={player.name!r})",
                             {"player_id": str(player.id), "competition": comp_name})
                    player_ok = False
                    continue

                # Stat values
                def safe_int(val, default=0):
                    try:
                        return int(str(val).replace(",", "")) if val else default
                    except (ValueError, TypeError):
                        return default

                appearances = safe_int(raw_stat.get("appearances"))
                goals = safe_int(raw_stat.get("goals"))
                assists = safe_int(raw_stat.get("assists"))
                minutes = safe_int(raw_stat.get("minutes_played", raw_stat.get("minutes")))
                clean_sheets = safe_int(raw_stat.get("clean_sheets"))

                # Upsert player_season_stints
                existing = (
                    session.query(PlayerSeasonStint)
                    .filter_by(
                        player_id=player.id,
                        season_id=season_obj.id,
                        team_id=team_obj.id,
                        competition_id=comp_obj.id,
                    )
                    .first()
                )

                now = datetime.utcnow()

                if existing is None:
                    stint = PlayerSeasonStint(
                        player_id=player.id,
                        season_id=season_obj.id,
                        team_id=team_obj.id,
                        competition_id=comp_obj.id,
                        appearances=appearances,
                        goals=goals,
                        assists=assists,
                        minutes=minutes,
                        clean_sheets=clean_sheets,
                        is_goalkeeper=(clean_sheets > 0),
                        source="fbref",
                        source_scraped_at=player.last_scraped_at or now,
                        created_at=now,
                        updated_at=now,
                    )
                    session.add(stint)
                    stints_created += 1
                else:
                    # Update stats to latest scraped values.
                    existing.appearances = appearances
                    existing.goals = goals
                    existing.assists = assists
                    existing.minutes = minutes
                    existing.clean_sheets = clean_sheets
                    existing.is_goalkeeper = clean_sheets > 0
                    existing.source_scraped_at = player.last_scraped_at or now
                    existing.updated_at = now
                    stints_updated += 1

            except Exception as exc:
                emit_log(session, job, "ERROR",
                         f"Error processing stat for player {player.name!r}: {exc}",
                         {"player_id": str(player.id), "raw_stat": str(raw_stat)[:200]})
                log.exception("  ↳ Unhandled error for %s: %s", player.name, exc)
                player_ok = False
                players_failed += 1

        if player_ok:
            players_ok += 1
        else:
            players_failed += 1

        # Commit every 100 players to keep the transaction manageable.
        if (players_ok + players_skipped + players_failed) % 100 == 0:
            if not dry_run:
                session.commit()
            total = players_ok + players_skipped + players_failed
            log.info("  Progress: %d/%d players processed…", total, len(players))

    # --- Summary ---
    log.info("")
    log.info("=== Backfill complete ===")
    log.info("  Players processed OK : %d", players_ok)
    log.info("  Players skipped      : %d (no career_stats)", players_skipped)
    log.info("  Players with errors  : %d", players_failed)
    log.info("  Stints created       : %d", stints_created)
    log.info("  Stints updated       : %d", stints_updated)

    # Finish the scrape job row.
    job.status = "success" if players_failed == 0 else "partial"
    job.players_scraped = players_ok
    job.players_failed = players_failed
    job.completed_at = datetime.utcnow()

    emit_log(session, job, "INFO",
             f"Backfill complete: {stints_created} created, {stints_updated} updated, "
             f"{players_failed} player errors.",
             {"stints_created": stints_created, "stints_updated": stints_updated,
              "players_failed": players_failed})

    if not dry_run:
        session.commit()
        log.info("  Changes committed ✅")
    else:
        session.rollback()
        log.info("  DRY-RUN: rolled back — no changes made.")

    session.close()
    log.info("")
    log.info("Next step: python verify_parity.py")
    log.info("           (must return 0 rows before applying V9 migration)")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="V8 Backfill: career_stats → player_season_stints")
    parser.add_argument("--dry-run", action="store_true", help="Print plan but do not commit.")
    args = parser.parse_args()
    run(dry_run=args.dry_run)

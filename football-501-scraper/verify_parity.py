"""
V8 Parity Verification — career_stats vs player_season_stints
=============================================================

Run this after ``backfill_season_stints.py`` and before applying V9.

The script computes per-player goal and appearance totals from both the
old ``players.career_stats`` JSONB column and the new
``player_season_stints`` table, then prints any discrepancies.

Exit code:
  0 — all totals match; safe to apply V9
  1 — discrepancies found; do not apply V9 until they are resolved

Usage::

    python verify_parity.py

Expected output when parity passes::

    Checking goal totals...
    Checking appearance totals...
    ✅  Parity OK — 0 discrepancies found. Safe to proceed with V9.

When discrepancies exist::

    ❌  Goal total mismatch for "Ryan Giggs" (id=...): old=162 new=161
    ...
    ❌  X discrepancies found. Do NOT apply V9 until resolved.
"""

import sys
import logging
from collections import defaultdict

from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker

sys.path.insert(0, ".")
from config import settings

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger(__name__)


PARITY_SQL = """
WITH old_goals AS (
    SELECT
        p.id                                          AS player_id,
        p.name                                        AS player_name,
        SUM((elem->>'goals')::int)                    AS total_goals,
        SUM((elem->>'appearances')::int)              AS total_appearances
    FROM players p,
         jsonb_array_elements(p.career_stats) AS elem
    WHERE jsonb_typeof(p.career_stats) = 'array'
    GROUP BY p.id, p.name
),
new_totals AS (
    SELECT
        player_id,
        SUM(goals)       AS total_goals,
        SUM(appearances) AS total_appearances
    FROM player_season_stints
    GROUP BY player_id
)
SELECT
    o.player_id,
    o.player_name,
    o.total_goals        AS old_goals,
    COALESCE(n.total_goals, 0)        AS new_goals,
    o.total_appearances  AS old_apps,
    COALESCE(n.total_appearances, 0)  AS new_apps
FROM old_goals o
LEFT JOIN new_totals n ON n.player_id = o.player_id
WHERE o.total_goals        != COALESCE(n.total_goals, 0)
   OR o.total_appearances  != COALESCE(n.total_appearances, 0)
ORDER BY o.player_name;
"""


def run() -> int:
    engine = create_engine(settings.database_url, echo=False)

    log.info("Connecting to database: %s", settings.database_url.split("@")[-1])
    log.info("Running parity check…")

    with engine.connect() as conn:
        result = conn.execute(text(PARITY_SQL))
        rows = result.fetchall()

    if not rows:
        log.info("✅  Parity OK — 0 discrepancies found. Safe to proceed with V9.")
        return 0

    log.error("❌  %d discrepancies found:", len(rows))
    for row in rows:
        player_id, player_name, old_g, new_g, old_a, new_a = row
        parts = []
        if old_g != new_g:
            parts.append(f"goals: old={old_g} new={new_g}")
        if old_a != new_a:
            parts.append(f"appearances: old={old_a} new={new_a}")
        log.error(
            '  ❌  "%s" (%s): %s',
            player_name,
            str(player_id)[:8],
            " | ".join(parts),
        )

    log.error("")
    log.error("Do NOT apply V9 until all discrepancies are resolved.")
    log.error("Re-run backfill_season_stints.py --dry-run to inspect the cause.")
    return 1


if __name__ == "__main__":
    sys.exit(run())

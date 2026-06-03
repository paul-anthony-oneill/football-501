"""
inspect_player.py — quick DB sanity check (post-V9)

Shows one player from player_season_stints with their aggregated stats.
Replaces the old career_stats JSONB query which no longer works after V9.

Usage:
    python inspect_player.py [player_name_substring]
"""
import sys
import os
import json
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from config import settings
from sqlalchemy import create_engine, text

engine = create_engine(settings.database_url)

search = f"%{sys.argv[1]}%" if len(sys.argv) > 1 else "%"

query = text("""
    SELECT
        p.name                         AS player,
        t.name                         AS team,
        c.name                         AS competition,
        s.label                        AS season,
        pss.appearances,
        pss.goals,
        pss.assists,
        pss.clean_sheets,
        pss.is_goalkeeper
    FROM player_season_stints pss
    JOIN players     p ON p.id = pss.player_id
    JOIN teams       t ON t.id = pss.team_id
    JOIN competitions c ON c.id = pss.competition_id
    JOIN seasons     s ON s.id = pss.season_id
    WHERE p.name ILIKE :search
    ORDER BY s.start_year DESC, pss.appearances DESC
    LIMIT 20
""")

with engine.connect() as conn:
    rows = conn.execute(query, {"search": search}).fetchall()
    if not rows:
        print(f"No player stints found matching '{sys.argv[1] if len(sys.argv) > 1 else '*'}'")
        sys.exit(0)

    print(f"{'Player':<30} {'Team':<25} {'Competition':<20} {'Season':<10} "
          f"{'Apps':>4} {'Gls':>4} {'Ast':>4} {'CS':>4} {'GK':>3}")
    print("-" * 110)
    for r in rows:
        print(f"{r.player:<30} {r.team:<25} {r.competition:<20} {r.season:<10} "
              f"{r.appearances:>4} {r.goals:>4} {r.assists:>4} {r.clean_sheets:>4} "
              f"{'Y' if r.is_goalkeeper else 'N':>3}")

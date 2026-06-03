"""
verify_2526.py — confirm 2025-26 season data is present (post-V9)

Replaces the old career_stats JSONB query which no longer works after V9.
Checks player_season_stints for the 2025-26 season label.

Usage:
    python verify_2526.py
"""
import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from config import settings
from sqlalchemy import create_engine, text

engine = create_engine(settings.database_url)

with engine.connect() as conn:
    # Count stints for 2025-26
    count_row = conn.execute(text("""
        SELECT COUNT(*) AS total_stints
        FROM player_season_stints pss
        JOIN seasons s ON s.id = pss.season_id
        WHERE s.label = '2025-26'
    """)).fetchone()

    total = count_row.total_stints if count_row else 0

    if total == 0:
        print("❌  No 2025-26 stints found in player_season_stints.")
        print("    Run scrape_historical.py --from-year 2025 or scrape_current_season.py")
        sys.exit(1)

    print(f"✅  Found {total} player_season_stints rows for 2025-26")

    # Show a sample
    sample = conn.execute(text("""
        SELECT p.name, t.name AS team, c.name AS competition,
               pss.appearances, pss.goals, pss.assists
        FROM player_season_stints pss
        JOIN players     p ON p.id = pss.player_id
        JOIN teams       t ON t.id = pss.team_id
        JOIN competitions c ON c.id = pss.competition_id
        JOIN seasons     s ON s.id = pss.season_id
        WHERE s.label = '2025-26'
        ORDER BY pss.goals DESC
        LIMIT 5
    """)).fetchall()

    print("\nTop scorers in 2025-26 (sample):")
    print(f"  {'Player':<30} {'Team':<25} {'Competition':<20} Apps Goals Ast")
    print("  " + "-" * 90)
    for r in sample:
        print(f"  {r.name:<30} {r.team:<25} {r.competition:<20} "
              f"{r.appearances:>4}  {r.goals:>4} {r.assists:>3}")

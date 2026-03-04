from sqlalchemy import create_engine, text
import sys
import os
import json

sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from config import settings

engine = create_engine(settings.database_url)

with engine.connect() as conn:
    result = conn.execute(text("SELECT name, career_stats FROM players WHERE career_stats @> '[{\"season\": \"2025-2026\"}]' LIMIT 1"))
    row = result.fetchone()
    if row:
        print(f"Player: {row[0]}")
        print("Career Stats (Latest):")
        # Find the 2025-2026 entry
        stat_2526 = [s for s in row[1] if s.get('season') == "2025-2026"]
        print(json.dumps(stat_2526, indent=2))
    else:
        print("No players with 2025-2026 stats found")

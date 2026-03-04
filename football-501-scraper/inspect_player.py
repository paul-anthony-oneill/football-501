from sqlalchemy import create_engine, text
import sys
import os
import json

# Add current dir to path to import config
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from config import settings

engine = create_engine(settings.database_url)

with engine.connect() as conn:
    result = conn.execute(text("SELECT name, career_stats FROM players WHERE jsonb_array_length(career_stats) > 0 LIMIT 1"))
    row = result.fetchone()
    if row:
        print(f"Player: {row[0]}")
        print("Career Stats:")
        print(json.dumps(row[1], indent=2))
    else:
        print("No players with stats found")

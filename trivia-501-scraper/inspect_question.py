from sqlalchemy import create_engine, text
import sys
import os
import json

# Add current dir to path to import config
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from config import settings

engine = create_engine(settings.database_url)

with engine.connect() as conn:
    result = conn.execute(text("SELECT question_text, config FROM questions LIMIT 1"))
    row = result.fetchone()
    if row:
        print(f"Question: {row[0]}")
        print("Config:")
        print(json.dumps(row[1], indent=2))
    else:
        print("No questions found")

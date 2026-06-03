"""
Setup Database (V4 Schema)
--------------------------
Creates all tables defined in models_v4.py using SQLAlchemy's metadata.
Run this once before first use, or after a fresh database.

Replaces init_db.sql (which creates the old V1 schema and will conflict).

Usage:
    python setup_db.py
"""

import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from sqlalchemy import create_engine, text
from database.models_v4 import Base
from config import settings


def setup():
    print(f"Connecting to database...")
    engine = create_engine(settings.database_url)

    # Enable pg_trgm for fuzzy player name matching
    with engine.connect() as conn:
        conn.execute(text("CREATE EXTENSION IF NOT EXISTS pg_trgm"))
        conn.commit()
    print("Extension pg_trgm enabled.")

    print("Creating V4 schema tables...")
    Base.metadata.create_all(engine)

    print("Tables created:")
    for table in sorted(Base.metadata.tables.keys()):
        print(f"  - {table}")

    print("\nDatabase setup complete. Next step: python scrape_current_season.py")


if __name__ == "__main__":
    setup()

"""
Simple script to reset database to V2 schema
"""
import sys
sys.path.insert(0, '.')

from sqlalchemy import create_engine, text
from config import settings
from database.crud_v2 import DatabaseManager

print('=' * 60)
print('Resetting database to V2 schema')
print('=' * 60)
print()

print('Step 1: Dropping all existing tables...')
engine = create_engine(settings.database_url)

with engine.connect() as conn:
    # Get all tables
    result = conn.execute(text("SELECT tablename FROM pg_tables WHERE schemaname = 'public'"))
    tables = [row[0] for row in result]

    print(f'  Found {len(tables)} tables: {", ".join(tables) if tables else "none"}')

    # Drop each table with CASCADE
    for table in tables:
        print(f'  Dropping {table}...')
        conn.execute(text(f"DROP TABLE IF EXISTS {table} CASCADE"))

    conn.commit()

print('  Done!')
print()

print('Step 2: Creating V2 tables...')
db = DatabaseManager()
db.init_db()
print('  Done!')
print()

print('Step 3: Enabling PostgreSQL extensions...')
try:
    with engine.connect() as conn:
        conn.execute(text("CREATE EXTENSION IF NOT EXISTS pg_trgm"))
        conn.commit()
        print('  pg_trgm extension enabled (for fuzzy search)')
except Exception as e:
    print(f'  Warning: Could not enable pg_trgm: {e}')
    print('  Fuzzy search may not work optimally')
print()

print('=' * 60)
print('Database reset complete!')
print('=' * 60)
print()
print('Next steps:')
print('  1. Populate sample data: python reset_database.py --populate')
print('  2. Or scrape real data: python example_usage_v2.py --example 1')

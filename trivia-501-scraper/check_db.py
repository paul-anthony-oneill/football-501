from sqlalchemy import create_engine, text
from config import settings

engine = create_engine(settings.database_url)
with engine.connect() as conn:
    print("Checking questions...")
    result = conn.execute(text("SELECT count(*) FROM questions"))
    print(f"Questions count: {result.scalar()}")
    
    print("Checking players...")
    result = conn.execute(text("SELECT count(*) FROM players"))
    print(f"Players count: {result.scalar()}")

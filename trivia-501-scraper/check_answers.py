"""Quick script to check answers count"""
import sys
import os
from sqlalchemy import create_engine, text

sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from config import settings

engine = create_engine(settings.database_url)

with engine.connect() as conn:
    result = conn.execute(text("SELECT COUNT(*) as count FROM answers"))
    count = result.fetchone()[0]
    print(f"Answers count: {count}")

    if count > 0:
        result = conn.execute(text("""
            SELECT q.question_text, COUNT(a.id) as answer_count
            FROM questions q
            LEFT JOIN answers a ON a.question_id = q.id
            WHERE q.is_active = true
            GROUP BY q.id, q.question_text
            LIMIT 5
        """))
        print("\nSample questions with answer counts:")
        for row in result:
            print(f"  - {row[0]}: {row[1]} answers")

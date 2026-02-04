"""Test that real data is queryable"""
import sys
import os
from sqlalchemy import create_engine, text

sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from config import settings

engine = create_engine(settings.database_url)

with engine.connect() as conn:
    # Get a sample question
    result = conn.execute(text("""
        SELECT id, question_text, metric_key
        FROM questions
        WHERE is_active = true
        LIMIT 1
    """))
    question = result.fetchone()

    if not question:
        print("No active questions found!")
    else:
        print(f"Question: {question[1]}")
        print(f"Metric: {question[2]}")
        print(f"Question ID: {question[0]}\n")

        # Get top 10 answers for this question
        result = conn.execute(text("""
            SELECT display_text, score, is_valid_darts, is_bust
            FROM answers
            WHERE question_id = :qid
            ORDER BY score DESC
            LIMIT 10
        """), {"qid": question[0]})

        print("Top 10 answers:")
        for row in result:
            darts_status = "OK" if row[2] else "INVALID DARTS"
            bust_status = "BUST" if row[3] else ""
            print(f"  {row[0]}: {row[1]} points {darts_status} {bust_status}")

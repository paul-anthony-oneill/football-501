#!/usr/bin/env python3
"""
Quick script to view database contents in a readable format.
Usage: python view_database.py
"""

from database import DatabaseManager
from sqlalchemy import text

def safe_print(text_str):
    """Print text with Windows console encoding fix."""
    try:
        print(text_str)
    except UnicodeEncodeError:
        print(text_str.encode('ascii', errors='replace').decode('ascii'))

def main():
    db = DatabaseManager()

    safe_print("\n" + "=" * 80)
    safe_print("FOOTBALL 501 DATABASE CONTENTS")
    safe_print("=" * 80)

    # Questions
    safe_print("\n--- QUESTIONS ---")
    questions = db.get_questions()
    for q in questions:
        answer_count = db.get_answer_count_by_question(q.id)
        safe_print(f"\nID {q.id}: {q.text}")
        safe_print(f"  League: {q.league}")
        safe_print(f"  Season: {q.season}")
        safe_print(f"  Team: {q.team}")
        safe_print(f"  Status: {q.status}")
        safe_print(f"  Answers: {answer_count}")

    # Answers for each question
    safe_print("\n--- ANSWERS (Top 10 per question) ---")
    for q in questions:
        with db.get_session() as session:
            result = session.execute(
                text("""
                    SELECT player_name, statistic_value, is_valid_darts_score, is_bust
                    FROM answers
                    WHERE question_id = :qid
                    ORDER BY statistic_value DESC
                    LIMIT 10
                """),
                {"qid": q.id}
            )
            answers = result.fetchall()

            if answers:
                safe_print(f"\nQuestion {q.id}: {q.text}")
                for i, ans in enumerate(answers, 1):
                    valid = "[VALID]" if ans[2] else "[INVALID]"
                    bust = "[BUST]" if ans[3] else ""
                    safe_print(f"  {i}. {ans[0]}: {ans[1]} {valid} {bust}")
            else:
                safe_print(f"\nQuestion {q.id}: No answers yet")

    # Scrape jobs
    safe_print("\n--- SCRAPE JOBS ---")
    jobs = db.get_scrape_jobs(limit=10)
    if jobs:
        for job in jobs:
            status_icon = "[OK]" if job.status == 'success' else "[FAIL]"
            safe_print(
                f"{status_icon} Job #{job.id}: {job.job_type} - "
                f"{job.rows_inserted} rows - {job.duration_seconds:.1f}s - "
                f"{job.created_at}"
            )
    else:
        safe_print("No scrape jobs recorded yet")

    # Summary
    safe_print("\n--- SUMMARY ---")
    with db.get_session() as session:
        q_count = session.execute(text("SELECT COUNT(*) FROM questions")).scalar()
        a_count = session.execute(text("SELECT COUNT(*) FROM answers")).scalar()
        j_count = session.execute(text("SELECT COUNT(*) FROM scrape_jobs")).scalar()

        safe_print(f"Total Questions: {q_count}")
        safe_print(f"Total Answers: {a_count}")
        safe_print(f"Total Jobs: {j_count}")

    safe_print("\n" + "=" * 80)

if __name__ == "__main__":
    main()

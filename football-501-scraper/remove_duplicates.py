#!/usr/bin/env python3
"""Remove duplicate answers from the database."""

from database import DatabaseManager
from sqlalchemy import text

def safe_print(text):
    try:
        print(text)
    except UnicodeEncodeError:
        print(text.encode('ascii', errors='replace').decode('ascii'))

def main():
    db = DatabaseManager()

    safe_print("\n" + "=" * 80)
    safe_print("REMOVING DUPLICATE ANSWERS")
    safe_print("=" * 80)

    with db.get_session() as session:
        # Find duplicates
        result = session.execute(text("""
            SELECT question_id, player_name, statistic_value, COUNT(*) as count
            FROM answers
            GROUP BY question_id, player_name, statistic_value
            HAVING COUNT(*) > 1
        """))
        duplicates = result.fetchall()

        safe_print(f"\nFound {len(duplicates)} duplicate player entries")

        if len(duplicates) == 0:
            safe_print("No duplicates to remove!")
            return

        # For each duplicate, keep only the one with lowest ID and delete others
        total_deleted = 0
        for dup in duplicates:
            question_id, player_name, stat_value, count = dup

            # Get all IDs for this duplicate
            result = session.execute(
                text("""
                    SELECT id FROM answers
                    WHERE question_id = :qid
                    AND player_name = :name
                    AND statistic_value = :value
                    ORDER BY id
                """),
                {"qid": question_id, "name": player_name, "value": stat_value}
            )
            ids = [row[0] for row in result.fetchall()]

            # Keep first ID, delete the rest
            ids_to_delete = ids[1:]
            if ids_to_delete:
                session.execute(
                    text("DELETE FROM answers WHERE id = ANY(:ids)"),
                    {"ids": ids_to_delete}
                )
                total_deleted += len(ids_to_delete)

        session.commit()

        safe_print(f"\nDeleted {total_deleted} duplicate entries")

    # Verify
    with db.get_session() as session:
        result = session.execute(text("SELECT COUNT(*) FROM answers"))
        total = result.scalar()
        safe_print(f"Total answers remaining: {total}")

    safe_print("\n" + "=" * 80)

if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""
Populate Manchester City Premier League appearances for all seasons from 1992-93 to present.

This script will:
1. Create questions for each season (if not exists)
2. Scrape player data from FBref
3. Populate answers table

Expected duration: ~4-5 minutes (7-second rate limit between requests)
"""

import sys
from datetime import datetime
from database import DatabaseManager, Question
from scrapers import FBrefScraper, DataTransformer
from jobs import QuestionPopulator

def safe_print(text):
    """Print text with Windows console encoding fix."""
    try:
        print(text)
    except UnicodeEncodeError:
        print(text.encode('ascii', errors='replace').decode('ascii'))

def generate_seasons():
    """Generate all Premier League seasons from 1992-93 to 2024-25."""
    seasons = []
    start_year = 1992
    end_year = 2024  # Current season 2024-25

    for year in range(start_year, end_year + 1):
        next_year = year + 1
        season = f"{year}-{next_year}"
        seasons.append(season)

    return seasons

def main():
    safe_print("\n" + "=" * 80)
    safe_print("MANCHESTER CITY PREMIER LEAGUE HISTORY POPULATION")
    safe_print("=" * 80)

    db = DatabaseManager()
    populator = QuestionPopulator()

    seasons = generate_seasons()
    safe_print(f"\nTotal seasons to populate: {len(seasons)}")
    safe_print(f"Expected duration: ~{len(seasons) * 7 / 60:.1f} minutes (7-second rate limit)")
    safe_print("\nSeasons: " + ", ".join(seasons[:5]) + f" ... {seasons[-1]}")
    safe_print("\nStarting population...")
    safe_print("\n" + "-" * 80)

    total_questions = 0
    total_answers = 0
    failed_seasons = []

    for i, season in enumerate(seasons, 1):
        safe_print(f"\n[{i}/{len(seasons)}] Processing {season}...")

        try:
            # Check if question already exists
            existing = db.get_questions(
                league='England Premier League',
                season=season,
                status='active'
            )

            # Filter for Manchester City
            man_city_q = [q for q in existing if q.team == 'Manchester City']

            if man_city_q:
                question = man_city_q[0]
                safe_print(f"  Question already exists (ID: {question.id})")

                # Check if answers exist
                answer_count = db.get_answer_count_by_question(question.id)
                if answer_count > 0:
                    safe_print(f"  Already populated with {answer_count} answers - SKIPPING")
                    total_answers += answer_count
                    continue
            else:
                # Create new question
                question_text = f"Appearances for Manchester City in Premier League {season}"

                with db.get_session() as session:
                    question = Question(
                        text=question_text,
                        league='England Premier League',
                        season=season,
                        team='Manchester City',
                        stat_type='appearances',
                        status='active'
                    )
                    session.add(question)
                    session.commit()
                    session.refresh(question)

                safe_print(f"  Created question (ID: {question.id})")
                total_questions += 1

            # Populate answers
            safe_print(f"  Scraping data (this will take ~7 seconds)...")
            result = populator.populate_single_question(question.id)

            if result['status'] == 'success':
                players_added = result.get('players_added', 0)
                safe_print(f"  [OK] Inserted {players_added} answers in {result['duration']:.1f}s")
                total_answers += players_added
            else:
                safe_print(f"  [ERROR] {result.get('error', 'Unknown error')}")
                failed_seasons.append(season)

        except Exception as e:
            safe_print(f"  [ERROR] {str(e)}")
            failed_seasons.append(season)
            continue

    # Summary
    safe_print("\n" + "=" * 80)
    safe_print("SUMMARY")
    safe_print("=" * 80)
    safe_print(f"Questions created: {total_questions}")
    safe_print(f"Total answers inserted: {total_answers}")
    safe_print(f"Seasons processed: {len(seasons) - len(failed_seasons)}/{len(seasons)}")

    if failed_seasons:
        safe_print(f"\nFailed seasons ({len(failed_seasons)}):")
        for season in failed_seasons:
            safe_print(f"  - {season}")
    else:
        safe_print("\n[OK] All seasons populated successfully!")

    safe_print("\n" + "=" * 80)

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        safe_print("\n\n[WARN] Interrupted by user")
        sys.exit(1)
    except Exception as e:
        safe_print(f"\n\n[ERROR] Unexpected error: {e}")
        sys.exit(1)

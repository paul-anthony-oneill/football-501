#!/usr/bin/env python3
"""
Populate Manchester City Premier League appearances for all seasons from 1992-93 to present.
VERSION 2: Uses the normalized schema workflow (Scrape -> Store -> Populate).

This script will:
1. Iterate through all Premier League seasons (1992-present)
2. Scrape the full league data for that season (storing in player_career_stats)
3. Create/Update the specific Man City question
4. Populate the answers from the stored data

Expected duration: ~10 minutes (due to scraping full leagues)
"""

import sys
import os
import time

# Add current directory to path so imports work
sys.path.append(os.getcwd())

from database.crud_v2 import DatabaseManager
from database.models_v2 import Question
from scrapers.player_career_scraper import PlayerCareerScraper
from jobs.populate_questions_v2 import QuestionPopulator

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
        # Format: "1992-1993"
        season = f"{year}-{next_year}"
        seasons.append(season)

    return seasons

def main():
    safe_print("\n" + "=" * 80)
    safe_print("MANCHESTER CITY PREMIER LEAGUE HISTORY POPULATION (V2)")
    safe_print("=" * 80)

    db = DatabaseManager()
    scraper = PlayerCareerScraper()
    populator = QuestionPopulator()

    seasons = generate_seasons()
    safe_print(f"\nTotal seasons to populate: {len(seasons)}")
    
    # We will prioritize recent seasons first for immediate value
    seasons.reverse()
    
    safe_print(f"\nSeasons (newest to oldest): {', '.join(seasons[:5])} ... {seasons[-1]}")
    safe_print("\nStarting population...")
    safe_print("\n" + "-" * 80)

    total_stats_stored = 0
    total_answers = 0
    failed_seasons = []

    for i, season in enumerate(seasons, 1):
        safe_print(f"\n[{i}/{len(seasons)}] Processing {season}...")

        try:
            # STEP 1: Scrape Data
            # Use canonical name "England Premier League" to ensure DB consistency
            league_name = "England Premier League"
            
            safe_print(f"  Scraping Man City stats for {season}...")
            
            scrape_result = scraper.scrape_team_players(
                team_name="Manchester City",
                league=league_name,
                season=season
            )
            
            stored = scrape_result.get('stats_stored', 0)
            processed = scrape_result.get('players_processed', 0)
            safe_print(f"  Stored {stored} stats records for {processed} players")
            total_stats_stored += stored

            if processed == 0:
                safe_print(f"  [WARN] No players found for {season}. Man City may have been relegated.")
                failed_seasons.append(f"{season} (Relegated/No Data)")
                continue

            # STEP 2: Create/Get Question
            # ...
            
            existing = db.get_active_questions()
            man_city_q = None
            for q in existing:
                if (q.team_id and db.get_team_by_id(q.team_id).name == 'Manchester City' and 
                    q.season_filter == season and 
                    q.stat_type == 'appearances'):
                    man_city_q = q
                    break

            if man_city_q:
                question = man_city_q
                safe_print(f"  Question already exists (ID: {question.id})")
            else:
                # Create new question
                question_text = f"Appearances for Manchester City in Premier League {season}"
                
                man_city_team = db.get_team_by_name("Manchester City")
                pl_comp = db.get_competition_by_name(league_name)
                
                if not man_city_team or not pl_comp:
                     man_city_team = db.get_or_create_team("Manchester City", "club", "England")
                     pl_comp = db.get_or_create_competition(league_name, "domestic_league", "England")

                question = db.create_question(
                    question_text=question_text,
                    stat_type='appearances',
                    team_id=man_city_team.id,
                    competition_id=pl_comp.id,
                    season_filter=season
                )
                safe_print(f"  Created question (ID: {question.id})")

            # STEP 3: Populate Answers (from cache)
            safe_print(f"  Populating answers from cache...")
            result = populator.populate_single_question(question.id)

            if result['status'] == 'success':
                players_added = result.get('players_added', 0)
                safe_print(f"  [OK] Inserted {players_added} answers")
                total_answers += players_added
            else:
                safe_print(f"  [ERROR] Population failed: {result.get('error', 'Unknown error')}")
                failed_seasons.append(season)

            # Polite wait between seasons
            time.sleep(2)

        except Exception as e:
            safe_print(f"  [ERROR] Critical failure for {season}: {str(e)}")
            import traceback
            traceback.print_exc()
            failed_seasons.append(season)
            continue

    # Summary
    safe_print("\n" + "=" * 80)
    safe_print("SUMMARY")
    safe_print("=" * 80)
    safe_print(f"Total stats stored: {total_stats_stored}")
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
